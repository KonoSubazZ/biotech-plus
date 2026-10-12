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
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 报告发送：把已审核的报告作为附件发出，并留审计痕、把报告置「已发送」。
 * <p>
 * 刻意**不加 {@code @Transactional}**：发信是外部副作用，事务回滚不能把发出的邮件收回来；
 * 所以顺序是「先写 SENDING 日志（独立提交）→ 发信 → 回写 SUCCESS/FAILED」，
 * 失败也一定留下 FAILED 记录与失败原因（不吞异常，向上抛给页面提示）。
 * <p>
 * 状态口径（report-flow.md）：只有 APPROVED 能发，成功后 APPROVED → SENT（终态），
 * 同时写 {@code analysis_report.report_sent_by / report_sent_at}。
 *
 * @author <你的名字>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportMailSendService {

    /** 允许发送的报告状态 */
    private static final String STATUS_APPROVED = "APPROVED";

    /** 发送成功后的报告状态 */
    private static final String STATUS_SENT = "SENT";

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
        requireApproved(report);
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

    /** 只有「已审核」能发送 */
    private void requireApproved(Map<String, Object> report) {
        String status = String.valueOf(report.get("status"));
        if (!STATUS_APPROVED.equals(status)) {
            throw new ServiceException("只有「已审核」的报告才能发送，当前状态：" + status + "，请先到「审核报告」通过", 400);
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

    /** 发送成功后把报告置 SENT 并记录发放人与时间 */
    private void markReportSent(Long reportId) {
        analysisReportMapper.update(null, Wrappers.<AnalysisReport>lambdaUpdate()
            .eq(AnalysisReport::getReportId, reportId)
            .set(AnalysisReport::getStatus, STATUS_SENT)
            .set(AnalysisReport::getReportSentBy, LoginHelper.getUsername())
            .set(AnalysisReport::getReportSentAt, new Date()));
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
