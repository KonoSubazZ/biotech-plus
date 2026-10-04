package org.dromara.report.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.common.satoken.utils.TenantContext;
import org.dromara.report.domain.vo.PreviewDrugVo;
import org.dromara.report.domain.vo.PreviewTrialVo;
import org.dromara.report.domain.vo.PreviewVariantVo;
import org.dromara.report.mapper.NkbEvidenceMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 位点说明装配（对齐 en7 的 getVarDrugNote 六段说明 + 证据/临床试验两类证据）：
 * <ul>
 *   <li>基因说明：NKB {@code gene_description.gene_description_chinese}（Approved）</li>
 *   <li>信号通路说明：NKB {@code gene_description.pathway_description_chinese}（Approved，**与基因说明同一行**，en7 原文 {@code pathway_description.trim()}）</li>
 *   <li>位点说明：NKB {@code gene_variant_description}，按**命中的知识库节点**取（自身或父级）</li>
 *   <li>突变说明：{@link HgvsTranslator} 生成（对齐 en7 的 translate_hgvs.pl）；丰度传原始数值（不带 %，避免 en7 的双百分号）；胚系无丰度时不拼丰度句</li>
 *   <li>证据说明：按分期分流（获批上市→批准说明、指南推荐→NCCN/CSCO 指南说明、其余→annotation_chinese；耐药不参与）</li>
 *   <li>临床试验：第二类证据（ID/名称/肿瘤类型/阶段/药物/地点），对齐 en7 {@code getClinicalTrial}</li>
 * </ul>
 * 后两类**不进 match_result 冻结**，每次预览重算（历史复用与首次匹配产出一致）。
 *
 * @author <你的名字>
 */
@Component
@RequiredArgsConstructor
public class VariantDescriptionEnricher {

    /** 分期：指南推荐 */
    private static final int PHASE_GUIDELINE = 23;

    /** 分期：获批上市 */
    private static final int PHASE_APPROVED = 24;

    private final NkbEvidenceMapper nkbEvidenceMapper;

    /**
     * 给位点行补四类说明
     *
     * @param item    位点行
     * @param freqRaw 原始丰度（胚系传 null，不拼丰度句）
     */
    public void enrich(PreviewVariantVo item, Object freqRaw) {
        Map<String, Object> gene = nkb(() -> nkbEvidenceMapper.selectGeneDescription(item.getGene()));
        item.setGeneDescription(gene == null ? null : PreviewSupport.asString(gene.get("geneDescription")));
        // en7 是 pathway_description.trim() 后写入；空白串按「没有」处理，避免前端渲染出空说明块
        String pathway = gene == null ? null : PreviewSupport.asString(gene.get("pathwayDescription"));
        item.setPathwayDescription(StringUtils.hasText(pathway) ? pathway.trim() : null);
        Long nodeId = resolveNodeId(item);
        if (nodeId != null) {
            Map<String, Object> node = nkb(() -> nkbEvidenceMapper.selectVariantDescription(nodeId));
            item.setVariantDescription(node == null ? null : PreviewSupport.asString(node.get("variantDescription")));
        }
        item.setRelatedMutations(relatedMutations(nodeId, item.getParentMutationIds()));
        item.setParentMutationNames(parentMutationNames(item.getParentMutationIds()));
        String freq = "CR_ALL".equals(item.getSourceType()) ? null : PreviewSupport.translationFreq(freqRaw);
        item.setMutationExplanation(HgvsTranslator.translate(item.getGene(), item.getOriVariant(), freq));
        enrichEvidence(item.getDrugMatch());
        item.setTrials(loadTrials(item.getDrugMatch()));
    }

