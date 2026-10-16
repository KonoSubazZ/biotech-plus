package org.dromara.report.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.domain.vo.ReportTemplateData;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.service.IReportSignService;
import org.dromara.report.utils.ReportStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 报告签名的**假实现**：上上签未对接期间顶替签名通道（`report.sign.provider=stub`，缺省就是它）。
 * <p>
 * 它只做状态链，不真的连通道：
 * <ul>
 *   <li>{@link #submit}：生成假流程号 {@code FAKE-SIGN-<reportId>-<时间戳>}，状态 43/52 → 50 报告签名中；</li>
 *   <li>{@link #handleCallback}：签名成功 → 50 → 60 报告待发送（53 报告签名成功是瞬时态，不落库）；</li>
 *   <li>{@link #handleFailure}：签名失败 → 50 → 52 报告签名失败 + 原因。</li>
 * </ul>
 * 接真实通道时：新增 {@code SsqReportSignService implements IReportSignService}（用 {@code ISsqSignService}
 * 的「报告交付」场景），把 {@code report.sign.provider} 改成对应值；本类不加删除也不影响（provider 不匹配就不装配）。
 *
 * @author <你的名字>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "report.sign.provider", havingValue = "stub", matchIfMissing = true)
public class ReportSignStubService implements IReportSignService {

    private final InterpretationMapper interpretationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submit(Long reportId) {
        Map<String, Object> report = interpretationMapper.selectReportRow(reportId);
        if (report == null) {
            throw new ServiceException("报告不存在或已删除：reportId=" + reportId);
        }
        int status = Integer.parseInt(String.valueOf(report.get("status")));
        if (status != ReportStatus.REVIEW_APPROVED && status != ReportStatus.SIGN_FAILED) {
            throw new ServiceException("当前状态（" + ReportStatus.label(status) + "）不允许送签：reportId=" + reportId);
        }

        String flowNo = "FAKE-SIGN-" + reportId + "-" + System.currentTimeMillis();
        int affected = interpretationMapper.updateReportSigning(reportId, flowNo);
        if (affected == 0) {
            throw new ServiceException("报告状态已变化，请刷新后重试：reportId=" + reportId);
        }
        log.info("【假签名】送签成功（未对接上上签，仅落状态）：reportId={} flowNo={}", reportId, flowNo);
        return flowNo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleCallback(String flowNo, String signedPdfUrl) {
        Map<String, Object> report = requireReportByFlowNo(flowNo);
        Long reportId = toLong(report.get("reportId"));
        // 未对接期没有真正的签名后 PDF：签名件路径留空（页面按「signed → pdf → docx」回退取文件）
        int affected = interpretationMapper.updateReportSignResult(reportId, ReportStatus.PENDING_SEND, null, null);
        if (affected == 0) {
            throw new ServiceException("报告不在「签名中」，无法确认签名成功：reportId=" + reportId);
        }
        log.info("【假签名】收到成功回调：reportId={} flowNo={} signedPdfUrl={}（未落签名件）",
            reportId, flowNo, StringUtils.hasText(signedPdfUrl) ? "有" : "无");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleFailure(String flowNo, String reason) {
        Map<String, Object> report = requireReportByFlowNo(flowNo);
        Long reportId = toLong(report.get("reportId"));
        String failReason = StringUtils.hasText(reason) ? reason : "签名通道返回失败";
        // 状态落 52 报告签名失败（带原因）；来源状态允许 43（送签即失败）与 50（回调失败）
        int affected = interpretationMapper.updateReportSignResult(
            reportId, ReportStatus.SIGN_FAILED, null, failReason);
        if (affected == 0) {
            throw new ServiceException("报告不在「签名中」，无法标记签名失败：reportId=" + reportId);
        }
        log.info("【假签名】收到失败回调：reportId={} flowNo={} 原因={}", reportId, flowNo, failReason);
    }

    /** 回调只带流程号：按流程号找报告，找不到直接报错（不猜） */
    private Map<String, Object> requireReportByFlowNo(String flowNo) {
        if (!StringUtils.hasText(flowNo)) {
            throw new ServiceException("签名流程号不能为空");
        }
        Map<String, Object> report = interpretationMapper.selectReportBySignFlowNo(flowNo);
        if (report == null) {
            throw new ServiceException("找不到该签名流程号对应的报告：flowNo=" + flowNo);
        }
        return report;
    }

    private Long toLong(Object value) {
        return value == null ? null : Long.valueOf(String.valueOf(value).trim());
    }
}
