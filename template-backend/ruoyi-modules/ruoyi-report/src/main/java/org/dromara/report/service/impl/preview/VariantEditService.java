package org.dromara.report.service.impl.preview;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.report.domain.bo.InterpretationVariantEditBo;
import org.dromara.report.domain.dto.PreviewContext;
import org.dromara.report.domain.vo.InterpretationContextVo;
import org.dromara.report.domain.vo.NkbVariantNodeVo;
import org.dromara.report.domain.vo.PreviewDrugVo;
import org.dromara.report.domain.vo.PreviewVariantVo;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.mapper.NkbEvidenceMapper;
import org.dromara.report.service.IInterpretationService;
import org.dromara.report.service.impl.NkbDrugMatcher;
import org.dromara.report.service.impl.revision.ReportRevisionPolicy;
import org.dromara.report.utils.ReportStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.dromara.report.service.impl.PreviewSupport.asInt;
import static org.dromara.report.service.impl.PreviewSupport.asLong;
import static org.dromara.report.service.impl.PreviewSupport.asString;
import static org.dromara.report.service.impl.PreviewSupport.nkb;

/**
 * 位点人工修改：父靶点（体细胞/胚系）、胚系临床意义、突变说明，统一写
 * {@code analysis_report.report_variant_edits}（报告粒度 JSON）。
 * <p>
 * 口径（2026-11-05）：
 * <ul>
 *   <li>只有未入库位点可改（history_* 无该 match_key）；来源=历史只读，这里直接拒绝</li>
 *   <li>不写 history_*、不写 {@code file_*.parent_mutation_id}：修改只影响本报告生成的 JSON</li>
 *   <li>审核通过冻结时从报告 JSON 派生 history_*，不读本表</li>
 * </ul>
 *
 * @author <你的名字>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VariantEditService {

    /** 单条突变说明最大长度（整段文本，够长但防脏数据） */
    private static final int MAX_NOTE_LENGTH = 4000;

    /** 一份报告的修改表总量上限（防有人把整篇文档粘进来） */
    private static final int MAX_EDITS_BYTES = 64 * 1024;

    private final InterpretationMapper interpretationMapper;

    private final NkbEvidenceMapper nkbEvidenceMapper;

    private final IInterpretationService interpretationService;

    private final NkbDrugMatcher drugMatcher;

    private final PreviewContextBuilder contextBuilder;

    private final VariantMatcher variantMatcher;

    /**
     * 保存位点人工修改（部分字段更新，null = 不改）。
     *
     * @param bo 入参（analysisId / reportId / sourceType / sourceId / parentMutationIds / clinicalSignificance / mutationDescription）
     */
    public void updateVariantEdits(InterpretationVariantEditBo bo) {
        Map<String, Object> report = contextBuilder.loadReport(bo.getReportId(), bo.getAnalysisId());
        Integer status = asInt(report.get("status"));
        if (status == null || !ReportRevisionPolicy.isGeneratable(status)) {
            throw new ServiceException("当前状态（" + ReportStatus.label(status == null ? -1 : status)
                + "）不允许修改位点：报告已送审或已发送");
        }
        Map<String, Object> row = interpretationMapper.selectVariantForTarget(bo.getSourceType(), bo.getSourceId(),
            bo.getAnalysisId());
        if (row == null) {
            throw new ServiceException("位点不存在或不属于该分析批次：" + bo.getSourceType() + "#" + bo.getSourceId());
        }
        String gene = asString(row.get("gene"));
        String variant = asString(row.get("variant"));
        String oriVariant = asString(row.get("oriVariant"));
        boolean germline = "CR_ALL".equals(bo.getSourceType());
        if (!germline && bo.getClinicalSignificance() != null) {
            throw new ServiceException("只有胚系位点可以人工确认临床意义：" + bo.getSourceType());
        }
        requireNotInHistory(bo, report, gene, variant, oriVariant, germline);
        requireValidTarget(bo, report, gene, variant, oriVariant);
        requireValidNote(bo.getMutationDescription());

        String key = ReportVariantEdits.key(gene, variant, bo.getSourceType(), bo.getSourceId());
        ReportVariantEdits.Edit patch = new ReportVariantEdits.Edit(
            bo.getParentMutationIds(), bo.getClinicalSignificance(), bo.getMutationDescription());
        String merged = ReportVariantEdits.merge(asString(report.get("reportVariantEdits")), key, patch);
        if (merged != null && merged.getBytes(StandardCharsets.UTF_8).length > MAX_EDITS_BYTES) {
            throw new ServiceException("该报告的位点修改总量超限（>64KB），请精简后再保存");
        }
        int affected = interpretationMapper.updateReportVariantEdits(
            bo.getReportId(), bo.getAnalysisId(), merged, LoginHelper.getUserId());
        if (affected == 0) {
            throw new ServiceException("报告状态已变化，请刷新后重试：reportId=" + bo.getReportId());
        }
        log.info("位点修改已保存：reportId={}，key={}，parents={}，significance={}，note={}",
            bo.getReportId(), key, bo.getParentMutationIds(), bo.getClinicalSignificance(),
            StringUtils.hasText(bo.getMutationDescription()) ? "有" : "无");
    }

    /**
     * 来源=历史（已入库）只读：按 matchKey 查 history_*，命中即拒绝。
     */
    private void requireNotInHistory(InterpretationVariantEditBo bo, Map<String, Object> report,
                                     String gene, String variant, String oriVariant, boolean germline) {
        InterpretationContextVo context = interpretationService.loadContext(bo.getReportId(), bo.getAnalysisId());
        PreviewContext pc = contextBuilder.build(bo.getAnalysisId(), bo.getReportId(), report, context);
        PreviewVariantVo item = new PreviewVariantVo();
        item.setSourceType(bo.getSourceType());
        item.setGene(gene);
        item.setVariant(variant);
        item.setOriVariant(oriVariant);
        item.setMutationType(germline ? "G" : "S");
        String matchKey = variantMatcher.matchKey(pc, item);
        Map<String, Object> history = germline
            ? interpretationMapper.selectGermlineHistory(matchKey)
            : interpretationMapper.selectSomaticHistory(matchKey);
        if (history != null) {
            throw new ServiceException("该位点已入库（来源=历史），只读；只有未入库位点可以人工修改");
        }
    }

    /**
     * 父靶点校验：节点必须在知识库（Approved），且改靶后必须能出药物证据。
     * 空列表 = 取消改靶（合法）。
     */
    private void requireValidTarget(InterpretationVariantEditBo bo, Map<String, Object> report,
                                    String gene, String variant, String oriVariant) {
        List<Long> parentIds = ParentMutationIds.distinct(bo.getParentMutationIds());
        if (parentIds.isEmpty()) {
            return;
        }
        requireApprovedNodes(parentIds);
        InterpretationContextVo context = interpretationService.loadContext(bo.getReportId(), bo.getAnalysisId());
        PreviewContext pc = contextBuilder.build(bo.getAnalysisId(), bo.getReportId(), report, context);
        NkbDrugMatcher.MatchResult matched = drugMatcher.match(gene, variant, oriVariant,
            "CR_ALL".equals(bo.getSourceType()), pc.getDiseaseScope(), parentIds);
        List<PreviewDrugVo> evidence = matched.getEvidence() == null ? List.of() : matched.getEvidence();
        if (evidence.isEmpty()) {
            throw new ServiceException("改靶后无药物信息，此操作无法保存");
        }
    }

    private void requireValidNote(String mutationDescription) {
        if (mutationDescription != null && mutationDescription.trim().length() > MAX_NOTE_LENGTH) {
            throw new ServiceException("突变说明不能超过 " + MAX_NOTE_LENGTH + " 字");
        }
    }

    /**
     * 改靶候选：按关键词查 NKB 位点节点（Approved），供人工指定父级。
     *
     * @param keyword 关键词（基因符号 / 节点名片段）
     * @return 候选节点列表（最多 50 条）
     */
    public List<NkbVariantNodeVo> searchParentNodes(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new ServiceException("请输入基因或位点关键词");
        }
        List<Map<String, Object>> rows = nkb(() -> nkbEvidenceMapper.selectVariantCandidates(keyword.trim(), 50));
        List<NkbVariantNodeVo> result = new ArrayList<>();
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            NkbVariantNodeVo vo = new NkbVariantNodeVo();
            vo.setMutationId(asLong(row.get("mutationId")));
            vo.setGene(asString(row.get("gene")));
            vo.setVariantName(asString(row.get("variantName")));
            vo.setEffectText(asString(row.get("effectText")));
            result.add(vo);
        }
        return result;
    }

    /**
     * 校验父级都来自知识库（Approved 节点）。
     *
     * @param parentIds 父级节点ID（已去重）
     */
    private void requireApprovedNodes(List<Long> parentIds) {
        List<String> names = nkb(() -> nkbEvidenceMapper.selectVariantNames(parentIds));
        if (names == null || names.size() < parentIds.size()) {
            throw new ServiceException("有父级节点在知识库中不存在或未审核，请重新选择");
        }
    }
}