    /**
     * 证据说明按分期分流（对齐 en7 {@code getVarDrugNote}，注意 en7 是 if/else if、**耐药分支在最前**）：
     * <ul>
     *   <li>耐药（approveRange ≥ 5）→ 保持 {@code annotation_chinese}（en7 走「耐药说明」段，**不取指南说明**）</li>
     *   <li>获益 + phase 23 指南推荐 → 指南说明（底表 {@code guideline_drug} + {@code guideline}，按 NCCN/CSCO 拼装，**不用 annotation**）</li>
     *   <li>获益 + phase 24 获批上市 → 批准说明（底表 {@code approved_drug.approval_description_chinese}）+ 获批机构</li>
     *   <li>其余获益 → {@code annotation_chinese}</li>
     * </ul>
     * 这些字段**不进 match_result 冻结**，每次预览重算，保证历史复用与首次匹配一致。
     *
     * @param evidence 证据行；无证据时直接返回
     */
    private void enrichEvidence(List<PreviewDrugVo> evidence) {
        List<Long> drugIds = distinctIds(evidence, PreviewDrugVo::getDrugId);
        List<Long> diseaseIds = distinctIds(evidence, PreviewDrugVo::getDiseaseId);
        if (drugIds.isEmpty() || diseaseIds.isEmpty()) {
            return;
        }
        Map<String, Map<String, Object>> approvals = indexRows(
            nkb(() -> nkbEvidenceMapper.selectApprovalDescriptions(drugIds, diseaseIds)), "drugId", "diseaseId");
        EvidenceDictionary dic = new EvidenceDictionary(approvals, guidelineIndex(drugIds, diseaseIds));
        for (PreviewDrugVo drug : evidence) {
            fillEvidenceRow(drug, dic);
        }
    }

    /**
     * 单条证据的说明分流：**耐药不参与**（en7 的耐药分支最先命中），获益按 23/24 取指南/批准说明
     *
     * @param drug 证据行（写回 guidelineDescription / approvalDescription / approvingAgency）
     * @param dic  本次证据集合的说明字典（批准 / 指南）
     */
    private void fillEvidenceRow(PreviewDrugVo drug, EvidenceDictionary dic) {
        boolean resistant = drug.getApproveRange() != null && drug.getApproveRange() >= 5;
        Integer phase = drug.getEvidencePhaseId();
        if (resistant || phase == null) {
            return;
        }
        String key = key(drug.getDrugId(), drug.getDiseaseId());
        if (phase == PHASE_GUIDELINE) {
            fillGuideline(drug, dic.guidelines().get(key));
            return;
        }
        if (phase == PHASE_APPROVED) {
            fillApproval(drug, dic.approvals().get(key));
        }
    }

    /** 说明字典：批准说明（按 药&癌种 索引）+ 指南行（按 药&癌种 索引） */
    private record EvidenceDictionary(Map<String, Map<String, Object>> approvals,
                                      Map<String, List<Map<String, Object>>> guidelines) {
    }

    /** 获批上市说明：approval_description_chinese + 获批机构（机构已有值时不覆盖） */
    private void fillApproval(PreviewDrugVo drug, Map<String, Object> approval) {
        if (approval == null) {
            return;
        }
        drug.setApprovalDescription(PreviewSupport.asString(approval.get("approvalDescription")));
        if (!StringUtils.hasText(drug.getApprovingAgency())) {
            drug.setApprovingAgency(PreviewSupport.asString(approval.get("approvingAgency")));
        }
    }

