package org.dromara.report.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.report.domain.bo.InterpretationPreviewBo;
import org.dromara.report.domain.bo.InterpretationReviewBo;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.service.IInterpretationPreviewService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

import static org.dromara.report.service.impl.PreviewSupport.asString;

/**
 * 报告审核：状态流转 + 提交审核时冻结匹配历史。
 * <p>
 * 冻结时机是「<b>提交审核</b>」：报告一旦送审，本次解读的位点匹配结果就是审核对象，
 * 必须把结果固定下来（history_* 冻结、NKB 更新不刷新），否则审到一半知识库变了会「审的和出报告的不是一回事」。
 * 预览与生成仍然只读（复用历史照常，但不写新行、不登记复用），未提交的报告不占用匹配历史。
 * <p>
 * 状态机（设计书 §11.1）：
 * <pre>
 * INTERPRETING --提交审核--> PENDING_REVIEW --通过--> APPROVED --发送--> SENT
 *                                      \--驳回--> REJECTED --重新提交--> PENDING_REVIEW
 * </pre>
 * 目前实现「提交审核」与「审核通过」；驳回（REJECTED）、发送（SENT）按页面逐个排期。
 *
 * @author <你的名字>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterpretationReviewService {

    /** 允许「提交审核」的来源状态（驳回后可重新提交） */
    private static final List<String> SUBMITTABLE_STATUS = List.of("INTERPRETING", "REJECTED");

    /** 提交审核后的报告状态 */
    private static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";

    /** 允许「审核通过」的来源状态：设计书 §11.1 只允许待审核 */
    private static final List<String> APPROVABLE_STATUS = List.of("PENDING_REVIEW");

    /** 审核通过后的报告状态 */
    private static final String STATUS_APPROVED = "APPROVED";

    /** 允许「驳回」的来源状态：只有待审核 */
    private static final List<String> REJECTABLE_STATUS = List.of("PENDING_REVIEW");

    /** 驳回后的报告状态 */
    private static final String STATUS_REJECTED = "REJECTED";

    private final InterpretationMapper interpretationMapper;

    private final PreviewContextBuilder contextBuilder;

    private final IInterpretationPreviewService previewService;

    /**
     * 提交审核：先冻结匹配历史，再把报告状态置为 PENDING_REVIEW。
     * <p>
     * 两件事在同一事务里：冻结失败（NKB 异常等）状态就不会变，避免出现「已送审但历史没冻结」的报告。
     * 只有已生成报告（有成品文件）才能提交 —— 审核对象就是那份生成的报告。
     * 冻结用的是页面上那套「报告生成参数」（与生成时同一套）：参数不同癌种范围就不同，冻结结果也会不同，
     * 所以前端必须把生成时的参数一起传过来（预览页在已生成状态下参数是锁定回填的）。
     *
     * @param bo 入参（analysisId / reportId / templateId / disease / reportDate / analyzer，与生成接口同参）
     */
    @Transactional(rollbackFor = Exception.class)
    public void submit(InterpretationPreviewBo bo) {
        Map<String, Object> report = contextBuilder.loadReport(bo.getReportId(), bo.getAnalysisId());
        String status = asString(report.get("status"));
        if (!SUBMITTABLE_STATUS.contains(status)) {
            throw new ServiceException("当前状态（" + status + "）不允许提交审核：reportId=" + bo.getReportId());
        }
        String reportFile = StringUtils.hasText(asString(report.get("reportFilePath")))
            ? asString(report.get("reportFilePath")) : asString(report.get("reportFileRawPath"));
        if (!StringUtils.hasText(reportFile)) {
            throw new ServiceException("报告尚未生成，请先在「报告预览」生成报告：reportId=" + bo.getReportId());
        }

        previewService.freezeHistory(bo);

        int affected = interpretationMapper.updateReportStatusIfIn(
            bo.getReportId(), STATUS_PENDING_REVIEW, SUBMITTABLE_STATUS);
        if (affected == 0) {
            throw new ServiceException("报告状态已变化，请刷新后重试：reportId=" + bo.getReportId());
        }
        log.info("报告已提交审核并冻结匹配历史：reportId={}，analysisId={}", bo.getReportId(), bo.getAnalysisId());
    }

    /**
     * 审核通过：只做状态流转（INTERPRETING / PENDING_REVIEW → APPROVED）。
     * <p>
     * 匹配历史在「提交审核」时就已经冻结，这里不再重算 —— 否则审核时页面参数一变就会多冻出一条不一致的历史。
     * 状态更新带来源状态条件，重复点击/并发时后到的那次影响行数为 0，直接报错让用户刷新。
     *
     * @param bo 入参（analysisId / reportId）
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(InterpretationPreviewBo bo) {
        Map<String, Object> report = contextBuilder.loadReport(bo.getReportId(), bo.getAnalysisId());
        String status = asString(report.get("status"));
        if (STATUS_APPROVED.equals(status)) {
            throw new ServiceException("报告已审核通过，无需重复审核：reportId=" + bo.getReportId());
        }
        if (!APPROVABLE_STATUS.contains(status)) {
            throw new ServiceException("当前状态（" + status + "）不允许审核通过：reportId=" + bo.getReportId());
        }
        assertNotProducer(report);

        int affected = interpretationMapper.updateReportReview(bo.getReportId(), STATUS_APPROVED, null,
            currentUserId(), LoginHelper.getUserId(), APPROVABLE_STATUS);
        if (affected == 0) {
            throw new ServiceException("报告状态已变化，请刷新后重试：reportId=" + bo.getReportId());
        }
        log.info("报告审核通过：reportId={}，analysisId={}", bo.getReportId(), bo.getAnalysisId());
    }

    /**
     * 驳回：把报告置为 REJECTED，并把驳回原因写进 analysis_report.comment（最新一条）。
     * <p>
     * 只有「待审核」能驳回；原因必填；产出人不能审核自己的报告；
     * 状态更新带来源状态条件，重复点击/并发时后到的那次影响行数为 0，直接报错让用户刷新。
     *
     * @param bo 入参（analysisId / reportId / reason）
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(InterpretationReviewBo bo) {
        if (!StringUtils.hasText(bo.getReason())) {
            throw new ServiceException("驳回原因不能为空");
        }
        Map<String, Object> report = contextBuilder.loadReport(bo.getReportId(), bo.getAnalysisId());
        String status = asString(report.get("status"));
        if (!REJECTABLE_STATUS.contains(status)) {
            throw new ServiceException("当前状态（" + status + "）不允许驳回：reportId=" + bo.getReportId());
        }
        assertNotProducer(report);

        int affected = interpretationMapper.updateReportReview(bo.getReportId(), STATUS_REJECTED,
            bo.getReason().trim(), currentUserId(), LoginHelper.getUserId(), REJECTABLE_STATUS);
        if (affected == 0) {
            throw new ServiceException("报告状态已变化，请刷新后重试：reportId=" + bo.getReportId());
        }
        log.info("报告已驳回：reportId={}，analysisId={}", bo.getReportId(), bo.getAnalysisId());
    }

    /** 产出人不能审核自己的报告（report_generated_by 存的是用户ID字符串，与当前登录用户同口径比较） */
    private void assertNotProducer(Map<String, Object> report) {
        String producer = asString(report.get("reportGeneratedBy"));
        if (producer != null && producer.equals(currentUserId())) {
            throw new ServiceException("产出人不能审核自己的报告：reportId=" + asString(report.get("reportId")));
        }
    }

    /** 当前登录用户ID（字符串口径，与 report_generated_by / report_checked_by 一致） */
    private String currentUserId() {
        return LoginHelper.getUserId() == null ? null : String.valueOf(LoginHelper.getUserId());
    }
}
