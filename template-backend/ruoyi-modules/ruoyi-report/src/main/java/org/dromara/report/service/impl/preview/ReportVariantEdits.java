package org.dromara.report.service.impl.preview;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.report.domain.vo.PreviewVariantVo;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 报告位点人工修改（报告粒度，存 {@code analysis_report.report_variant_edits}）。
 * <p>
 * 口径（2026-11-05）：
 * <ul>
 *   <li>只有未入库位点可改（history_* 里没有该 match_key）；来源=历史整行只读</li>
 *   <li>三个可改字段：父靶点 parentMutationIds、胚系临床意义 clinicalSignificance、突变说明 mutationDescription</li>
 *   <li>键 = {@code gene_variant_sourceType:sourceId}（同报告内唯一；不同源表 id 会撞，故带 sourceType）</li>
 *   <li>只存 diff；整列为 NULL = 无人工修改</li>
 *   <li>预览/生成读取并套用；审核通过从「报告 JSON」冻结，不再读本表</li>
 * </ul>
 *
 * @author <你的名字>
 */
@Slf4j
public final class ReportVariantEdits {

    /** 只用于本 JSON 列，与全局 Jackson 配置无关 */
    private static final ObjectMapper JSON = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private static final int MIN_SIGNIFICANCE = 1;

    private static final int MAX_SIGNIFICANCE = 5;

    private ReportVariantEdits() {
    }

    /** 单个位点的人工修改；三个字段都可为空（空 = 不覆盖） */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Edit {

        /** 父靶点节点ID；空数组 = 取消改靶（有意义的值，不当作“未设置”） */
        private List<Long> parentMutationIds;

        /** 胚系临床意义 1~5 */
        private Integer clinicalSignificance;

        /** 突变说明（整段文本；空串 = 恢复默认，落库时存 null） */
        private String mutationDescription;
    }

    /**
     * 编辑键：基因_变异_来源类型:位点ID。
     *
     * @param gene       基因
     * @param variant    变异（规范短名）
     * @param sourceType 源位点表（CR_ALL / CNV / Fusion / SNP_INDEL…）
     * @param sourceId   源位点主键
     * @return 键，如 {@code BRCA1_A2825E_CR_ALL:24}
     */
    public static String key(String gene, String variant, String sourceType, Long sourceId) {
        return nz(gene).trim() + "_" + nz(variant).trim() + "_"
            + nz(sourceType).trim() + ":" + (sourceId == null ? "" : sourceId);
    }

    /** 按位点算编辑键 */
    public static String key(PreviewVariantVo item) {
        return item == null ? "" : key(item.getGene(), item.getVariant(), item.getSourceType(), item.getSourceId());
    }

    /**
     * 解析编辑表；空值/脏 JSON 一律按“无修改”处理（不阻断预览）。
     *
     * @param json 列原值
     * @return 键 → 修改（可修改的新 Map）
     */
    public static Map<String, Edit> parse(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        Map<String, Edit> parsed;
        try {
            parsed = JSON.readValue(json, new TypeReference<Map<String, Edit>>() {});
        } catch (Exception e) {
            log.warn("报告位点修改 JSON 解析失败，按无修改处理：{}", json, e);
            return new LinkedHashMap<>();
        }
        Map<String, Edit> result = new LinkedHashMap<>();
        if (parsed == null) {
            return result;
        }
        for (Map.Entry<String, Edit> entry : parsed.entrySet()) {
            Edit edit = sanitize(entry.getValue());
            if (edit != null) {
                result.put(entry.getKey(), edit);
            }
        }
        return result;
    }

    /**
     * 合并写回：patch 里为 null 的字段保持原值；空说明 = 删除说明；整条为空则删键。
     *
     * @param currentJson 当前列值（可空）
     * @param key         编辑键
     * @param patch       本次修改
     * @return 新 JSON；整表为空返回 null（调用方写 NULL）
     */
    public static String merge(String currentJson, String key, Edit patch) {
        Map<String, Edit> edits = parse(currentJson);
        Edit current = edits.get(key);
        Edit merged = new Edit(
            patch != null && patch.getParentMutationIds() != null
                ? distinct(patch.getParentMutationIds()) : (current == null ? null : current.getParentMutationIds()),
            patch != null && patch.getClinicalSignificance() != null
                ? patch.getClinicalSignificance() : (current == null ? null : current.getClinicalSignificance()),
            patch != null && patch.getMutationDescription() != null
                ? blankToNull(patch.getMutationDescription()) : (current == null ? null : current.getMutationDescription())
        );
        if (isEmpty(merged)) {
            edits.remove(key);
        } else {
            edits.put(key, sanitize(merged));
        }
        if (edits.isEmpty()) {
            return null;
        }
        try {
            return JSON.writeValueAsString(edits);
        } catch (Exception e) {
            throw new IllegalArgumentException("报告位点修改序列化失败", e);
        }
    }

    /**
     * 取某个位点的人工修改。
     *
     * @param edits 编辑表
     * @param item  位点
     * @return 修改；没有返回 null
     */
    public static Edit find(Map<String, Edit> edits, PreviewVariantVo item) {
        return edits == null || item == null ? null : edits.get(key(item));
    }

    /**
     * 把人工突变说明写到 items（来源=历史的行跳过，保持冻结内容）。
     * 未覆盖的位点走默认说明（{@link VariantNotes#compose}）。
     *
     * @param items 位点列表
     * @param edits 编辑表
     */
    public static void applyDescription(List<PreviewVariantVo> items, Map<String, Edit> edits) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (PreviewVariantVo item : items) {
            Edit edit = Boolean.TRUE.equals(item.getFromHistory()) ? null : find(edits, item);
            String manual = edit == null ? null : edit.getMutationDescription();
            if (StringUtils.hasText(manual)) {
                item.setMutationDescription(manual.trim());
                item.setMutationDescriptionEdited(true);
            } else {
                item.setMutationDescription(VariantNotes.compose(item));
                item.setMutationDescriptionEdited(false);
            }
        }
    }

    private static Edit sanitize(Edit edit) {
        if (edit == null) {
            return null;
        }
        List<Long> parents = edit.getParentMutationIds() == null ? null : distinct(edit.getParentMutationIds());
        Integer significance = edit.getClinicalSignificance();
        if (significance != null && (significance < MIN_SIGNIFICANCE || significance > MAX_SIGNIFICANCE)) {
            significance = null;
        }
        String description = blankToNull(edit.getMutationDescription());
        Edit sanitized = new Edit(parents, significance, description);
        return isEmpty(sanitized) ? null : sanitized;
    }

    private static boolean isEmpty(Edit edit) {
        return edit == null || (edit.getParentMutationIds() == null
            && edit.getClinicalSignificance() == null && edit.getMutationDescription() == null);
    }

    private static List<Long> distinct(List<Long> ids) {
        if (ids == null) {
            return null;
        }
        List<Long> result = new ArrayList<>();
        for (Long id : ids) {
            if (id != null && !result.contains(id)) {
                result.add(id);
            }
        }
        return result;
    }

    private static String blankToNull(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
