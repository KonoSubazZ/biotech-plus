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

    static String clsPrefixLabel(String classPrefix) {
        if ("Class5".equals(classPrefix)) {
            return "Class5(致病)";
        }
        return "Class4".equals(classPrefix) ? "Class4(疑似致病)" : null;
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

    static List<PreviewDrugVo> parseDrugMatch(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return JSON.readValue(json, new TypeReference<List<PreviewDrugVo>>() {});
        } catch (Exception e) {
            log.warn("预览匹配历史反序列化失败，按无证据处理", e);
            return List.of();
        }
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
