package org.dromara.report.service;

import org.dromara.common.core.exception.ServiceException;
import org.springframework.util.PropertyPlaceholderHelper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 报告命名解析：{@code {{变量}}} 用同名变量直接替换，其余全是静态文本、原样保留。
 * <p>
 * 变量是**扁平字段名**（如 {@code {{client}}}、{@code {{subbarcode}}}），不搞点号路径/命名空间/字典，
 * 可用的就下面 {@link #ALLOWED_VARIABLES} 这些，值由调用方按同样名字塞进 map。
 * <p>
 * 不引 FreeMarker：取值 + 白名单用 spring-core 自带的 {@link PropertyPlaceholderHelper} 足够，
 * 且模板里写不出后端预期之外的语法。
 *
 * @author <你的名字>
 */
public final class ReportNameResolver {

    /** 可用的字段变量：名字就是替换用的名字（见 ReportNameVariables 里的取值来源） */
    public static final List<String> ALLOWED_VARIABLES = List.of(
        "client", "templateCode", "templateName", "templateVersion", "reportType",
        "reportId", "analysisId", "subbarcode", "sampleCode", "product", "disease",
        "gender", "birthYear", "researchCenterName", "participantNumber",
        "specimenType", "reportDate", "analysisDate");

    /** report_template.report_name 为空时的默认拼接规则（业务口径，改这里等于改默认命名） */
    public static final String DEFAULT_PATTERN =
        "{{subbarcode}}{{client}}{{templateName}}{{reportId}}";

    private static final Set<String> ALLOWED = new LinkedHashSet<>(ALLOWED_VARIABLES);

    /** valueSeparator=null → 不支持 {{k:默认}}；ignoreUnresolvable=false → 解析不到就抛错 */
    private static final PropertyPlaceholderHelper PLACEHOLDER_HELPER =
        new PropertyPlaceholderHelper("{{", "}}", null, false);

    /** 占位符内容：只允许变量名（替换用；匹配不到的内容交给下面的宽松匹配给报错文案） */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_]+)\\s*}}");

    /** 宽松匹配占位符内容（含中文、竖线等非法写法），只用于报出「哪个变量不合法」 */
    private static final Pattern LOOSE_PLACEHOLDER = Pattern.compile("\\{\\{\\s*([^{}]+?)\\s*}}");

    /** 名字里不认别的模板引擎写法（{% %}/#{ }/${ }），大概率是写错了 */
    private static final Pattern FOREIGN_SYNTAX = Pattern.compile("\\{%|#\\{|\\$\\{");

    /** 文件名长度上限：中文按 3 字节算，80 字符 ≈ 240 字节 < 255 */
    private static final int MAX_FILE_NAME_LENGTH = 80;

    private ReportNameResolver() {
    }

    /**
     * 校验命名模板：用法 + 变量都在白名单里。空模板合法（表示用默认拼接规则）。
     *
     * @param pattern 命名模板
     */
    public static void validate(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return;
        }
        if (FOREIGN_SYNTAX.matcher(pattern).find()) {
            throw new ServiceException(
                "报告命名只支持 {{变量}}（如 {{client}}），不支持 {% %}/#{ }/${ }：" + pattern, 400);
        }
        String staticText = LOOSE_PLACEHOLDER.matcher(pattern).replaceAll("");
        if (staticText.contains("{{") || staticText.contains("}}")) {
            throw new ServiceException("报告命名存在未闭合或非法占位符：" + pattern, 400);
        }
        Matcher matcher = LOOSE_PLACEHOLDER.matcher(pattern);
        Set<String> used = new LinkedHashSet<>();
        while (matcher.find()) {
            used.add(matcher.group(1).trim());
        }
        for (String variable : used) {
            if (!ALLOWED.contains(variable)) {
                throw new ServiceException("报告命名变量不合法：" + variable
                    + "；可用变量：" + String.join(", ", ALLOWED_VARIABLES), 400);
            }
        }
    }

    /**
     * 渲染报告名：{@code {{变量}}} 换成值，其余原样。模板为空时用 {@link #DEFAULT_PATTERN}。
     *
     * @param pattern 命名模板（可空）
     * @param variables 变量值（名字与 {@link #ALLOWED_VARIABLES} 一致；缺的按空串）
     * @return 报告名（未做文件名安全化，落盘前用 {@link #toFileName}）
     */
    public static String render(String pattern, Map<String, String> variables) {
        String effective = pattern == null || pattern.isBlank() ? DEFAULT_PATTERN : pattern;
        validate(effective);
        String normalized = PLACEHOLDER.matcher(effective).replaceAll(match -> "{{" + match.group(1) + "}}");
        String rendered = PLACEHOLDER_HELPER.replacePlaceholders(normalized, key -> variables.getOrDefault(key, ""));
        return rendered.trim();
    }

    /**
     * 报告名 → 落盘文件名：非法字符替换、压缩连续下划线、去首尾空白与点、限长。
     *
     * @param reportName 报告名
     * @return 安全文件名；入参为空时返回空串
     */
    public static String toFileName(String reportName) {
        String fileName = reportName == null ? "" : reportName.trim();
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_");
        fileName = fileName.replaceAll("_{2,}", "_");
        fileName = fileName.replaceAll("^[\\s.]+", "");
        fileName = fileName.replaceAll("[\\s.]+$", "");
        if (fileName.length() > MAX_FILE_NAME_LENGTH) {
            fileName = fileName.substring(0, MAX_FILE_NAME_LENGTH);
        }
        return fileName;
    }
}
