package org.dromara.qc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.qc.domain.SampleNotify;
import org.dromara.qc.domain.bo.SampleNotifySendBo;
import org.dromara.qc.domain.vo.QcNotifySampleVo;
import org.dromara.qc.domain.vo.SampleNotifyMailPreviewVo;
import org.dromara.qc.domain.vo.SampleNotifySendVo;
import org.dromara.qc.domain.vo.SampleNotifySignVo;
import org.dromara.qc.mapper.SampleNotifyMapper;
import org.dromara.qc.service.ISampleNotifyDeliveryService;
import org.dromara.qc.utils.NotifyConstants;
import org.dromara.report.domain.ReportMailConfig;
import org.dromara.report.domain.vo.ReportMailSenderOptionVo;
import org.dromara.report.service.IReportMailConfigService;
import org.dromara.report.service.ISsqSignService;
import org.dromara.report.service.impl.ReportMailSender;
import org.dromara.report.utils.ArtifactPermissions;
import org.dromara.report.utils.ReportArtifactPaths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 样本通知台账 服务实现（交付侧）
 * <p>
 * 送签 → 刷新签署 → 提交（待处理）→ 发送邮件（成功 / 失败）。
 * 发送方法刻意**不加 {@code @Transactional}**：发信是外部副作用，事务回滚不能把邮件收回来；
 * 失败状态必须独立落库，否则页面永远看不到「发送失败」。
 *
 * @author <你的名字>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SampleNotifyDeliveryServiceImpl implements ISampleNotifyDeliveryService {

    /** 上上签查询接口实测值：签署完成 */
    private static final String CONTRACT_STATUS_COMPLETE = "COMPLETE";
    /** 上上签查询接口实测值：已派发待签 */
    private static final String CONTRACT_STATUS_SENT = "SENT";
    /** 失败原因入库截断 */
    private static final int FAILURE_MAX = 500;

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    private final SampleNotifyMapper notifyMapper;
    private final ISsqSignService ssqSignService;
    private final IReportMailConfigService mailConfigService;
    private final ReportMailSender mailSender;
    private final NotifyQcStatusWriter qcStatusWriter;

    /** 制品根目录（容器内 /app/reports） */
    @Value("${report.artifact.root:/app/reports}")
    private String artifactRoot;

    // ── 送签 / 刷新签署 ───────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SampleNotifySignVo sign(Long id) {
        SampleNotify record = requireNotify(id);
        if (!NotifyConstants.Status.GENERATED.equals(record.getNotifyStatus())) {
            throw new ServiceException("当前状态为「" + NotifyConstants.Status.label(record.getNotifyStatus())
                + "」，不能再送签", 409);
        }
        if (!NotifyConstants.Sign.NONE.equals(record.getSignStatus())) {
            throw new ServiceException("该通知已经送签（合同号 " + record.getSignContractId()
                + "），不重复送签；要重新签请先重新生成通知", 409);
        }
        if (StringUtils.isBlank(record.getPdfPath())) {
            throw new ServiceException("该通知还没有 PDF（OnlyOffice 转换失败），请先重新生成通知再送签", 409);
        }
        String contractId = NotifyConstants.Type.isDelay(record.getNotifyType())
            ? ssqSignService.sendReportDelay(record.getPdfPath())
            : ssqSignService.sendQcUnqualified(record.getPdfPath());
        notifyMapper.update(null, Wrappers.<SampleNotify>lambdaUpdate()
            .set(SampleNotify::getSignContractId, contractId)
            .set(SampleNotify::getSignStatus, NotifyConstants.Sign.SUBMITTED)
            .set(SampleNotify::getUpdateTime, new Date())
            .eq(SampleNotify::getId, id));
        SampleNotifySignVo vo = new SampleNotifySignVo();
        vo.setNotifyId(id);
        vo.setContractId(contractId);
        vo.setSigned(false);
        vo.setSignStatus(NotifyConstants.Sign.SUBMITTED);
        vo.setNotifyStatus(record.getNotifyStatus());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SampleNotifySignVo refreshSign(Long id) {
        SampleNotify record = requireNotify(id);
        SampleNotifySignVo vo = new SampleNotifySignVo();
        vo.setNotifyId(id);
        vo.setContractId(record.getSignContractId());
        vo.setSignStatus(record.getSignStatus());
        vo.setNotifyStatus(record.getNotifyStatus());
        vo.setSignedPdfPath(record.getSignedPdfPath());
        if (StringUtils.isBlank(record.getSignContractId())) {
            throw new ServiceException("该通知还没送签，无法刷新签署状态", 409);
        }
        // 已经拉到签名件：直接返回，不重复下载覆盖
        if (StringUtils.isNotBlank(record.getSignedPdfPath()) && Files.isRegularFile(Path.of(record.getSignedPdfPath()))) {
            vo.setSigned(true);
            vo.setSignedPdfName(Path.of(record.getSignedPdfPath()).getFileName().toString());
            return vo;
        }
        String status = ssqSignService.queryContractStatus(record.getSignContractId());
        vo.setContractStatus(status);
        if (!isSignFinished(status)) {
            vo.setSigned(false);
            return vo;
        }
        Path signedDir = ReportArtifactPaths.signedDir(artifactRoot, ReportArtifactPaths.NOTIFY, LocalDate.now());
        Path signed = ssqSignService.downloadSignedPdf(record.getSignContractId(), signedDir,
            fileBaseOf(record.getPdfPath()));
        ArtifactPermissions.relax(signed);
        notifyMapper.update(null, Wrappers.<SampleNotify>lambdaUpdate()
            .set(SampleNotify::getSignedPdfPath, signed.toString())
            .set(SampleNotify::getSignStatus, NotifyConstants.Sign.SIGNED)
            .set(SampleNotify::getFilePath, signed.toString())
            .set(SampleNotify::getUpdateTime, new Date())
            .eq(SampleNotify::getId, id));
        vo.setSigned(true);
        vo.setSignedPdfPath(signed.toString());
        vo.setSignedPdfName(signed.getFileName().toString());
        vo.setSignStatus(NotifyConstants.Sign.SIGNED);
        return vo;
    }

    // ── 提交 ─────────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        SampleNotify record = requireNotify(id);
        if (!NotifyConstants.Status.GENERATED.equals(record.getNotifyStatus())) {
            throw new ServiceException("当前状态为「" + NotifyConstants.Status.label(record.getNotifyStatus())
                + "」，不能提交", 409);
        }
        if (!NotifyConstants.Sign.SIGNED.equals(record.getSignStatus())
            || StringUtils.isBlank(record.getSignedPdfPath())) {
            throw new ServiceException("通知还没完成签署，请先送签并刷新签署状态", 409);
        }
        notifyMapper.update(null, Wrappers.<SampleNotify>lambdaUpdate()
            .set(SampleNotify::getNotifyStatus, NotifyConstants.Status.PENDING)
            .set(SampleNotify::getUpdateTime, new Date())
            .eq(SampleNotify::getId, id));
        qcStatusWriter.write(record.getSubbarcode(), record.getNotifyType(), NotifyConstants.Status.PENDING);
    }

    // ── 发送 ─────────────────────────────────────────────────────

    @Override
    public SampleNotifyMailPreviewVo mailPreview(Long id) {
        SampleNotify record = requireNotify(id);
        QcNotifySampleVo sample = loadSample(record.getSubbarcode());
        List<String> to = recipientCandidates(record, sample);
        List<String> cc = splitAddresses(notifyMapper.selectProjectCcEmail(record.getProjectNo()));
        Map<String, String> values = mailValues(record);
        String subject = render(NotifyConstants.Mail.SUBJECT, values);
        String content = render(NotifyConstants.Type.isDelay(record.getNotifyType())
            ? NotifyConstants.Mail.BODY_DELAY : NotifyConstants.Mail.BODY_QC, values);

        SampleNotifyMailPreviewVo vo = new SampleNotifyMailPreviewVo();
        vo.setNotifyId(id);
        vo.setToCandidates(to);
        vo.setCcCandidates(cc);
        vo.setSubject(subject);
        vo.setContent(content);
        vo.setAttachmentName(attachmentName(record));

        List<ReportMailSenderOptionVo> options = new ArrayList<>(mailConfigService.senderOptions());
        options.forEach(option -> {
            option.setSubject(subject);
            option.setContent(content);
        });
        vo.setSenderOptions(options);
        vo.setDefaultConfigId(defaultConfigId(options));
        boolean hasEnabled = options.stream().anyMatch(option -> "1".equals(option.getEnabled()));
        String blocked = blockedReason(record, sample, to, hasEnabled);
        vo.setCanSend(blocked == null);
        vo.setBlockedReason(blocked);
        return vo;
    }

    @Override
    public SampleNotifySendVo send(Long id, SampleNotifySendBo bo) {
        SampleNotify record = requireNotify(id);
        if (!NotifyConstants.Status.PENDING.equals(record.getNotifyStatus())
            && !NotifyConstants.Status.FAILED.equals(record.getNotifyStatus())) {
            throw new ServiceException("只有「待处理 / 发送失败」的通知能发送，当前："
                + NotifyConstants.Status.label(record.getNotifyStatus()), 409);
        }
        if (!NotifyConstants.Sign.SIGNED.equals(record.getSignStatus())
            || StringUtils.isBlank(record.getSignedPdfPath())
            || !Files.isRegularFile(Path.of(record.getSignedPdfPath()))) {
            throw new ServiceException("签名件不存在（可能已被清理），请重新送签并刷新签署状态", 409);
        }
        // 先取发件邮箱：配置有问题（没配/停用/缺密码）要在这里就拦掉，不落发送失败状态
        ReportMailConfig config = mailConfigService.loadForSend(bo.getConfigId());
        String messageId;
        try {
            messageId = mailSender.send(config, bo.getTo(), bo.getCc(), bo.getSubject(),
                toHtml(bo.getContent()), new File(record.getSignedPdfPath()));
        } catch (RuntimeException e) {
            markFailed(id, bo.getTo(), bo.getCc(), e.getMessage());
            throw e instanceof ServiceException serviceException
                ? serviceException
                : new ServiceException("邮件发送失败：" + e.getMessage());
        }
        notifyMapper.update(null, Wrappers.<SampleNotify>lambdaUpdate()
            .set(SampleNotify::getNotifyStatus, NotifyConstants.Status.SENT)
            .set(SampleNotify::getSenderBy, LoginHelper.getUserId())
            .set(SampleNotify::getSenderTime, new Date())
            .set(SampleNotify::getSendTo, bo.getTo())
            .set(SampleNotify::getSendCc, bo.getCc())
            .set(SampleNotify::getSendError, null)
            .set(SampleNotify::getUpdateTime, new Date())
            .eq(SampleNotify::getId, id));
        qcStatusWriter.write(record.getSubbarcode(), record.getNotifyType(), NotifyConstants.Status.SENT);
        return new SampleNotifySendVo(id, bo.getTo(), bo.getCc(), config.getFromEmail(), messageId);
    }

    /** 发送失败：独立落库（不在发信事务里，失败痕迹不会回滚） */
    private void markFailed(Long id, String to, String cc, String reason) {
        notifyMapper.update(null, Wrappers.<SampleNotify>lambdaUpdate()
            .set(SampleNotify::getNotifyStatus, NotifyConstants.Status.FAILED)
            .set(SampleNotify::getSenderBy, LoginHelper.getUserId())
            .set(SampleNotify::getSenderTime, new Date())
            .set(SampleNotify::getSendTo, to)
            .set(SampleNotify::getSendCc, cc)
            .set(SampleNotify::getSendError, truncate(reason))
            .set(SampleNotify::getUpdateTime, new Date())
            .eq(SampleNotify::getId, id));
    }

    // ── 收件人 / 文案 ─────────────────────────────────────────────

    /**
     * 收件人候选 = 样本报告接收邮箱 + 研究中心收件邮箱。
     * 样本/中心查不到时返回已解析到的部分，由 blockedReason 给出原因。
     */
    private List<String> recipientCandidates(SampleNotify record, QcNotifySampleVo sample) {
        List<String> to = new ArrayList<>();
        if (sample != null) {
            to.addAll(splitAddresses(sample.getReportEmail()));
            if (StringUtils.isNotBlank(sample.getCenter())) {
                to.addAll(splitAddresses(notifyMapper.selectCenterReceiveEmail(record.getProjectNo(), sample.getCenter())));
            }
        }
        return distinct(to);
    }

    /** 不允许发送的原因（按「先配置后流程」的顺序给第一条） */
    private String blockedReason(SampleNotify record, QcNotifySampleVo sample, List<String> to, boolean hasEnabledSender) {
        if (!NotifyConstants.Status.PENDING.equals(record.getNotifyStatus())
            && !NotifyConstants.Status.FAILED.equals(record.getNotifyStatus())) {
            return "只有「待处理 / 发送失败」的通知能发送，当前：" + NotifyConstants.Status.label(record.getNotifyStatus());
        }
        if (!NotifyConstants.Sign.SIGNED.equals(record.getSignStatus())) {
            return "通知还没完成签署，请先送签并刷新签署状态";
        }
        if (StringUtils.isBlank(record.getSignedPdfPath()) || !Files.isRegularFile(Path.of(record.getSignedPdfPath()))) {
            return "签名件不存在（可能已被清理），请重新送签并刷新签署状态";
        }
        if (sample == null) {
            return "查不到样本信息（cms_sample），无法解析收件人";
        }
        if (StringUtils.isBlank(sample.getReportEmail())) {
            return "样本未填「报告接收邮箱」（cms_sample.report_email）";
        }
        if (StringUtils.isBlank(record.getProjectNo())) {
            return "该通知没有项目编号，无法匹配研究中心收件邮箱";
        }
        if (StringUtils.isBlank(sample.getCenter())) {
            return "样本未填研究中心名称，无法匹配研究中心收件邮箱";
        }
        String centerEmail = notifyMapper.selectCenterReceiveEmail(record.getProjectNo(), sample.getCenter());
        if (StringUtils.isBlank(centerEmail)) {
            return "项目「" + record.getProjectNo() + "」的研究中心「" + sample.getCenter() + "」未配置收件邮箱";
        }
        if (!hasEnabledSender) {
            return "没有可用的发件邮箱：请到「报告管理 › 邮件配置」新增一条并启用";
        }
        return null;
    }

    private Map<String, String> mailValues(SampleNotify record) {
        Map<String, String> values = new HashMap<>();
        values.put("prefix", NotifyConstants.Type.mailSubjectPrefix(record.getNotifyType()));
        values.put("subbarcode", nvl(record.getSubbarcode()));
        values.put("sampleName", nvl(record.getSampleName()));
        values.put("participantNo", nvl(record.getParticipantNo()));
        values.put("projectNo", nvl(record.getProjectNo()));
        values.put("notifyDate", nvl(record.getNotifyDate()));
        values.put("studyNo", nvl(record.getStudyNo()));
        values.put("sampleType", nvl(record.getSampleType()));
        values.put("reason", nvl(record.getReason()));
        values.put("delayReason", nvl(record.getDelayReason()));
        values.put("expectedReportDate", nvl(record.getExpectedReportDate()));
        return values;
    }

    /** {{key}} 占位符替换（缺值给空串） */
    private String render(String template, Map<String, String> values) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(nvl(values.get(matcher.group(1)))));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String attachmentName(SampleNotify record) {
        String path = record.getSignedPdfPath();
        return StringUtils.isBlank(path) ? null : Path.of(path).getFileName().toString();
    }

    private Long defaultConfigId(List<ReportMailSenderOptionVo> options) {
        if (options.isEmpty()) {
            return null;
        }
        return options.stream()
            .filter(option -> "1".equals(option.getIsDefault()))
            .findFirst()
            .orElse(options.get(0))
            .getConfigId();
    }

    // ── 工具 ─────────────────────────────────────────────────────

    /** 取样本当前信息（报告接收邮箱 / 研究中心用；记录本身只在当前租户可见，样本同租户） */
    private QcNotifySampleVo loadSample(String subbarcode) {
        return notifyMapper.selectSampleForNotify(subbarcode);
    }

    private SampleNotify requireNotify(Long id) {
        SampleNotify record = notifyMapper.selectById(id);
        if (record == null) {
            throw new ServiceException("通知记录不存在：id=" + id);
        }
        return record;
    }

    private boolean isSignFinished(String status) {
        if (CONTRACT_STATUS_COMPLETE.equalsIgnoreCase(status)) {
            return true;
        }
        if (!CONTRACT_STATUS_SENT.equalsIgnoreCase(status)) {
            log.warn("上上签合同状态不是已实测的两个值（SENT / COMPLETE）：{}，按「未完成」处理", status);
        }
        return false;
    }

    private String fileBaseOf(String pdfPath) {
        if (StringUtils.isBlank(pdfPath)) {
            throw new ServiceException("该通知没有 PDF 路径，无法命名签名件");
        }
        String name = Path.of(pdfPath).getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    /** 正文换行统一转 &lt;br&gt;（与报告发送同口径） */
    private String toHtml(String content) {
        return content.replace("\r\n", "<br>").replace("\n", "<br>");
    }

    private List<String> splitAddresses(String addresses) {
        if (StringUtils.isBlank(addresses)) {
            return new ArrayList<>();
        }
        return Arrays.stream(addresses.split("[,;]"))
            .map(String::trim)
            .filter(address -> !address.isEmpty())
            .distinct()
            .toList();
    }

    private List<String> distinct(List<String> values) {
        return values.stream().filter(StringUtils::isNotBlank).distinct().toList();
    }

    private String truncate(String reason) {
        String text = StringUtils.isBlank(reason) ? "未知原因" : reason;
        return text.length() > FAILURE_MAX ? text.substring(0, FAILURE_MAX) : text;
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}
