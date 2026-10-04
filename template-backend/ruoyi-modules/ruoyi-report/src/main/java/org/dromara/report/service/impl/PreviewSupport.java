package org.dromara.report.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.TenantContext;
import org.dromara.report.domain.vo.PreviewDrugVo;
import org.dromara.report.domain.vo.PreviewVariantVo;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 报告预览的公共工具（从 {@link InterpretationPreviewServiceImpl} 拆出来，避免单文件过长）
 * <p>
 * 都是无状态静态方法：JSON 序列化、匹配键 SHA-256、字段取值/规范化、跨库 NKB 查询包装。
 * <p>
 * 注意 {@link #nkb(Supplier)}：nkb 表没有 tenant_id，必须绕过租户拦截。
 *
 * @author <你的名字>
 */
@Slf4j
final class PreviewSupport {

    private PreviewSupport() {
    }

    /** 单个文件内容序列化用（与 ruoyi 全局 Jackson 配置无关，只用于历史 match_result） */
    private static final ObjectMapper JSON = new ObjectMapper();

    static List<Map<String, Object>> filterByProductGenes(List<PreviewVariantVo> items, List<String> productGenes) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (PreviewVariantVo item : items) {
            if (!productGenes.contains(item.getGene())) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("gene", item.getGene());
            row.put("mutationType", item.getMutationType());
            row.put("result", item.getVariant());
            row.put("zygosity", item.getZygosity());
            row.put("classification", item.getClinicalSignificanceLabel());
            out.add(row);
        }
        return out;
    }

    static boolean pipelineHas(String moduleCode, String module) {
        if (!StringUtils.hasText(moduleCode)) {
            return false;
        }
        for (String code : moduleCode.split(";")) {
            if (code.trim().toUpperCase(Locale.ROOT).equals(module)) {
                return true;
            }
        }
        return false;
    }

    static String mutationTypeCode(String sourceType, String mutationTypeRaw) {
        if ("CNV".equals(sourceType)) {
            return "CNV";
        }
        if ("FUSION".equals(sourceType)) {
            return "FUSION";
        }
        String raw = nz(mutationTypeRaw).toLowerCase(Locale.ROOT);
        if (raw.contains("ins")) {
            return "I";
        }
        if (raw.contains("del")) {
            return "D";
        }
        return "S";
    }

    static String normalizeGender(String gender) {
        if (gender == null) {
            return "UNKNOWN";
        }
        return switch (gender.trim()) {
            case "男", "M", "MALE", "male" -> "MALE";
            case "女", "F", "FEMALE", "female" -> "FEMALE";
            default -> "UNKNOWN";
        };
    }

    // ---------------------------------------------------------------- 工具

    /** NKB 跨库只读查询：nkb 表没有 tenant_id，必须绕过租户拦截 */
    static <T> T nkb(java.util.function.Supplier<T> action) {
        return TenantContext.withoutTenant(action::get);
    }

    static String toJson(Object value) {
        try {
            return JSON.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception e) {
            log.warn("预览匹配结果序列化失败", e);
            return "[]";
        }
    }

    /**
     * 反序列化冻结的匹配结果（en7 口径下含 evidence / drugGroups / drugAuditList 等）。
     * <p>
     * 返回 {@code null} 表示这条冻结结果**不是当前结构**（例如旧规则时代写下的纯数组），
     * 调用方应按最新规则重算并覆盖同一条历史，避免旧规则结果长期生效。
     */
    @SuppressWarnings("unchecked")
    static NkbDrugMatcher.MatchResult parseMatchResult(String json) {
        NkbDrugMatcher.MatchResult result = null;
        if (!StringUtils.hasText(json)) {
            return result;
        }
        try {
            Map<String, Object> frozen = JSON.readValue(json, new TypeReference<Map<String, Object>>() {});
            if (!frozen.containsKey("inNkb")) {
                // 旧结构（纯数组）：交给调用方按最新规则重算并覆盖
                log.warn("冻结的匹配结果是旧结构，需要按最新规则重算：{}", json.substring(0, Math.min(json.length(), 80)));
                return result;
            }
            result = new NkbDrugMatcher.MatchResult();
            result.setEvidence(new ArrayList<>());
            result.setDrugGroups(new LinkedHashMap<>());
            result.setDrugAuditList(new ArrayList<>());
            result.setInNkb((Boolean) frozen.get("inNkb"));
            result.setMutationId(asLong(frozen.get("mutationId")));
            result.setMatchedNode((String) frozen.get("matchedNode"));
            result.setEffectText((String) frozen.get("effectText"));
            result.setVariationClass((String) frozen.get("variationClass"));
            result.setDescription((String) frozen.get("description"));
            Object evidence = frozen.get("evidence");
            if (evidence != null) {
                result.setEvidence(JSON.convertValue(evidence, new TypeReference<List<PreviewDrugVo>>() {}));
            }
            Object groups = frozen.get("drugGroups");
            if (groups != null) {
                result.setDrugGroups(JSON.convertValue(groups, new TypeReference<Map<String, String>>() {}));
            }
            Object audit = frozen.get("drugAuditList");
            if (audit != null) {
                result.setDrugAuditList(JSON.convertValue(audit, new TypeReference<List<Map<String, Object>>>() {}));
            }
        } catch (Exception e) {
            // 反序列化失败同样按「旧结构」处理：置空让调用方重算
            log.warn("预览匹配历史反序列化失败，需要按最新规则重算", e);
            result = null;
        }
        return result;
    }

    static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceException("计算匹配键失败：" + e.getMessage());
        }
    }

    static String nz(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 类型列第二段（变异类别）。改口径只需改这张表 —— 用户口径：`体系|变异类别|核酸类型`，
     * 例：`S|Indel|DNA`、`S|Somatic|RNA`（核酸类型只有融合行才有）。
     */
    private static final Map<String, String> VARIANT_KIND = Map.of(
        "SNP_INDEL", "Indel",
        "CNV", "CNV",
        "FUSION", "Somatic",
        "CR_ALL", "Somatic"
    );

    /**
     * 类型列展示值（用户口径）
     * <ul>
     *   <li>胚系（肿瘤遗传风险）统一为 `G`</li>
     *   <li>体细胞为 `S_{变异类别}_{核酸类型}`，如 `S_Indel_DNA`；融合按 tag 判 DNA/RNA（`S_Somatic_RNA`）</li>
     * </ul>
     *
     * @param sourceType    来源表（SNP_INDEL / CNV / FUSION / CR_ALL）
     * @param germline      是否胚系
     * @param fusionQuality 融合质量串（含 DNA → DNA，否则 RNA）
     * @return 展示值
     */
    static String typeText(String sourceType, boolean germline, String fusionQuality) {
        if (germline) {
            return "G";
        }
        String kind = VARIANT_KIND.getOrDefault(sourceType, sourceType);
        String nucleic = "FUSION".equals(sourceType) ? nucleicAcid(fusionQuality) : "DNA";
        return String.join("_", "S", kind, nucleic);
    }

    /** 融合的核酸类型：fusion_quality 含 DNA → DNA；否则按 en7 口径视为 RNA */
    static String nucleicAcid(String fusionQuality) {
        boolean dna = fusionQuality != null
            && fusionQuality.toUpperCase(java.util.Locale.ROOT).contains("DNA");
        return dna ? "DNA" : "RNA";
    }

    /**
     * 变异类型展示值：VEP `ExonicFunc`（体细胞取 `exonic_func_known_gene`，胚系取 `exonic_func`）→ 中文。
     * <p>
     * 映射抄自 en7 `previewReportList.jsp` 的 `translateMutType`；表中没有的值原样返回
     * （CNV / FUSION 行传进来的就是 `CNV` / `FUSION`）。
     *
     * @param exonicFunc VEP 变异类型（如 nonsynonymous SNV）
     * @return 中文展示值；空时返回 null
     */
    static String variantTypeText(String exonicFunc) {
        if (!StringUtils.hasText(exonicFunc)) {
            return null;
        }
        String value = exonicFunc.trim();
        return switch (value) {
            case "nonsynonymous SNV" -> "错义突变";
            case "synonymous SNV" -> "同义突变";
            case "nonframeshift insertion" -> "非移码插入突变";
            case "nonframeshift deletion" -> "非移码缺失突变";
            case "frameshift insertion", "frameshift deletion", "frameshift indel" -> "移码突变";
            case "nonframeshift indel" -> "非移码突变";
            case "stopgain" -> "无义突变";
            case "stoploss" -> "stoploss";
            case "splicing" -> "剪接突变";
            case "promoter" -> "启动子区变异";
            case "unknown" -> "未知";
            default -> value;
        };
    }

    /**
     * 丰度/reads 展示值（对齐 en7 的 mutFreq 口径）
     * <ul>
     *   <li>扩增/缺失（拷贝数）：原值，无单位</li>
     *   <li>融合 DNA：freq 是 0~1 小数 → 乘 100 保留两位 + `%`</li>
     *   <li>融合 RNA：freq 存的是 reads 数 → 取整数部分，无单位</li>
     *   <li>SNP/Indel：库内已是百分数 → 原值 + `%`</li>
     * </ul>
     *
     * @param sourceType    来源表
     * @param freqRaw       原始丰度/拷贝数/reads
     * @param fusionQuality 融合质量串
     * @return 展示值；无丰度口径时返回 null（前端显示 -）
     */
    static String abundanceText(String sourceType, Object freqRaw, String fusionQuality) {
        String empty = null;
        if (freqRaw == null || !StringUtils.hasText(String.valueOf(freqRaw))) {
            return empty;
        }
        String raw = String.valueOf(freqRaw).trim();
        if ("CNV".equals(sourceType)) {
            return raw;
        }
        try {
            if ("FUSION".equals(sourceType)) {
                double value = Double.parseDouble(raw);
                if ("DNA".equals(nucleicAcid(fusionQuality))) {
                    return String.format(java.util.Locale.ROOT, "%.2f%%", value * 100);
                }
                return String.valueOf((long) value);
            }
        } catch (NumberFormatException e) {
            // 非数值（如 "12X"、带文字的描述）：原样展示，不改写业务值
            log.debug("丰度不是纯数值，按原值展示：{}", raw);
            return raw;
        }
        return raw.endsWith("%") ? raw : raw + "%";
    }

    /**
     * 默认排序权重（对齐 en7 setOrderNum：数值越大越靠前）
     * <p>
     * 组成：丰度基数 + 结果类型（有用药证据 70000 / 无证据 10000，CNV 类 +300、融合 +200）
     * + Cosmic 命中 +100 + 药物最高等级权重（A 9000 … 其他 1000）。
     * 这样「I 类/II 类（有药）」天然排在「III 类（无证据）」前面。
     *
     * @param freqRaw  原始丰度
     * @param fused    是否融合
     * @param cnvLike  是否扩增/缺失类
     * @param cosmic   Cosmic 列（空/`.` 视为没有）
     * @param evidence 证据列表（可为空）
     * @return 排序权重
     */
    static float orderNum(Object freqRaw, boolean fused, boolean cnvLike, Object cosmic,
                          List<PreviewDrugVo> evidence) {
        float order = freqOrderBase(freqRaw);
        boolean hasDrug = evidence != null && !evidence.isEmpty();
        if (hasDrug) {
            order += 70000;
            if (cnvLike) {
                order += 300;
            } else if (fused) {
                order += 200;
            }
        } else {
            order += 10000;
        }
        if (cosmic != null && StringUtils.hasText(String.valueOf(cosmic)) && !".".equals(String.valueOf(cosmic))) {
            order += 100;
        }
        return order + (hasDrug ? topLevelWeight(evidence) : 0);
    }

    /**
     * 传给突变说明生成器的丰度值：去尾部 `%`、保留原值（`12X` / `合` / 数值）
     *
     * @param freqRaw 原始丰度
     * @return 归一后的丰度；空或 `.` 返回 null（不输出丰度句）
     */
    static String translationFreq(Object freqRaw) {
        String empty = null;
        if (freqRaw == null) {
            return empty;
        }
        String value = String.valueOf(freqRaw).trim();
        if (!StringUtils.hasText(value) || ".".equals(value)) {
            return empty;
        }
        return value.endsWith("%") ? value.substring(0, value.length() - 1) : value;
    }

    /** 丰度基数：`12X` → 212；空/`.`/含「合」→ 0；其余取数值 */
    private static float freqOrderBase(Object freqRaw) {
        if (freqRaw == null) {
            return 0f;
        }
        String value = String.valueOf(freqRaw).trim();
        if (!StringUtils.hasText(value) || ".".equals(value) || value.contains("合") || value.contains("H")) {
            return 0f;
        }
        try {
            if (value.endsWith("X")) {
                return 200 + Float.parseFloat(value.substring(0, value.length() - 1));
            }
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            // 非数值丰度（如 "12X" 之外的描述）不参与排序权重，按 0 处理
            log.debug("丰度无法转为排序权重，按 0 处理：{}", value);
            return 0f;
        }
    }

    /** 药物最高等级权重（en7：A 9000 / B 8000 / C 7000 / D 6000 / 耐药A 5000 … 其他 1000） */
    private static int topLevelWeight(List<PreviewDrugVo> evidence) {
        int weight = 0;
        for (PreviewDrugVo drug : evidence) {
            Integer range = drug.getApproveRange();
            int current = range == null ? 1000 : Math.max(1000, 10000 - range * 1000);
            weight = Math.max(weight, current);
        }
        return weight;
    }

    /**
     * 位点行展示字段 + 默认排序权重（en7 setOrderNum 口径）
     *
     * @param row      查询原始行
     * @param item     位点行
     * @param germline 是否胚系
     */
    static void decorate(Map<String, Object> row, PreviewVariantVo item, boolean germline) {
        String sourceType = item.getSourceType();
        Object freqRaw = row.get("freqRaw");
        Object cosmic = row.get("cosmic");
        String fusionQuality = asString(row.get("fusionQuality"));
        item.setFileType(asString(row.get("fileType")));
        item.setNucleicAcid("FUSION".equals(sourceType) ? nucleicAcid(fusionQuality) : null);
        item.setTypeText(typeText(sourceType, germline, fusionQuality));
        item.setVariantTypeText(variantTypeText(asString(row.get("mutationTypeRaw"))));
        item.setAbundanceText(abundanceText(sourceType, freqRaw, fusionQuality));
        boolean fused = "FUSION".equals(sourceType);
        boolean cnvLike = "CNV".equals(sourceType)
            || String.valueOf(item.getVariant()).contains("Amplification")
            || String.valueOf(item.getVariant()).contains("Loss");
        item.setOrderNum(orderNum(freqRaw, fused, cnvLike, cosmic, item.getDrugMatch()));
    }

    /** 默认排序：orderNum 降序（有用药证据、丰度高、有 Cosmic 的在前）；同权重按来源主键稳定 */
    static List<PreviewVariantVo> sortByOrderNum(List<PreviewVariantVo> items) {
        items.sort(java.util.Comparator.comparing((PreviewVariantVo row) ->
            row.getOrderNum() == null ? 0f : row.getOrderNum()).reversed()
            .thenComparing(PreviewVariantVo::getSourceId));
        return items;
    }

    static String firstNonBlank(String... values) {
        String hit = null;
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                hit = value.trim();
                break;
            }
        }
        return hit;
    }

    static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    static Long asLong(Object value) {
        return value == null ? null
            : (value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value)));
    }

    static Integer asInt(Object value) {
        return value == null ? null
            : (value instanceof Number number ? number.intValue() : Integer.valueOf(String.valueOf(value)));
    }
}
