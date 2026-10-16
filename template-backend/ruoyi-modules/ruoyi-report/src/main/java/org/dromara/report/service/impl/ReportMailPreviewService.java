package org.dromara.report.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.report.domain.vo.InterpretationContextVo;
import org.dromara.report.domain.vo.InterpretationLimsVo;
import org.dromara.report.domain.vo.InterpretationMailPreviewVo;
import org.dromara.report.domain.vo.ReportMailSenderOptionVo;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.service.IInterpretationService;
import org.dromara.report.service.IReportMailConfigService;
import org.dromara.report.utils.ReportMailDefaults;
import org.dromara.report.utils.ReportStatus;
import org.dromara.report.service.impl.ReportMailVariables;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 「报告发送」Tab 的参数装配：可选发件邮箱 + 收件/抄送候选 + 渲染后的标题正文 + 附件名 + 可否发送。
 * <p>
 * 四条口径：
 * <ul>
 *   <li><b>发件邮箱</b>：来自「邮件配置」列表（可多条），默认那条排最前并作为默认选中；
 *       每条都带上按本报告变量渲染好的标题/正文，页面切换邮箱时直接换文案，不用再请求。</li>
 *   <li><b>收件候选只看 LIMS</b>：取该报告 LIMS 的 4 个邮箱字段（报告接收人 / 送检邮箱 / 患者信息邮箱 / 接诊医生邮箱），
 *       去空去重，且只留含 {@code @} 的值（LIMS 里这几个字段常存姓名）；不按客户查表（本期不做按客户配收件人）。</li>
 *   <li><b>标题正文是默认值</b>：取「默认发件邮箱」那套模板渲染；页面可改。</li>
 *   <li><b>可发送条件</b>：报告状态为「报告待发送 / 报告发送失败」+ 制品文件存在 + 至少有一条启用的发件邮箱。</li>
 * </ul>
 * 报告状态与 LIMS 都走既有入口（{@code selectReportRow} / {@code loadContext}），口径与「LIMS 信息」Tab 完全一致。
 *
 * @author <你的名字>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportMailPreviewService {

    /** 可发送的报告状态：待发送（60）/ 发送失败可重发（61）—— 与 ReportMailSendService 同一口径 */
    private static final List<Integer> SENDABLE_STATUS =
        List.of(ReportStatus.PENDING_SEND, ReportStatus.SEND_FAILED);

    private final InterpretationMapper interpretationMapper;

    private final IInterpretationService interpretationService;

    private final IReportMailConfigService mailConfigService;

    private final ReportMailVariables mailVariables;

    /**
     * 装配发送页参数。
     *
     * @param reportId   报告ID
     * @param analysisId 分析数据ID（校验报告属于该批次）
     * @return 发送页参数
     */
    public InterpretationMailPreviewVo preview(Long reportId, Long analysisId) {
        Map<String, Object> report = loadReport(reportId, analysisId);
        InterpretationContextVo context = interpretationService.loadContext(reportId, analysisId);
        InterpretationLimsVo lims = context == null ? null : context.getLims();

        Map<String, String> values = mailVariables.build(report, lims);
        List<ReportMailSenderOptionVo> options = renderOptions(mailConfigService.senderOptions(), values);

        InterpretationMailPreviewVo vo = new InterpretationMailPreviewVo();
        vo.setSenderOptions(options);
        vo.setDefaultConfigId(defaultConfigId(options));
        vo.setToCandidates(recipientCandidates(lims));
        vo.setCcCandidates(new ArrayList<>());

        // 页面初始的标题/正文 = 默认发件邮箱那套模板渲染结果
        ReportMailSenderOptionVo selected = optionById(options, vo.getDefaultConfigId());
        vo.setSubject(selected == null ? null : selected.getSubject());
        vo.setContent(selected == null ? null : selected.getContent());

        // 附件名取报告行登记的制品路径文件名：只用于页面展示，发送时仍由 ReportFileService 做目录越界与存在性校验
        String attachmentName = fileName(firstNotBlank(
            asString(report.get("reportSignedFilePath")),
            asString(report.get("reportFilePath")),
            asString(report.get("reportFileRawPath"))));
        vo.setAttachmentName(attachmentName);

        Integer status = statusCode(report.get("status"));
        boolean hasEnabledSender = options.stream().anyMatch(option -> "1".equals(option.getEnabled()));
        vo.setConfigEnabled(hasEnabledSender);
        vo.setCanSend(hasEnabledSender && SENDABLE_STATUS.contains(status) && attachmentName != null);
        vo.setBlockedReason(blockedReason(hasEnabledSender, status, attachmentName));
        return vo;
    }

    /** 报告状态码（report_status 别名为 status；取不到/非法时给 null，由调用方按「未知」提示） */
    private Integer statusCode(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            // 脏数据（状态列不是数字）按「未知状态」处理，不阻断预览：留日志便于排查
            log.warn("报告状态不是合法数字，按未知状态处理：status={}", value);
            return null;
        }
    }

    /** 报告行 + 批次校验 */
    private Map<String, Object> loadReport(Long reportId, Long analysisId) {
        Map<String, Object> report = interpretationMapper.selectReportRow(reportId);
        if (report == null) {
            throw new ServiceException("报告不存在或已删除：reportId=" + reportId, 400);
        }
        if (!String.valueOf(analysisId).equals(asString(report.get("analysisId")))) {
            throw new ServiceException("报告与分析批次不匹配：reportId=" + reportId + "，analysisId=" + analysisId, 400);
        }
        return report;
    }

    /** 每条发件邮箱用它自己的标题/正文模板渲染一份（变量值对所有邮箱是同一套） */
    private List<ReportMailSenderOptionVo> renderOptions(List<ReportMailSenderOptionVo> options,
                                                        Map<String, String> values) {
        options.forEach(option -> {
            option.setSubject(mailVariables.render(
                StringUtils.isBlank(option.getSubject()) ? ReportMailDefaults.SUBJECT : option.getSubject(), values));
            option.setContent(mailVariables.render(
                StringUtils.isBlank(option.getContent()) ? ReportMailDefaults.CONTENT : option.getContent(), values));
        });
        return options;
    }

    /** 默认选中的发件邮箱：优先带默认标记的，否则第一条（与后端发信的兜底顺序一致） */
    private Long defaultConfigId(List<ReportMailSenderOptionVo> options) {
        if (options.isEmpty()) {
            return null;
        }
        ReportMailSenderOptionVo preferred = options.stream()
            .filter(option -> "1".equals(option.getIsDefault()))
            .findFirst()
            .orElse(options.get(0));
        return preferred.getConfigId();
    }

    private ReportMailSenderOptionVo optionById(List<ReportMailSenderOptionVo> options, Long configId) {
        if (configId == null) {
            return null;
        }
        return options.stream()
            .filter(option -> configId.equals(option.getConfigId()))
            .findFirst()
            .orElse(null);
    }

    /** 不允许发送的原因（按「先配置后流程」的顺序给第一条）；三项都满足时返回空表示可发送 */
    private String blockedReason(boolean hasEnabledSender, Integer status, String attachmentName) {
        String reason = null;
        if (!hasEnabledSender) {
            reason = "没有可用的发件邮箱：请到「报告管理 › 邮件配置」新增一条并启用";
        } else if (!SENDABLE_STATUS.contains(status)) {
            reason = "只有「报告待发送」的报告才能发送，当前状态：" + ReportStatus.label(status);
        } else if (attachmentName == null) {
            reason = "报告文件尚未生成，请先在「报告预览」页生成报告";
        }
        return reason;
    }

    /** LIMS 的 4 个邮箱字段 → 去空去重的收件候选（保持字段定义顺序；只留像邮箱的，见 addIfPresent） */
    private List<String> recipientCandidates(InterpretationLimsVo lims) {
        if (lims == null) {
            return new ArrayList<>();
        }
        Set<String> candidates = new LinkedHashSet<>();
        addIfPresent(candidates, lims.getReportReceiver());
        addIfPresent(candidates, lims.getEmailAddress());
        addIfPresent(candidates, lims.getPatientInfoEmail());
        addIfPresent(candidates, lims.getDoctorEmail());
        return new ArrayList<>(candidates);
    }

    /**
     * 加一个候选地址。只收「含 @」的值：LIMS 的 reportReceiver 等字段实际存的是**姓名**
     * （如「王医生」），带成默认收件人会让发信直接失败，所以这里先过滤掉；
     * 页面仍可手输任意地址（由后端/邮箱服务校验）。
     */
    private void addIfPresent(Set<String> target, String address) {
        if (StringUtils.isNotBlank(address) && address.contains("@")) {
            target.add(address.trim());
        }
    }

    /**
     * 制品路径 → 文件名。只用于页面展示（发送时由 ReportFileService 校验目录越界与存在性）。
     * 路径为空就是没有附件，这里明确返回 null，由调用方据此判定「不可发送」并给出原因；
     * 用字符串截取而不是 Paths.get()，避免非法路径抛 InvalidPathException 还要吞异常。
     */
    private String fileName(String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        String normalized = path.replace('\\', '/');
        String name = StringUtils.substringAfterLast(normalized, "/");
        return StringUtils.isBlank(name) ? null : name;
    }

    /** 取第一个非空白值；全空返回 null（调用方按「没有附件」处理） */
    private String firstNotBlank(String... values) {
        return java.util.Arrays.stream(values)
            .filter(StringUtils::isNotBlank)
            .findFirst().orElse(null);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