    /** 指南行按 (药,癌种) 建索引 */
    private Map<String, List<Map<String, Object>>> guidelineIndex(List<Long> drugIds, List<Long> diseaseIds) {
        Map<String, List<Map<String, Object>>> index = new HashMap<>();
        List<Map<String, Object>> rows = nkb(() -> nkbEvidenceMapper.selectGuidelineDescriptions(drugIds, diseaseIds));
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                index.computeIfAbsent(key(row.get("drugId"), row.get("diseaseId")), k -> new ArrayList<>()).add(row);
            }
        }
        return index;
    }

    /** 取某列去重后的非空值（保持出现顺序） */
    private List<Long> distinctIds(List<PreviewDrugVo> evidence, Function<PreviewDrugVo, Long> getter) {
        List<Long> values = new ArrayList<>();
        if (evidence == null) {
            return values;
        }
        for (PreviewDrugVo drug : evidence) {
            Long value = getter.apply(drug);
            if (value != null && !values.contains(value)) {
                values.add(value);
            }
        }
        return values;
    }

    /**
     * 指南推荐说明：en7 {@code ReportCrServiceImpl:1183-1206} 的串法
     * ——「NCCN指南推荐 药名(癌种) + 指南描述」/「CSCO指南推荐 …」，多份用换行分隔。
     *
     * @param drug  证据行（写回 guidelineDescription / guidelineTypes）
     * @param rows  该 (药,癌种) 的指南行；没有时不写
     */
    private void fillGuideline(PreviewDrugVo drug, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        String drugAndDisease = (drug.getDrugName() == null ? "" : drug.getDrugName())
            + "(" + (drug.getDisease() == null ? "" : drug.getDisease()) + ")";
        StringBuilder text = new StringBuilder();
        List<String> types = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String type = PreviewSupport.asString(row.get("guidelineType"));
            String description = PreviewSupport.asString(row.get("guidelineDescription"));
            if (!StringUtils.hasText(description)) {
                continue;
            }
            if (type != null && !types.contains(type)) {
                types.add(type);
            }
            text.append(type).append("指南推荐").append(drugAndDisease).append(description.trim()).append("\n");
        }
        if (text.length() > 0) {
            drug.setGuidelineDescription(text.toString().trim());
            drug.setGuidelineTypes(types);
        }
    }

    /**
     * 临床试验证据（第二类证据）：对齐 en7 {@code AnalysisReportDao.getClinicalTrial}
     * ——只取招募中/邀请入组、试验已审核，并剔除 {@code clinical_trial_exclude_disease} 里本癌种排除的试验；
     * 按「药物 + 试验号」去重（en7 cliSet），阶段从高到低（en7 order_num desc）。
     *
     * @param evidence 证据行（用它们的 annotationId 反查试验）
     * @return 临床试验列表；没有时为空列表
     */
    private List<PreviewTrialVo> loadTrials(List<PreviewDrugVo> evidence) {
        List<Long> annotationIds = distinctIds(evidence, PreviewDrugVo::getAnnotationId);
        if (annotationIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> diseaseIds = distinctIds(evidence, PreviewDrugVo::getDiseaseId);
        return toTrials(nkb(() -> nkbEvidenceMapper.selectClinicalTrials(annotationIds, diseaseIds)));
    }

    /** 试验行 → VO；按「药物 + 试验号」去重（en7 的 cliSet，顺序沿用 SQL 的阶段倒序） */
    private List<PreviewTrialVo> toTrials(List<Map<String, Object>> rows) {
        List<PreviewTrialVo> trials = new ArrayList<>();
        if (rows == null) {
            return trials;
        }
        List<String> seen = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String trialId = PreviewSupport.asString(row.get("trialId"));
            String drugName = PreviewSupport.asString(row.get("drugName"));
            if (seen.contains(drugName + " " + trialId)) {
                continue;
            }
            seen.add(drugName + " " + trialId);
            trials.add(toTrial(row, drugName, trialId));
        }
        return trials;
    }

    private PreviewTrialVo toTrial(Map<String, Object> row, String drugName, String trialId) {
        PreviewTrialVo trial = new PreviewTrialVo();
        trial.setTrialId(trialId);
        trial.setTitle(PreviewSupport.asString(row.get("title")));
        trial.setTrialCondition(PreviewSupport.asString(row.get("trialCondition")));
        trial.setPhase(PreviewSupport.asString(row.get("phase")));
        trial.setPhaseText(phaseText(trial.getPhase()));
        trial.setLocation(PreviewSupport.asString(row.get("location")));
        trial.setDrugName(drugName);
        trial.setAnnotationId(PreviewSupport.asLong(row.get("annotationId")));
        return trial;
    }

    /** 阶段中文（en7 previewReportList.jsp 的 translatePhase） */
    private String phaseText(String phase) {
        String unknown = "未知";
        if (phase == null) {
            return unknown;
        }
        return switch (phase) {
            case "Phase IV" -> "IV期";
            case "Phase III" -> "III期";
            case "Phase II/III" -> "II/III期";
            case "Phase II" -> "II期";
            case "Phase I/II" -> "I/II期";
            case "Phase I" -> "I期";
            default -> unknown;
        };
    }

    private Map<String, Map<String, Object>> indexRows(List<Map<String, Object>> rows, String keyName, String secondName) {
        Map<String, Map<String, Object>> indexed = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                indexed.putIfAbsent(key(row.get(keyName), row.get(secondName)), row);
            }
        }
        return indexed;
    }

    private String key(Object drugId, Object diseaseId) {
        return drugId + "&" + diseaseId;
    }

    /**
     * 关联突变节点名：命中节点自身 + 一层父级 + 人工改靶父级（en7 的 mutationIdList 口径）。
     * <p>
     * 只依赖冻结的 matchedMutationId 与父级ID，所以历史复用与首次匹配产出完全一致
     * （不进 match_result，避免改动冻结结构）。改靶后的父级要出现在这里，否则界面看不出改靶生效。
     *
     * @param nodeId    命中的节点ID；未收录时为 null
     * @param parentIds 人工改靶父级节点ID；未改靶时为空
     * @return 节点名列表（都没有时为空）
     */
    private List<String> relatedMutations(Long nodeId, List<Long> parentIds) {
        List<Long> ids = new ArrayList<>();
        if (nodeId != null) {
            ids.add(nodeId);
            List<Long> parents = nkb(() -> nkbEvidenceMapper.selectParentMutationIds(nodeId));
            if (parents != null) {
                ids.addAll(parents);
            }
        }
        if (parentIds != null) {
            for (Long parentId : parentIds) {
                if (parentId != null && !ids.contains(parentId)) {
                    ids.add(parentId);
                }
            }
        }
        List<String> names = new ArrayList<>();
        if (!ids.isEmpty()) {
            List<String> hit = nkb(() -> nkbEvidenceMapper.selectVariantNames(ids));
            if (hit != null) {
                names.addAll(hit);
            }
        }
        return names;
    }

    /**
     * 人工改靶父级的节点名（**与传入ID同序**，取不到时该位为 null）：界面回显用。
     *
     * @param parentIds 人工父级节点ID；未改靶时为空
     * @return 节点名列表；未改靶时为空列表
     */
    private List<String> parentMutationNames(List<Long> parentIds) {
        List<String> names = new ArrayList<>();
        if (parentIds == null || parentIds.isEmpty()) {
            return names;
        }
        Map<Long, String> namesById = new HashMap<>();
        List<Map<String, Object>> rows = nkb(() -> nkbEvidenceMapper.selectVariantNamesByIds(parentIds));
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                namesById.put(PreviewSupport.asLong(row.get("mutationId")),
                    PreviewSupport.asString(row.get("variantName")));
            }
        }
        for (Long parentId : parentIds) {
            names.add(namesById.get(parentId));
        }
        return names;
    }

    /**
     * 命中节点ID：优先用匹配结果里的；历史冻结记录里没有时，按命中的节点名回查一次
     *
     * @param item 位点行
     * @return 节点ID；知识库未收录时返回 null
     */
    private Long resolveNodeId(PreviewVariantVo item) {
        if (item.getMatchedMutationId() != null) {
            return item.getMatchedMutationId();
        }
        Long miss = null;
        boolean notMatched = !Boolean.TRUE.equals(item.getInNkb()) || item.getMatchedNode() == null;
        if (notMatched) {
            // 允许 null：知识库未收录该位点时没有节点说明
            return miss;
        }
        String gene = item.getGene();
        String nodeName = item.getMatchedNode();
        Map<String, Object> node = nkb(() -> nkbEvidenceMapper.selectVariantNode(gene, nodeName));
        return node == null ? miss : PreviewSupport.asLong(node.get("mutationId"));
    }

    private <T> T nkb(java.util.function.Supplier<T> action) {
        return TenantContext.withoutTenant(action::get);
    }
}
