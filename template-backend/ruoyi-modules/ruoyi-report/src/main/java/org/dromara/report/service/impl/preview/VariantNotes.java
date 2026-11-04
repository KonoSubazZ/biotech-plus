package org.dromara.report.service.impl.preview;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.domain.vo.PreviewVariantVo;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 位点「突变说明」的人工覆盖（报告粒度，存 {@code analysis_report.report_variant_notes}）。
 * <p>
 * 为什么存在：预览 JSON 不落库（report-module-design §0.4），而正式生成会重新查库组装，
 * 所以「人工改的说明」必须落在输入侧，由预览与生成共用的组装入口读取。
 * <p>
 * 存储形态：{@code {"CR_ALL:44": "人工说明"}}，键 = 来源类型:位点ID；只存改过的位点，
 * 没改的不写（整列为 NULL）。空文本 = 恢复默认（删键）。
 * <p>
 * 与 {@link VariantDescriptionEnricher} 的分工：那边算**默认说明**（每次实时查 NKB，不冻结），
 * 这边只管**人工覆盖**与最终取值。默认口径对齐 en7 {@code ReportCrServiceImpl}：
 * {@code mutDesc + variantDescription} 直接拼接，NKB 位点说明缺失时用四段兜底 {@code description}。
 *
 * @author <你的名字>
 */
@Slf4j
public final class VariantNotes {

    /** 只用于覆盖表的序列化，与 ruoyi 全局 Jackson 配置无关 */
    private static final ObjectMapper JSON = new ObjectMapper();

    private VariantNotes() {
    }

    /**
     * 覆盖表的键：来源类型:位点ID（如 {@code CR_ALL:44}）。
     *
     * @param sourceType 源位点表（SNP_INDEL / CNV / FUSION / CR_ALL）
     * @param sourceId   源位点表主键
     * @return 键
     */
    public static String key(String sourceType, Long sourceId) {
        return (sourceType == null ? "" : sourceType.trim()) + ":" + (sourceId == null ? "" : sourceId);
    }

    /**
     * 解析覆盖表；空值/脏 JSON 一律按「全部默认」处理（不阻断预览）。
     *
     * @param json {@code report_variant_notes} 列原值
     * @return 键 → 人工说明（可修改的新 Map）
     */
    public static Map<String, String> parse(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, String> map = JSON.readValue(json, new TypeReference<Map<String, String>>() {});
            return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
        } catch (Exception e) {
            log.warn("报告位点说明 JSON 解析失败，按全部默认处理：{}", json, e);
            return new LinkedHashMap<>();
        }
    }

    /**
     * 把「人工覆盖 / 默认说明」写到每个位点：
     * 有覆盖用覆盖并打 {@code mutationDescriptionEdited=true}，否则用 {@link #compose} 算默认。
     *
     * @param items 位点列表（体细胞或胚系）
     * @param notes 覆盖表
     */
    public static void apply(List<PreviewVariantVo> items, Map<String, String> notes) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<String, String> safeNotes = notes == null ? Map.of() : notes;
        for (PreviewVariantVo item : items) {
            String manual = safeNotes.get(key(item.getSourceType(), item.getSourceId()));
            if (StringUtils.hasText(manual)) {
                item.setMutationDescription(manual.trim());
                item.setMutationDescriptionEdited(true);
            } else {
                item.setMutationDescription(compose(item));
                item.setMutationDescriptionEdited(false);
            }
        }
    }

    /**
     * 默认说明（en7 口径）：HGVS 中文译文 + 位点说明，直接拼接；
     * NKB 位点说明缺失时用四段兜底 {@code description}。
     *
     * @param item 位点（需已由 {@link VariantDescriptionEnricher} 填好三类说明）
     * @return 默认说明；都没有时 null
     */
    public static String compose(PreviewVariantVo item) {
        String hgvs = item.getMutationExplanation() == null ? "" : item.getMutationExplanation();
        String locus = StringUtils.hasText(item.getVariantDescription())
            ? item.getVariantDescription()
            : (item.getDescription() == null ? "" : item.getDescription());
        String text = hgvs + locus;
        return StringUtils.hasText(text) ? text : null;
    }

    /**
     * 合并写回：写文本 = 覆盖；空白文本 = 删除该位点覆盖（恢复默认）。
     *
     * @param currentJson 当前列值（可空）
     * @param sourceType  源位点表
     * @param sourceId    源位点主键
     * @param text        人工说明；空白 = 恢复默认
     * @return 新的覆盖表 JSON；整表为空时返回 null（调用方写 NULL）
     */
    public static String merge(String currentJson, String sourceType, Long sourceId, String text) {
        Map<String, String> notes = parse(currentJson);
        if (StringUtils.hasText(text)) {
            notes.put(key(sourceType, sourceId), text.trim());
        } else {
            notes.remove(key(sourceType, sourceId));
        }
        if (notes.isEmpty()) {
            return null;
        }
        try {
            return JSON.writeValueAsString(notes);
        } catch (Exception e) {
            throw new ServiceException("位点说明序列化失败：" + e.getMessage());
        }
    }

    /**
     * 制品里的说明是否与当前覆盖表一致（提交审核前的兜底校验）。
     * <p>
     * 报告生成后再改说明、却没重新生成时，送审的 DOCX 里还是旧文字 —— 用这条拦住：
     * 覆盖表里的每个键都要在制品里找到「同样文本且 edited=true」的位点；制品里 edited=true 的位点
     * 也必须在覆盖表里。制品里找不到的位点跳过（可能已不再报出），不误伤。
     *
     * @param items 制品里的位点（体细胞 + 胚系合并传入）
     * @param notes 当前覆盖表（空 Map = 没有人工说明）
     * @return true = 一致，可提交
     */
    public static boolean matchesArtifact(List<PreviewVariantVo> items, Map<String, String> notes) {
        Map<String, String> edited = new LinkedHashMap<>();
        if (items != null) {
            for (PreviewVariantVo item : items) {
                if (Boolean.TRUE.equals(item.getMutationDescriptionEdited())) {
                    edited.put(key(item.getSourceType(), item.getSourceId()),
                        item.getMutationDescription() == null ? "" : item.getMutationDescription().trim());
                }
            }
        }
        Map<String, String> safeNotes = notes == null ? Map.of() : notes;
        for (Map.Entry<String, String> entry : safeNotes.entrySet()) {
            if (!StringUtils.hasText(entry.getValue())) {
                continue;
            }
            if (edited.containsKey(entry.getKey()) && !edited.get(entry.getKey()).equals(entry.getValue().trim())) {
                return false;
            }
            if (!edited.containsKey(entry.getKey()) && hasItem(items, entry.getKey())) {
                // 位点还在制品里，但制品没标记人工修改 → 说明是生成后才改的
                return false;
            }
        }
        for (Map.Entry<String, String> entry : edited.entrySet()) {
            String note = safeNotes.get(entry.getKey());
            if (!StringUtils.hasText(note) || !note.trim().equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /** 制品里是否存在该键的位点（用于区分「已不报出」与「生成后才改」） */
    private static boolean hasItem(List<PreviewVariantVo> items, String noteKey) {
        if (items == null) {
            return false;
        }
        for (PreviewVariantVo item : items) {
            if (noteKey.equals(key(item.getSourceType(), item.getSourceId()))) {
                return true;
            }
        }
        return false;
    }
}
