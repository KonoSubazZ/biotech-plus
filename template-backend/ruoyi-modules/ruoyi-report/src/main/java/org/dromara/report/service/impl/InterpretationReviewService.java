package org.dromara.report.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.satoken.utils.TenantContext;
import org.dromara.report.domain.bo.InterpretationPreviewBo;
import org.dromara.report.domain.bo.InterpretationReviewBo;
import org.dromara.report.domain.vo.PreviewVariantVo;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.service.IInterpretationPreviewService;
import org.dromara.report.service.IReportSignService;
import org.dromara.report.service.impl.preview.ReportVariantEdits;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.dromara.report.utils.ReportStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.dromara.report.service.impl.PreviewSupport.asString;
import org.dromara.report.service.impl.preview.PreviewContextBuilder;
import org.dromara.report.mapper.ReportRevisionMapper;
import org.dromara.report.service.impl.revision.ReportArtifactStore;
import org.dromara.report.service.impl.revision.ReportOperationLock;
import org.dromara.report.service.impl.revision.ReportRevisionPolicy;
import org.dromara.report.domain.vo.ReportTemplateData;

/**
 * 报告审核：状态流转 + 审核通过时冻结匹配历史。
 * <p>
 * 冻结时机是「<b>审核通过（40/41 → 43）</b>」：先冻结、再改状态，同一事务；驳回的报告不产生历史。
 * 冻结的是「报告已生成 JSON 里的参数」算出的匹配结果（history_* 冻结、NKB 更新不刷新），
 * 保证审的、出报告的是同一套结果。预览/生成/提交审核都只读（复用历史照常，但不写新行、不登记复用）。
 * <p>
 * 状态机（数字码见 {@link ReportStatus}）：
 * <pre>
 * 32 --首次提交--> 40 报告待审核（复审提交直接分配原审核人，进入 41） --审核通过--> 43 报告审核成功（随后自动送签）
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
    private final ReportRevisionMapper revisionMapper;
    private final ReportArtifactStore artifactStore;
    private final ReportOperationLock operationLock;
    private final ReportFileService reportFileService;

    private final ObjectMapper objectMapper;

    /** 制品根目录（与预览/生成同一配置项）：提交前核对制品里的位点说明与库里覆盖表是否一致 */
    @Value("${report.artifact.root:/app/reports}")
    private String artifactRoot;

    /**
     * 提交解读：只把报告状态置为 40（报告待审核），**不冻结匹配历史**。
     * <p>
     * 历史冻结在「审核通过」时做（见 {@link #approveLocked}）：驳回的报告不产生任何历史，
     * 避免未通过审核的匹配结果污染 history_*。
     * 只有已生成报告（有成品文件）才能提交 —— 审核对象就是那份生成的报告。
     *
     * @param bo 入参（analysisId / reportId / templateId / disease / reportDate / analyzer，与生成接口同参）
     */
    @Transactional(rollbackFor = Exception.class)
    public void submit(InterpretationPreviewBo bo) {
        operationLock.run(bo.getReportId(), () -> submitLocked(bo));
    }

    private void submitLocked(InterpretationPreviewBo bo) {
        Map<String, Object> report = contextBuilder.loadReport(bo.getReportId(), bo.getAnalysisId());
        int status = currentStatus(report);
        if (status != ReportStatus.GENERATED) {
            throw new ServiceException("当前状态（" + ReportStatus.label(status)
                + "）不允许提交解读：reportId=" + bo.getReportId());
        }

        reportFileService.loadReportFile(bo.getReportId(), bo.getAnalysisId());
        // 位点人工修改是报告正文的一部分：生成后又改过、却没重新生成时，送审的 DOCX 还是旧内容 —— 拦住
        assertVariantEditsMatched(report);
        Long reviewerId = optionalLong(report.get("reportReviewerId"));
        if (reviewerId != null) {
            requireAvailableReviewer(report, reviewerId);
        }
        String reportFile = StringUtils.hasText(asString(report.get("reportFilePath")))
            ? asString(report.get("reportFilePath")) : asString(report.get("reportFileRawPath"));
        if (!StringUtils.hasText(reportFile)) {
            throw new ServiceException("报告尚未生成，请先在「报告预览」生成报告：reportId=" + bo.getReportId());
        }

        int affected = revisionMapper.submitReview(bo.getReportId(), reviewerId);
        if (affected == 0) {
            throw new ServiceException("报告状态已变化，请刷新后重试：reportId=" + bo.getReportId());
        }
        log.info("报告已提交审核：reportId={}，analysisId={}（历史冻结在审核通过时执行）", bo.getReportId(), bo.getAnalysisId());
    }

    /**
     * 提交审核前核对：制品 JSON 必须已经体现 {@code report_variant_edits} 里的全部人工修改。
     * <p>
     * 场景：生成后又改了父靶点/临床意义/突变说明却没重新生成 → 送审的 DOCX 还是旧内容。
     * 不一致直接拒绝提交，让解读先重新生成。
     *
     * @param report 报告行（含 reportJsonPath / reportVariantEdits）
     */
    private void assertVariantEditsMatched(Map<String, Object> report) {
        String jsonPath = asString(report.get("reportJsonPath"));
        Map<String, ReportVariantEdits.Edit> edits = ReportVariantEdits.parse(asString(report.get("reportVariantEdits")));
        if (edits.isEmpty() || !StringUtils.hasText(jsonPath)) {
            return;
        }
        Path file = Paths.get(jsonPath).toAbsolutePath().normalize();
        Path root = Paths.get(artifactRoot).toAbsolutePath().normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            return;
        }
        try {
            ReportTemplateData data = objectMapper.readValue(
                Files.readString(file, StandardCharsets.UTF_8), ReportTemplateData.class);
            Map<String, PreviewVariantVo> items = new java.util.LinkedHashMap<>();
            if (data.getSomaticVariants() != null && data.getSomaticVariants().getItems() != null) {
                data.getSomaticVariants().getItems().forEach(item -> items.put(ReportVariantEdits.key(item), item));
            }
            if (data.getGermlineVariants() != null && data.getGermlineVariants().getItems() != null) {
                data.getGermlineVariants().getItems().forEach(item -> items.put(ReportVariantEdits.key(item), item));
            }
            for (Map.Entry<String, ReportVariantEdits.Edit> entry : edits.entrySet()) {
                PreviewVariantVo item = items.get(entry.getKey());
                if (item == null || !sameEdit(entry.getValue(), item)) {
                    throw new ServiceException("位点「" + entry.getKey()
                        + "」的人工修改未写入已生成报告，请重新生成后再提交审核");
                }
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("提交审核前核对位点修改失败（跳过校验）：reportId={}，{}", report.get("reportId"), e.getMessage());
        }
    }

    /** 制品 item 是否体现了这条人工修改 */
    private boolean sameEdit(ReportVariantEdits.Edit edit, PreviewVariantVo item) {
        if (edit.getParentMutationIds() != null) {
            List<Long> expected = edit.getParentMutationIds();
            List<Long> actual = item.getParentMutationIds() == null ? List.of() : item.getParentMutationIds();
            if (!expected.equals(actual)) {
                return false;
            }
        }
        if (edit.getClinicalSignificance() != null
            && !edit.getClinicalSignificance().equals(item.getClinicalSignificance())) {
            return false;
        }
        if (edit.getMutationDescription() != null) {
            String actual = item.getMutationDescription() == null ? "" : item.getMutationDescription().trim();
            if (!edit.getMutationDescription().trim().equals(actual)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 审核通过：先冻结匹配历史，再把状态流转（40/41 → 43 报告审核成功，随后自动送签转 50）。
     * <p>
     * 冻结与状态更新在**同一事务**：freezeFromArtifact 抛错则审核通过整体回滚，不会出现「已通过但历史没冻结」。
     * 冻结只读报告 JSON 制品，不再现场重算（报告内容 = 历史冻结结果）。
     * 与生成时同一套，不信前端传参 —— 否则审核时参数一变就会冻出与报告不一致的历史。
     * 状态更新带来源状态条件，重复点击/并发时后到的那次影响行数为 0，直接报错让用户刷新。
     *
     * @param bo 入参（analysisId / reportId）
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(InterpretationPreviewBo bo) {
        operationLock.run(bo.getReportId(), () -> approveLocked(bo));
    }

    private void approveLocked(InterpretationPreviewBo bo) {
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

        // 历史冻结（2026-10-27 用户口径）：提交审核不再写历史，改到「审核通过」时冻结
        // （历史检出与解读历史都不因提交/驳回产生，避免驳回报告污染历史）
        previewService.freezeFromArtifact(bo.getReportId(), bo.getAnalysisId());

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
        operationLock.run(bo.getReportId(), () -> rejectLocked(bo));
    }

    private void rejectLocked(InterpretationReviewBo bo) {
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

        report.put("rejectedBy", LoginHelper.getUserId());
        report.put("rejectedAt", java.time.LocalDateTime.now().toString());
        artifactStore.rejected(report, bo.getReason().trim());
        // 文件已移到 report/rejected/<analysisDate>/，库里不再保留失效路径（归档位置不入库）
        interpretationMapper.clearReportArtifact(bo.getReportId());

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
        Long reviewer = optionalLong(report.get("reportReviewerId"));
        if (reviewer == null) {
            reviewer = optionalLong(report.get("reportReviewClaimedBy"));
        }
        ReportRevisionPolicy.requireReviewer(LoginHelper.getUserId(),
            optionalLong(report.get("reportGeneratedBy")), reviewer);
    }

    private void requireAvailableReviewer(Map<String, Object> report, Long reviewerId) {
        if (reviewerId.equals(optionalLong(report.get("reportGeneratedBy")))) {
            throw new ServiceException("原审核人是本次产出人，不能提交定向复审");
        }
        int available = TenantContext.withoutTenant(() ->
            revisionMapper.countReviewer(reviewerId, asString(report.get("tenantId"))));
        if (available == 0) {
            throw new ServiceException("原审核人已停用或无审核权限，请联系管理员恢复审核资格");
        }
    }

    private Long optionalLong(Object value) {
        if (value == null) {
            return null; // 原始公共待审核报告尚无指定审核人。
        }
        return Long.valueOf(value.toString());
    }

    /** 当前登录用户ID（字符串口径，与 report_generated_by / report_checked_by 一致） */
    private String currentUserId() {
        return LoginHelper.getUserId() == null ? null : String.valueOf(LoginHelper.getUserId());
    }
}
