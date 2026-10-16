package org.dromara.report.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.report.domain.bo.InterpretationPreviewBo;
import org.dromara.report.domain.bo.InterpretationReviewBo;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.service.IInterpretationPreviewService;
import org.dromara.report.service.IReportSignService;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.dromara.report.utils.ReportStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

import static org.dromara.report.service.impl.PreviewSupport.asString;
import org.dromara.report.service.impl.preview.PreviewContextBuilder;

/**
 * 报告审核：状态流转 + 提交审核时冻结匹配历史。
 * <p>
 * 冻结时机是「<b>提交审核</b>」：报告一旦送审，本次解读的位点匹配结果就是审核对象，
 * 必须把结果固定下来（history_* 冻结、NKB 更新不刷新），否则审到一半知识库变了会「审的和出报告的不是一回事」。
 * 预览与生成仍然只读（复用历史照常，但不写新行、不登记复用），未提交的报告不占用匹配历史。
 * <p>
 * 状态机（数字码见 {@link ReportStatus}）：
 * <pre>
 * 31/32/33 --提交解读--> 40 报告待审核 --审核通过--> 43 报告审核成功（随后自动送签）
 *                                    \--审核不通过--> 回退 31 报告解读中（同一条记录，带回退原因）
 * </pre>
 * 「认领」（30→31、40→41）由样本流转/详情页的动作接口负责，这里只管审核动作本身。
 *
 * @author <你的名字>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterpretationReviewService {

    /** 允许「提交解读」的来源状态：解读中 / 生成成功 / 生成失败（都还能再提交一次） */
    private static final List<Integer> SUBMITTABLE_STATUS =
        List.of(ReportStatus.INTERPRETING, ReportStatus.GENERATED, ReportStatus.GENERATE_FAILED);

    /** 提交解读后的报告状态：报告待审核 */
    private static final int STATUS_PENDING_REVIEW = ReportStatus.PENDING_REVIEW;

    /** 允许「审核通过 / 审核不通过」的来源状态：待审核（未认领也能审）、审核中（认领后审） */
    private static final List<Integer> REVIEWABLE_STATUS =
        List.of(ReportStatus.PENDING_REVIEW, ReportStatus.REVIEWING);

    /** 审核通过后的报告状态 */
    private static final int STATUS_APPROVED = ReportStatus.REVIEW_APPROVED;

    /** 审核不通过后的落点状态：回退到「报告解读中」 */
    private static final int STATUS_REVERTED = ReportStatus.INTERPRETING;

    private final InterpretationMapper interpretationMapper;

    private final PreviewContextBuilder contextBuilder;

    private final IInterpretationPreviewService previewService;

    /** 报告签名通道（上上签未对接期间是假实现：只落状态链，见 ReportSignStubService） */
    private final IReportSignService reportSignService;

    /**
     * 提交解读：先冻结匹配历史，再把报告状态置为 40（报告待审核）。
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
        int status = currentStatus(report);
        if (!SUBMITTABLE_STATUS.contains(status)) {
            throw new ServiceException("当前状态（" + ReportStatus.label(status)
                + "）不允许提交解读：reportId=" + bo.getReportId());
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
     * 审核通过：只做状态流转（40/41 → 43 报告审核成功，随后自动送签转 50）。
     * <p>
     * 匹配历史在「提交审核」时就已经冻结，这里不再重算 —— 否则审核时页面参数一变就会多冻出一条不一致的历史。
     * 状态更新带来源状态条件，重复点击/并发时后到的那次影响行数为 0，直接报错让用户刷新。
     *
     * @param bo 入参（analysisId / reportId）
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(InterpretationPreviewBo bo) {
        Map<String, Object> report = contextBuilder.loadReport(bo.getReportId(), bo.getAnalysisId());
        int status = currentStatus(report);
        if (STATUS_APPROVED == status) {
            throw new ServiceException("报告已审核通过，无需重复审核：reportId=" + bo.getReportId());
        }
        if (!REVIEWABLE_STATUS.contains(status)) {
            throw new ServiceException("当前状态（" + ReportStatus.label(status)
                + "）不允许审核通过：reportId=" + bo.getReportId());
        }
        assertNotProducer(report);

        int affected = interpretationMapper.updateReportReview(bo.getReportId(), STATUS_APPROVED, null,
            LoginHelper.getUserId(), LoginHelper.getUserId(), REVIEWABLE_STATUS, null, null);
        if (affected == 0) {
            throw new ServiceException("报告状态已变化，请刷新后重试：reportId=" + bo.getReportId());
        }
        log.info("报告审核通过：reportId={}，analysisId={}", bo.getReportId(), bo.getAnalysisId());

        // 口径：审核通过（43）后自动送签 → 50 报告签名中。
        // 送签必须等「审核通过」这个事务**提交之后**再做：送签在独立事务里（见 IReportSignService#submit），
        // 事务没提交它读不到 43，会被状态校验拦下；提交后再送签，送签失败也只落 52，不会回滚审核通过。
        Long reportId = bo.getReportId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    autoSign(reportId);
                }
            });
        } else {
            autoSign(reportId);
        }
    }

    /**
     * 审核通过后自动送签（事务提交后执行）：送签失败落 52 报告签名失败 + 原因，样本留在「报告签名」页签可重送。
     * 这里把异常吞掉——「审核通过」已经成立，不能因为送签通道问题把用户看到的结果改回去。
     */
    private void autoSign(Long reportId) {
        try {
            reportSignService.submit(reportId);
        } catch (RuntimeException e) {
            log.warn("审核通过后自动送签失败，状态落 52 报告签名失败：reportId={} 原因={}", reportId, e.getMessage());
            interpretationMapper.updateReportSignResult(reportId, ReportStatus.SIGN_FAILED, null, e.getMessage());
        }
    }

    /**
     * 驳回：把报告回退到 31（报告解读中，同一条记录），原因写进 comment 与回退字段。
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
        int status = currentStatus(report);
        if (!REVIEWABLE_STATUS.contains(status)) {
            throw new ServiceException("当前状态（" + ReportStatus.label(status)
                + "）不允许驳回：reportId=" + bo.getReportId());
        }
        assertNotProducer(report);

        // 审核不通过：同一条报告回退到「报告解读中」，来源状态与原因留在回退字段里
        // （样本流转「报告解读中」页签用它打「审核失败回退」标签，与正常认领区分）
        int affected = interpretationMapper.updateReportReview(bo.getReportId(), STATUS_REVERTED,
            bo.getReason().trim(), LoginHelper.getUserId(), LoginHelper.getUserId(), REVIEWABLE_STATUS,
            status, bo.getReason().trim());
        if (affected == 0) {
            throw new ServiceException("报告状态已变化，请刷新后重试：reportId=" + bo.getReportId());
        }
        log.info("报告已驳回：reportId={}，analysisId={}", bo.getReportId(), bo.getAnalysisId());
    }

    /** 报告行里的状态码（XML 把 report_status 别名为 status） */
    private int currentStatus(Map<String, Object> report) {
        Object value = report.get("status");
        if (value == null) {
            throw new ServiceException("报告状态缺失：reportId=" + asString(report.get("reportId")));
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new ServiceException("报告状态非法（" + value + "）：reportId=" + asString(report.get("reportId")));
        }
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
