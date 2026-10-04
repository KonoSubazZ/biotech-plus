package org.dromara.report.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.common.satoken.utils.TenantContext;
import org.dromara.report.domain.vo.PreviewVariantVo;
import org.dromara.report.mapper.NkbEvidenceMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 位点四类说明的装配（对齐 en7 的 getVarDrugNote 六段说明）：基因说明 / 信号通路说明 / 位点说明 / 突变说明
 * <ul>
 *   <li>基因说明：NKB {@code gene_description.gene_description_chinese}（Approved）</li>
 *   <li>信号通路说明：NKB {@code gene_description.pathway_description_chinese}（Approved，**与基因说明同一行**，en7 原文 {@code pathway_description.trim()}）</li>
 *   <li>位点说明：NKB {@code gene_variant_description}，按**命中的知识库节点**取（自身或父级）</li>
 *   <li>突变说明：{@link HgvsTranslator} 生成（对齐 en7 的 translate_hgvs.pl）；
 *       丰度传原始数值（不带 %，避免 en7 的双百分号）；胚系无丰度时不拼丰度句</li>
 * </ul>
 *
 * @author <你的名字>
 */
@Component
@RequiredArgsConstructor
public class VariantDescriptionEnricher {

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
