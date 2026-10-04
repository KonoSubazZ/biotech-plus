package org.dromara.report.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.satoken.utils.TenantContext;
import org.dromara.report.domain.vo.PreviewDrugVo;
import org.dromara.report.mapper.NkbEvidenceMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * NKB 药物匹配（对齐 report_en7 口径）
 * <p>
 * 与 en7 的对应关系（出处见 docs/context/report/report-match-rules.md）：
 * <ul>
 *   <li>节点解析四级兜底：精确 → fs 归一 → 融合 KDD → 截断类失活（{@code ReportCrServiceImpl.getMutationID}）</li>
 *   <li>节点集合 = 自身 + 一层父级（{@code getMutIdList}）</li>
 *   <li>癌种范围 = 本癌种 + 祖先 + 子孙，再按性别/瘤种剔除（{@code getDiseaseList} + {@code solidTumorFiltration}）</li>
 *   <li>证据过滤 = Approved + 关系{1,4,6} + 分期>11 + 类型=2 + 药物未作废 + 癌种已审核 + mutation_type(S/G)</li>
 *   <li>药物分层 = {@code getDrugLevel}（1-4 获益A-D、5-8 耐药A-D、9 其他）+ {@code getGive}（父级癌种有临床试验则升级）</li>
 *   <li>其他癌种获批药降格为 C 级（{@code fetchNkbDrugInfo} 692-712 行）</li>
 *   <li>去重按「药物 × 癌种」保留最高等级，但证据明细全量保留（{@code filterDrugList} + {@code buildDrugAuditList}）</li>
 * </ul>
 *
 * @author <你的名字>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NkbDrugMatcher {

    private final NkbEvidenceMapper nkbEvidenceMapper;

    private final NkbDiseaseScopeResolver diseaseScopeResolver;

    /** 单次匹配最多取多少条证据（避免 NKB 异常数据把响应撑爆） */
    private static final int MAX_EVIDENCE = 200;

    /** KDD（激酶结构域）基因与外显子区间，抄自 en7 VariantServiceImpl.isFusionKDDVariant */
    private static final List<String[]> KDD_RANGES = List.of(
        new String[]{"FGFR1", "10", "17"}, new String[]{"NTRK1", "13", "17"}, new String[]{"NTRK3", "14", "19"},
        new String[]{"FGFR2", "11", "18"}, new String[]{"NTRK2", "16", "21"}, new String[]{"TMPRSS2", "9", "13"},
        new String[]{"ALK", "20", "28"}, new String[]{"FGFR3", "11", "17"}, new String[]{"PDGFRA", "12", "21"},
        new String[]{"BRAF", "12", "18"}, new String[]{"FGFR4", "11", "17"}, new String[]{"PDGFRB", "12", "21"},
        new String[]{"EGFR", "18", "25"}, new String[]{"FLT3", "14", "23"}, new String[]{"RET", "12", "18"},
        new String[]{"ERBB2", "19", "25"}, new String[]{"KIT", "11", "20"}, new String[]{"ROS1", "36", "42"},
        new String[]{"ERBB4", "18", "24"}, new String[]{"MET", "16", "21"}, new String[]{"PIK3CA", "14", "21"});

    /**
     * 匹配主流程
     *
     * @param gene            基因
     * @param variant         位点短名（如 V559D）
     * @param oriVariant      原始位点描述
     * @param germline        是否胚系（决定 mutation_type：S / G）
     * @param scope           癌种范围（由 {@link NkbDiseaseScopeResolver} 解析）
     * @param manualParentIds 人工改靶指定的父级节点ID（可多个；en7 的 parent_mutID，未知/未收录的位点靠它出证据）
     * @return 匹配结果
     */
    public MatchResult match(String gene, String variant, String oriVariant, boolean germline,
                             NkbDiseaseScopeResolver.DiseaseScope scope, List<Long> manualParentIds) {
        Node node = resolveNode(gene, variant, oriVariant);
        List<Long> nodeIds = node == null ? new ArrayList<>() : nodesWithParents(node.mutationId());
        appendManualParents(nodeIds, manualParentIds);
        if (nodeIds.isEmpty()) {
            return MatchResult.notMatched(description(variant));
        }
        String mutationType = germline ? "G" : "S";
        List<PreviewDrugVo> evidence = new ArrayList<>(query(nkbEvidenceMapper.selectDrugAnnotations(
            nodeIds, scope.diseaseIds(), mutationType, MAX_EVIDENCE)));
        List<PreviewDrugVo> otherCancer = query(nkbEvidenceMapper.selectOtherApprovedDrugs(
            nodeIds, scope.diseaseIds(), scope.excludedIds(), mutationType, MAX_EVIDENCE));
        buildGrades(evidence, scope.parentDiseaseIds(), false);
        buildGrades(otherCancer, scope.parentDiseaseIds(), true);
        mergeOtherCancerDrugs(evidence, otherCancer);
        dedupeByDrugAndDisease(evidence);

        MatchResult result = new MatchResult();
        // 改了靶但位点本身知识库未收录：inNkb 仍为 false，证据来自人工指定的父级
        result.setInNkb(node != null);
        result.setMutationId(node == null ? null : node.mutationId());
        result.setMatchedNode(node == null ? null : node.nodeName());
        result.setEffectText(node == null ? null : node.effectText());
        result.setEvidence(evidence);
        result.setDrugGroups(groupDrugNames(evidence));
        result.setDrugAuditList(buildDrugAuditList(evidence));
        result.setVariationClass(variationClass(geneName(gene), evidence));
        result.setDescription(description(variant));
        return result;
    }

    /** 节点集合追加人工改靶父级（en7：本地保存的 parent_mutID 逐条去重追加） */
    private void appendManualParents(List<Long> nodeIds, List<Long> manualParentIds) {
        if (manualParentIds == null) {
            return;
        }
        for (Long id : manualParentIds) {
            if (id != null && !nodeIds.contains(id)) {
                nodeIds.add(id);
            }
        }
    }

    // ---------------------------------------------------------------- 节点解析

    /** 节点解析四级兜底（en7 getMutationID） */
    private Node resolveNode(String gene, String variant, String oriVariant) {
        Node node = null;
        if (StringUtils.hasText(gene) && StringUtils.hasText(variant)) {
            node = firstMatchingNode(gene, variant, oriVariant);
        }
        // 允许 null：知识库未收录该位点（调用方按「无证据」处理，不再向上兜底）
        return node;
    }

    /** 四级兜底的实际判定：精确 → fs 归一 → 融合 KDD → 截断类失活 */
    private Node firstMatchingNode(String gene, String variant, String oriVariant) {
        Node node = queryNode(gene, variant);
        if (node == null && variant.contains("fs")) {
            // fs 归一：p.R611Qfs*12 → R611fs
            String tmp = variant.split("fs")[0].replaceAll("[0-9]+$", "");
            node = queryNode(gene, tmp + "fs");
        }
        if (node == null && variant.contains("Fusion") && isFusionKdd(gene, oriVariant)) {
            node = queryNode(gene, "KDD Mutation");
        }
        boolean truncating = variant.contains("fs") || variant.contains("*")
            || variant.contains("+") || variant.contains("-");
        if (node == null && truncating && !variant.contains("Fusion")) {
            // 截断型兜底：借用该基因的失活节点；该基因没有失活节点时仍为 null
            node = queryNode(gene, "Inactive Mutation");
        }
        return node;
    }

    private Node queryNode(String gene, String variantName) {
        Map<String, Object> row = nkb(() -> nkbEvidenceMapper.selectVariantNode(gene, variantName));
        Node node = null;
        if (row != null) {
            node = new Node(PreviewSupport.asLong(row.get("mutationId")),
                PreviewSupport.asString(row.get("variantName")), PreviewSupport.asString(row.get("effectText")));
        }
        return node;
    }

    /** en7 isFusionKDDVariant：按「基因-基因 Fusion 首字母起:首字母止」比对 */
    private boolean isFusionKdd(String gene, String oriVariant) {
        if (!StringUtils.hasText(oriVariant)) {
            return false;
        }
        String initial = String.valueOf(gene.charAt(0));
        for (String[] range : KDD_RANGES) {
            if (!range[0].equals(gene)) {
                continue;
            }
            String forward = String.format("%s-%s Fusion %s%s:%s%s",
                range[0], range[0], initial, range[1], initial, range[2]);
            String backward = String.format("%s-%s Fusion %s%s:%s%s",
                range[0], range[0], initial, range[2], initial, range[1]);
            if (oriVariant.contains(forward) || oriVariant.contains(backward)) {
                return true;
            }
        }
        return false;
    }

    private List<Long> nodesWithParents(Long mutationId) {
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(mutationId);
        List<Long> parents = nkb(() -> nkbEvidenceMapper.selectParentMutationIds(mutationId));
        if (parents != null) {
            ids.addAll(parents);
        }
        return new ArrayList<>(ids);
    }

    // ---------------------------------------------------------------- 癌种范围

    // ---------------------------------------------------------------- 分级 / 去重

    /** 耐药关系字典ID（nkb.relationship：1 敏感性增加 / 4 有益的 / 6 抗药性） */
    private static final int RESISTANT_RELATION_ID = 6;

    /**
     * 是否耐药证据。
     * <p>
     * ⚠️ 只能按字典ID判：SQL 取的是 `relationship_chinese`（展示用中文），
     * 早先按 `"Resistant".equals(...)` 比中文，恒为 false → 抗药性证据被当获益药（已修）。
     *
     * @param drug 证据行
     * @return 是否抗药性
     */
    private boolean isResistant(PreviewDrugVo drug) {
        return drug.getRelationshipId() != null && drug.getRelationshipId() == RESISTANT_RELATION_ID;
    }

    /** 按 en7 getDrugLevel 计算等级码，并补 give / relation / levelName */
    private void buildGrades(List<PreviewDrugVo> list, List<Long> parentDiseaseIds, boolean otherCancer) {
        for (PreviewDrugVo drug : list) {
            boolean resistant = isResistant(drug);
            drug.setFromOtherCancer(otherCancer);
            drug.setRelation(resistant ? "RESISTANT" : "BENEFIT");
            if (otherCancer) {
                // en7：其他癌种的获批药一律降格 → 耐药 7 / 获益 3
                drug.setApproveRange(resistant ? 7 : 3);
            } else {
                drug.setApproveRange(drugLevel(drug, parentDiseaseIds));
            }
            drug.setLevelName(levelName(drug.getApproveRange()));
        }
    }

    /**
     * en7 getDrugLevel：1-4 获益 A-D，5-8 耐药 A-D，9 其他（不输出）
     *
     * @param drug             证据行
     * @param parentDiseaseIds 父级癌种ID（give 判定用）
     * @return 等级码
     */
    private int drugLevel(PreviewDrugVo drug, List<Long> parentDiseaseIds) {
        Integer phase = drug.getEvidencePhaseId();
        if (phase == null) {
            return 9;
        }
        if (isResistant(drug) && phase >= 12) {
            return resistantLevel(phase, () -> give(drug, parentDiseaseIds));
        }
        if (phase > 22) {
            return 1;
        }
        if (phase > 17 && phase < 23) {
            return 2;
        }
        if (phase > 15 && phase < 18) {
            return 3;
        }
        if (phase == 14) {
            return switch (give(drug, parentDiseaseIds)) {
                case "2" -> 3;
                case "1" -> 4;
                default -> 9;
            };
        }
        return phase > 11 && phase < 14 ? 4 : 9;
    }

    /** 耐药分支（en7 getDrugLevel 的 Resistant 段）：5-8，give=0 落 9 */
    private int resistantLevel(int phase, java.util.function.Supplier<String> giveSupplier) {
        if (phase > 22) {
            return 5;
        }
        if (phase > 17 && phase < 23) {
            return 6;
        }
        if (phase > 15 && phase < 18) {
            return 7;
        }
        if (phase == 14) {
            return switch (giveSupplier.get()) {
                case "2" -> 7;
                case "1" -> 8;
                default -> 9;
            };
        }
        return phase > 11 && phase < 14 ? 8 : 9;
    }

    /** en7 getGive：父级癌种有招募中试验 → 2；否则 has_previous_clinical_result 空或 Y → 1；否则 0 */
    private String give(PreviewDrugVo drug, List<Long> parentDiseaseIds) {
        if (parentDiseaseIds != null && !parentDiseaseIds.isEmpty() && drug.getAnnotationId() != null) {
            Long annotationId = drug.getAnnotationId();
            Integer trials = nkb(() -> nkbEvidenceMapper.selectRecruitingTrialCount(annotationId, parentDiseaseIds));
            if (trials != null && trials > 0) {
                return "2";
            }
        }
        // 按 en7 语义：has_previous_clinical_result 为空或 Y → 1（落 D 级），否则 0（不输出）
        return drug.getHasPreviousClinicalResult() == null || "Y".equals(drug.getHasPreviousClinicalResult())
            ? "1" : "0";
    }

    private String levelName(Integer approveRange) {
        String level = null;
        if (approveRange != null && approveRange >= 1 && approveRange <= 8) {
            level = String.valueOf((char) ('A' + (approveRange - 1) % 4));
        }
        return level;
    }

    /** en7：本癌种已有 A/B 级（1,2,5,6）的同一 drug_id，其他癌种不再重复输出 */
    private void mergeOtherCancerDrugs(List<PreviewDrugVo> evidence, List<PreviewDrugVo> otherCancer) {
        Set<Long> localTopDrugIds = new HashSet<>();
        for (PreviewDrugVo drug : evidence) {
            if (drug.getApproveRange() != null && Set.of(1, 2, 5, 6).contains(drug.getApproveRange())) {
                localTopDrugIds.add(drug.getDrugId());
            }
        }
        for (PreviewDrugVo drug : otherCancer) {
            if (!localTopDrugIds.contains(drug.getDrugId())) {
                evidence.add(drug);
            }
        }
    }

    /**
     * en7 filterDrugList：**获益 / 耐药各自一个桶**，同一（药物 × 癌种）在各自桶里只保留**最高等级**（等级码最小）那条；
     * 等级 9 丢弃。所以同一个药可以同时出现在获益组和耐药组（关系不同，属两条不同证据）。
     */
    private void dedupeByDrugAndDisease(List<PreviewDrugVo> evidence) {
        Map<String, Integer> benefitBest = new LinkedHashMap<>();
        Map<String, Integer> resistantBest = new LinkedHashMap<>();
        for (PreviewDrugVo drug : evidence) {
            if (drug.getApproveRange() == null || drug.getApproveRange() == 9) {
                continue;
            }
            Map<String, Integer> bucket = isResistant(drug) ? resistantBest : benefitBest;
            bucket.merge(drug.getDrugId() + "&" + drug.getDiseaseId(), drug.getApproveRange(), Math::min);
        }
        evidence.removeIf(drug -> drug.getApproveRange() == null || drug.getApproveRange() == 9
            || !drug.getApproveRange().equals(bestOf(drug, benefitBest, resistantBest)));
        evidence.sort(Comparator.comparing(PreviewDrugVo::getApproveRange));
    }

    /** 取该证据所属桶（获益/耐药）的最优等级，用于去重判定 */
    private Integer bestOf(PreviewDrugVo drug, Map<String, Integer> benefitBest, Map<String, Integer> resistantBest) {
        Map<String, Integer> bucket = isResistant(drug) ? resistantBest : benefitBest;
        return bucket.get(drug.getDrugId() + "&" + drug.getDiseaseId());
    }

    // ---------------------------------------------------------------- 输出结构

    /** en7 getDrugNameStr：按「药名+癌种」去重，其他检测要求的药名加 # */
    private Map<String, String> groupDrugNames(List<PreviewDrugVo> evidence) {
        Map<String, String> groups = new LinkedHashMap<>();
        groups.put("drugsA", names(evidence, 1));
        groups.put("drugsB", names(evidence, 2));
        groups.put("drugsC", names(evidence, 3));
        groups.put("drugsD", names(evidence, 4));
        groups.put("resistantDrugsA", names(evidence, 5));
        groups.put("resistantDrugsB", names(evidence, 6));
        groups.put("resistantDrugsC", names(evidence, 7));
        groups.put("resistantDrugsD", names(evidence, 8));
        return groups;
    }

    /** 生成某等级的药物名串：按「药名+癌种」去重，其他检测要求的加 #（en7 getDrugNameStr） */
    private String names(List<PreviewDrugVo> evidence, int range) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> out = new ArrayList<>();
        for (PreviewDrugVo drug : evidence) {
            if (drug.getApproveRange() == null || drug.getApproveRange() != range
                || !StringUtils.hasText(drug.getDrugName())) {
                continue;
            }
            if (!seen.add(drug.getDrugName() + drug.getDisease())) {
                continue;
            }
            out.add("1".equals(drug.getOtherTestRequired()) ? drug.getDrugName() + "#" : drug.getDrugName());
        }
        return out.isEmpty() ? null : String.join(";", out);
    }

    /** en7 buildDrugAuditList：{drug, relation, level, evidenceDiseaseId, evidenceDiseaseName} */
    private List<Map<String, Object>> buildDrugAuditList(List<PreviewDrugVo> evidence) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (PreviewDrugVo drug : evidence) {
            if (drug.getApproveRange() == null || drug.getApproveRange() < 1 || drug.getApproveRange() > 8) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("drug", drug.getDrugName());
            item.put("relation", drug.getApproveRange() <= 4 ? "BENEFIT" : "RESISTANT");
            item.put("level", levelName(drug.getApproveRange()));
            item.put("evidenceDiseaseId", drug.getDiseaseId());
            item.put("evidenceDiseaseName", drug.getDisease());
            item.put("matchedNode", drug.getNodeName());
            result.add(item);
        }
        return result;
    }

    /** en7：有 A/B 获益或耐药 A/B → I 类；有证据但只有 C/D → II 类；无证据 → III 类 */
    private String variationClass(String gene, List<PreviewDrugVo> evidence) {
        if ("多靶点循证".equals(gene) || "Complex".equals(gene)) {
            return "-";
        }
        boolean hasTop = evidence.stream().anyMatch(d -> d.getApproveRange() != null
            && Set.of(1, 2, 5, 6).contains(d.getApproveRange()));
        if (hasTop) {
            return "I类";
        }
        return evidence.isEmpty() ? "III类" : "II类";
    }

    /** en7 四段描述模板 */
    private String description(String variant) {
        if ("Amplification".equals(variant)) {
            return "该变异为基因扩增，可能导致蛋白表达增加。";
        }
        if ("Loss".equals(variant)) {
            return "该变异为基因缺失，可能导致蛋白表达增加。";
        }
        String value = variant == null ? "" : variant;
        boolean truncating = value.contains("fs") || value.contains("*") || value.contains("+") || value.contains("-");
        if (truncating && !value.contains("Fusion")) {
            return "该变异为失活突变，可能会导致蛋白功能缺失。";
        }
        return "该突变临床意义未明，若导致蛋白功能异常，可能影响下游信号通路，参与肿瘤发生发展。";
    }

    private String geneName(String gene) {
        return gene == null ? "" : gene;
    }

    // ---------------------------------------------------------------- 工具

    private <T> T nkb(Supplier<T> action) {
        return TenantContext.withoutTenant(action::get);
    }

    private <T> List<T> query(Supplier<List<T>> action) {
        List<T> result = nkb(action::get);
        return result == null ? List.of() : result;
    }

    private static List<PreviewDrugVo> query(List<PreviewDrugVo> rows) {
        return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
    }

    /** 命中的知识库节点 */
    private record Node(Long mutationId, String nodeName, String effectText) {
    }

    /**
     * 癌种范围
     *
     * @param diseaseIds       参与证据过滤的癌种（本癌种 + 祖先 + 子孙，已剔除性别/瘤种冲突）
     * @param parentDiseaseIds 本癌种 + 祖先（give 判定与「其他癌种」区分用）
     * @param excludedIds      被剔除的癌种（性别/实体瘤·血液瘤）
     */
    public record DiseaseScope(List<Long> diseaseIds, List<Long> parentDiseaseIds, List<Long> excludedIds) {
    }

    /** 匹配结果 */
    @lombok.Data
    public static class MatchResult {
        /** 知识库节点ID */
        private Long mutationId;
        /** 是否命中知识库 */
        private Boolean inNkb;
        /** 命中节点名（自身或父级） */
        private String matchedNode;
        /** effect 功能判定 */
        private String effectText;
        /** 证据明细（全量保留） */
        private List<PreviewDrugVo> evidence;
        /** 按等级分组的药物名串 */
        private Map<String, String> drugGroups;
        /** 审核列表 */
        private List<Map<String, Object>> drugAuditList;
        /** 位点分级 I类/II类/III类/- */
        private String variationClass;
        /** 描述文案 */
        private String description;

        static MatchResult notMatched(String description) {
            MatchResult result = new MatchResult();
            result.setInNkb(false);
            result.setEvidence(new ArrayList<>());
            result.setDrugGroups(new LinkedHashMap<>());
            result.setDrugAuditList(new ArrayList<>());
            result.setVariationClass("III类");
            result.setDescription(description);
            return result;
        }
    }
}
