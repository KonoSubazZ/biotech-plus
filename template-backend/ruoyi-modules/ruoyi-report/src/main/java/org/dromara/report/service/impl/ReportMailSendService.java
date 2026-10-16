package org.dromara.report.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.report.domain.AnalysisReport;
import org.dromara.report.domain.AnalysisReportEmailLog;
import org.dromara.report.domain.ReportMailConfig;
import org.dromara.report.domain.bo.InterpretationSendBo;
import org.dromara.report.mapper.AnalysisReportEmailLogMapper;
import org.dromara.report.mapper.AnalysisReportMapper;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.service.IReportMailConfigService;
import org.dromara.report.service.impl.ReportMailSender;
import org.dromara.report.utils.ReportStatus;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 报告发送：把「待发送」的报告（签名成功后 60）作为附件发出，并留审计痕、把报告置 62 完成。
 * <p>
 * 刻意**不加 {@code @Transactional}**：发信是外部副作用，事务回滚不能把发出的邮件收回来；
 * 所以顺序是「先写 SENDING 日志（独立提交）→ 发信 → 回写 SUCCESS/FAILED」，
 * 失败也一定留下 FAILED 记录与失败原因（不吞异常，向上抛给页面提示）。
 * <p>
 * 状态口径（样本流转 §8）：可发状态 = 60 报告待发送 / 61 发送失败（重发）；
 * 43 报告审核成功在签名流程（M7）落地前也放行，落地后收紧为 60/61。
 * 发送成功 → 62（终态＝完成报告），失败 → 61（可重发），同时写
 * {@code analysis_report.report_sent_by / report_sent_at}。
 *
 * @author <你的名字>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportMailSendService {

    /** 允许发送的报告状态：待发送（60）/ 发送失败可重发（61）；43 在签名流程落地前放行 */
    private static final List<Integer> SENDABLE_STATUS =
        List.of(ReportStatus.PENDING_SEND, ReportStatus.SEND_FAILED);

    /** 发送成功＝完成报告（终态） */
    private static final int STATUS_SENT = ReportStatus.COMPLETED;

    /** 邮件模板标识（写进审计日志） */
    private static final String TEMPLATE_CODE = "REPORT_SEND";

    /** 失败原因入库前截断长度（列宽 1000） */
    private static final int FAILURE_REASON_MAX = 1000;

    private final InterpretationMapper interpretationMapper;

    private final AnalysisReportMapper analysisReportMapper;

    private final AnalysisReportEmailLogMapper emailLogMapper;

    private final ReportFileService reportFileService;

    private final IReportMailConfigService mailConfigService;

    private final ReportMailSender mailSender;

    /**
     * 发送报告邮件。
     *
     * @param bo 入参（analysisId / reportId / to / cc / subject / content / attachReport / configId）
     * @return {traceId, messageId, recipientCount, fromEmail}
     */
    public Map<String, Object> send(InterpretationSendBo bo) {
        Map<String, Object> report = loadReport(bo.getReportId(), bo.getAnalysisId());
        requireSendable(report);
        // 先取发件邮箱：配置有问题（没配/停用/缺密码）要在这里就拦掉，不要先落一条 SENDING 日志
        ReportMailConfig config = mailConfigService.loadForSend(bo.getConfigId());

        List<String> recipients = splitAddresses(bo.getTo());
        AnalysisReportEmailLog logRow = insertSendingLog(bo, recipients);
        try {
            String messageId = mailSender.send(config, bo.getTo(), bo.getCc(), bo.getSubject(),
                toHtml(bo.getContent()), resolveAttachments(bo));
            markSuccess(logRow.getId(), messageId);
            markReportSent(bo.getReportId());
            return buildResult(logRow.getTraceId(), messageId, recipients.size(), config.getFromEmail());
        } catch (RuntimeException e) {
            markFailed(logRow.getId(), e.getMessage());
            markReportSendFailed(bo.getReportId());
            throw e instanceof ServiceException serviceException
                ? serviceException
                : new ServiceException("邮件发送失败：" + e.getMessage());
        }
    }

    /** 取报告行并校验属于该分析批次 */
    private Map<String, Object> loadReport(Long reportId, Long analysisId) {
        Map<String, Object> report = interpretationMapper.selectReportRow(reportId);
        if (report == null) {
            throw new ServiceException("报告不存在或已删除：reportId=" + reportId, 400);
        }
        if (!String.valueOf(analysisId).equals(String.valueOf(report.get("analysisId")))) {
            throw new ServiceException("报告与分析批次不匹配：reportId=" + reportId + "，analysisId=" + analysisId, 400);
        }
        return report;
    }

    /** 只有「报告待发送」能发送（发送失败的可重发） */
    private void requireSendable(Map<String, Object> report) {
        int status = Integer.parseInt(String.valueOf(report.get("status")));
        if (!SENDABLE_STATUS.contains(status)) {
            throw new ServiceException("只有「报告待发送」的报告才能发送，当前状态："
                + ReportStatus.label(status) + "，请先完成审核与签名", 400);
        }
    }

    /** 附件：默认带上报告制品；页面显式取消（attachReport=false）时不带（此时返回空 File[]，由调用方按「不带附件」发） */
    private File[] resolveAttachments(InterpretationSendBo bo) {
        if (Boolean.FALSE.equals(bo.getAttachReport())) {
            return new File[0];
        }
        return new File[]{reportFileService.loadReportFile(bo.getReportId(), bo.getAnalysisId())};
    }

    /** 写一条 SENDING 日志（必须在发信之前，发信失败的痕迹才有落点） */
    private AnalysisReportEmailLog insertSendingLog(InterpretationSendBo bo, List<String> recipients) {
        AnalysisReportEmailLog row = new AnalysisReportEmailLog();
        row.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        row.setAnalysisId(bo.getAnalysisId());
        row.setReportId(bo.getReportId());
        row.setRecipients(String.join(";", recipients));
        row.setRecipientCount((long) recipients.size());
        row.setTemplateCode(TEMPLATE_CODE);
        row.setStatus("SENDING");
        row.setTriggeredBy(LoginHelper.getUsername());
        row.setRequestedAt(new Date());
        emailLogMapper.insert(row);
        return row;
    }

    private void markSuccess(Long logId, String messageId) {
        emailLogMapper.update(null, Wrappers.<AnalysisReportEmailLog>lambdaUpdate()
            .eq(AnalysisReportEmailLog::getId, logId)
            .set(AnalysisReportEmailLog::getStatus, "SUCCESS")
            .set(AnalysisReportEmailLog::getProviderMessageId, messageId)
            .set(AnalysisReportEmailLog::getFinishedAt, new Date()));
    }

    private void markFailed(Long logId, String reason) {
        emailLogMapper.update(null, Wrappers.<AnalysisReportEmailLog>lambdaUpdate()
            .eq(AnalysisReportEmailLog::getId, logId)
            .set(AnalysisReportEmailLog::getStatus, "FAILED")
            .set(AnalysisReportEmailLog::getFailureReason, truncate(reason))
            .set(AnalysisReportEmailLog::getFinishedAt, new Date()));
    }

    /**
     * 发送成功后把报告置 62（完成报告）并记录发送人与时间。
     * <p>
     * 注意：{@code update(null, wrapper)} 这种只给 Wrapper 的写法**不会触发** BaseEntity 的
     * update_time 自动填充（MyBatis-Plus 只对实体填充），所以这里必须显式 set update_time —— 
     * 样本流转页靠 update_time 判断数据有没有变化。
     */
    private void markReportSent(Long reportId) {
        analysisReportMapper.update(null, Wrappers.<AnalysisReport>lambdaUpdate()
            .eq(AnalysisReport::getReportId, reportId)
            .set(AnalysisReport::getReportStatus, STATUS_SENT)
            .set(AnalysisReport::getReportSentBy, LoginHelper.getUserId())
            .set(AnalysisReport::getReportSentAt, new Date())
            .set(AnalysisReport::getUpdateTime, new Date()));
    }

    /** 发送失败（网络异常/收件地址错等）：报告置 61，页面可修改后重发；失败原因在邮件审计日志里 */
    private void markReportSendFailed(Long reportId) {
        analysisReportMapper.update(null, Wrappers.<AnalysisReport>lambdaUpdate()
            .eq(AnalysisReport::getReportId, reportId)
            .set(AnalysisReport::getReportStatus, ReportStatus.SEND_FAILED)
            .set(AnalysisReport::getUpdateTime, new Date()));
    }

    private Map<String, Object> buildResult(String traceId, String messageId, int recipientCount, String fromEmail) {
        Map<String, Object> result = new HashMap<>();
        result.put("traceId", traceId);
        result.put("messageId", messageId);
        result.put("recipientCount", recipientCount);
        result.put("fromEmail", fromEmail);
        return result;
    }

    /** 正文换行统一转 &lt;br&gt;（与生产 SendEmailController 同口径） */
    private String toHtml(String content) {
        return content.replace("\r\n", "<br>").replace("\n", "<br>");
    }

    /** 逗号 / 分号分隔 → 去空去重（保持输入顺序） */
    private List<String> splitAddresses(String addresses) {
        return Arrays.stream(addresses.split("[,;]"))
            .map(String::trim)
            .filter(address -> !address.isEmpty())
            .distinct()
            .toList();
    }

    private String truncate(String reason) {
        String text = StringUtils.isBlank(reason) ? "未知原因" : reason;
        return text.length() > FAILURE_REASON_MAX ? text.substring(0, FAILURE_REASON_MAX) : text;
    }
}
