package org.dromara.report.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.domain.bo.InterpretationGermlineSignificanceBo;
import org.dromara.report.domain.bo.InterpretationTargetBo;
import org.dromara.report.domain.vo.InterpretationContextVo;
import org.dromara.report.domain.vo.NkbVariantNodeVo;
import org.dromara.report.domain.vo.PreviewDrugVo;
import org.dromara.report.domain.vo.PreviewVariantVo;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.mapper.NkbEvidenceMapper;
import org.dromara.report.service.IInterpretationService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.dromara.report.service.impl.PreviewSupport.asLong;
import static org.dromara.report.service.impl.PreviewSupport.asString;
import static org.dromara.report.service.impl.PreviewSupport.nkb;

/**
 * 位点人工干预：改靶（人工指定知识库父级）、胚系五级临床意义确认、父级候选检索。
 * <p>
 * 与匹配（{@link VariantMatcher}）分家的理由：匹配是「读知识库出结果」，这里是「人工改数据」，
 * 两者的校验与风险不同 —— 人工改动必须按 en7 口径卡住（父级要在知识库且改靶后必须能出药物证据）。
 * <p>
 * 事务由调用方（{@code InterpretationPreviewServiceImpl} 的实现方法）开启，这里只做业务校验与写库。
 *
 * @author <你的名字>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VariantEditService {

    private final InterpretationMapper interpretationMapper;

    private final NkbEvidenceMapper nkbEvidenceMapper;

    private final IInterpretationService interpretationService;

    private final NkbDrugMatcher drugMatcher;

    private final PreviewContextBuilder contextBuilder;

    private final VariantMatcher variantMatcher;

    /**
     * 人工确认胚系五级临床意义（1~5）。
     * <p>
     * 保存前要求该位点已经建立匹配历史（预览过一次），按匹配键更新；
     * <b>只更新该列</b>，不重跑匹配、不覆盖已冻结的 match_result / 用药 / 位点等级
     * （早先会重跑 en7 匹配「顺手刷新」冻结结果，NKB 一更新就改掉老记录，与「冻结只读」冲突，已去掉）。
     *
     * @param bo 入参（analysisId / reportId / sourceId / clinicalSignificance）
     */
    public void updateGermlineSignificance(InterpretationGermlineSignificanceBo bo) {
        Integer significance = bo.getClinicalSignificance();
        if (significance == null || significance < 1 || significance > 5) {
            throw new ServiceException("临床意义只能是 1~5：1致病/2可能致病/3未知临床意义/4可能良性/5良性");
        }
        Map<String, Object> report = contextBuilder.loadReport(bo.getReportId(), bo.getAnalysisId());
        Map<String, Object> row = interpretationMapper.selectGermlineVariant(bo.getSourceId(), bo.getAnalysisId());
        if (row == null) {
            throw new ServiceException("胚系位点不存在或不属于该分析批次：sourceId=" + bo.getSourceId());
        }
        InterpretationContextVo context = interpretationService.loadContext(bo.getReportId(), bo.getAnalysisId());
        PreviewContext pc = contextBuilder.build(bo.getAnalysisId(), bo.getReportId(), report, context);
        PreviewVariantVo item = new PreviewVariantVo();
        item.setSourceType("CR_ALL");
        item.setGene(asString(row.get("gene")));
        item.setVariant(asString(row.get("variant")));
        item.setOriVariant(asString(row.get("oriVariant")));
        item.setZygosity(asString(row.get("zygosity")));
        // 必须带上人工父级：它参与匹配键，漏了会把改靶后的确认写到旧键上（实测踩到）
        item.setParentMutationIds(ParentMutationIds.parse(row.get("parentMutationId")));
        item.setMutationType("G");
        int affected = interpretationMapper.updateGermlineSignificance(variantMatcher.matchKey(pc, item), significance);
        if (affected == 0) {
            throw new ServiceException("该位点还没有匹配历史，请先预览一次再确认临床意义");
        }
    }

    /**
     * 改靶（体细胞 / 胚系共用）：给位点人工指定一个或多个 NKB 父级节点，
     * 写入源位点表 {@code parent_mutation_id}（逗号分隔）→ 产生新的匹配键，下次预览走继承逻辑。
     * <p>
     * 空列表 = 取消改靶。保存前按 en7 口径校验「改靶后必须能出药物证据」，出不来直接拒绝。
     *
     * @param bo 入参（analysisId / reportId / sourceType / sourceId / parentMutationIds）
     */
    public void updateVariantTarget(InterpretationTargetBo bo) {
        Map<String, Object> row = interpretationMapper.selectVariantForTarget(bo.getSourceType(), bo.getSourceId(),
            bo.getAnalysisId());
        if (row == null) {
            throw new ServiceException("位点不存在或不属于该分析批次：" + bo.getSourceType() + "#" + bo.getSourceId());
        }
        List<Long> parentIds = ParentMutationIds.distinct(bo.getParentMutationIds());
        if (!parentIds.isEmpty()) {
            requireApprovedNodes(parentIds);
            requireDrugEvidence(bo, row, parentIds);
        }
        String parentIdsText = parentIds.isEmpty() ? null : ParentMutationIds.toKey(parentIds);
        int affected = interpretationMapper.updateParentMutation(bo.getSourceType(), bo.getSourceId(),
            bo.getAnalysisId(), parentIdsText);
        if (affected == 0) {
            throw new ServiceException("改靶失败：位点不存在或不属于该分析批次");
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
     * 校验父级都来自知识库（Approved 节点）。人工改靶必须挂到知识库里有的节点上。
     *
     * @param parentIds 父级节点ID（已去重）
     */
    private void requireApprovedNodes(List<Long> parentIds) {
        List<String> names = nkb(() -> nkbEvidenceMapper.selectVariantNames(parentIds));
        if (names == null || names.size() < parentIds.size()) {
            throw new ServiceException("有父级节点在知识库中不存在或未审核，请重新选择");
        }
    }

    /**
     * en7 口径（{@code GeneticMarkerVwServiceImpl.hasTargetDrugInfo}）：<b>改靶后必须能出药物证据</b>，
     * 出不来直接拒绝保存。
     *
     * @param bo        改靶入参
     * @param row       源位点行（gene / variant / oriVariant）
     * @param parentIds 人工父级节点ID
     */
    private void requireDrugEvidence(InterpretationTargetBo bo, Map<String, Object> row, List<Long> parentIds) {
        Map<String, Object> report = contextBuilder.loadReport(bo.getReportId(), bo.getAnalysisId());
        InterpretationContextVo context = interpretationService.loadContext(bo.getReportId(), bo.getAnalysisId());
        PreviewContext pc = contextBuilder.build(bo.getAnalysisId(), bo.getReportId(), report, context);
        boolean germline = "CR_ALL".equals(bo.getSourceType());
        NkbDrugMatcher.MatchResult matched = drugMatcher.match(asString(row.get("gene")), asString(row.get("variant")),
            asString(row.get("oriVariant")), germline, pc.getDiseaseScope(), parentIds);
        List<PreviewDrugVo> evidence = matched.getEvidence() == null ? List.of() : matched.getEvidence();
        if (evidence.isEmpty()) {
            throw new ServiceException("改靶后无药物信息，此操作无法保存");
        }
    }
}
