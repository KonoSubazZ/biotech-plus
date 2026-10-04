package org.dromara.report.service;

import org.dromara.common.core.exception.ServiceException;
import org.springframework.util.PropertyPlaceholderHelper;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 报告命名模板解析：把 {@code {{路径}}} 按变量字典渲染成报告名与落盘文件名。
 * <p>
 * 命名模板只有两种成分：{@code {{路径}}} 是动态取值，其余是静态文本、逐字原样保留。
 * 刻意不引入 FreeMarker：取值 + 字典校验用 spring-core 自带的
 * {@link PropertyPlaceholderHelper}（前后缀 {{ }}、不允许默认值语法、解析不到就抛错）已足够，
 * 且模板里写不出后端预期之外的东西。
 *
 * @author <你的名字>
 */
public final class ReportNameResolver {

    /** 取值为空就报错的路径（否则会产出「圣域__报告」这类残缺名） */
    private static final Set<String> REQUIRED_PATHS =
        new LinkedHashSet<>(Set.of("reportId", "templateCode", "template.templateName"));

    /** valueSeparator=null → 不支持 {{k:默认}}；ignoreUnresolvable=false → 解析不到就抛错 */
    private static final PropertyPlaceholderHelper PLACEHOLDER_HELPER =
        new PropertyPlaceholderHelper("{{", "}}", null, false);

    /** 占位符内容：点号分隔的路径，段内只允许 [A-Za-z0-9_] */
    private static final Pattern PLACEHOLDER =
        Pattern.compile("\\{\\{\\s*([A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)*)\\s*}}");

    /** 只拒绝别的模板引擎标记；其余静态字符（中文、空格、: # ( ) 等）一律放行 */
    private static final Pattern FOREIGN_SYNTAX = Pattern.compile("\\{%|#\\{|\\$\\{");

    /** 文件名长度上限：中文按 3 字节算，80 字符 ≈ 240 字节 < 255 */
    private static final int MAX_FILE_NAME_LENGTH = 80;

    private ReportNameResolver() {
    }

    /**
     * 校验命名模板的语法与变量。空模板合法（表示用默认命名）。
     *
     * @param pattern 命名模板
     * @param catalog 变量字典
     */
    public static void validate(String pattern, ReportNameCatalog catalog) {
        if (pattern == null || pattern.isBlank()) {
            return;
        }
        if (FOREIGN_SYNTAX.matcher(pattern).find()) {
            throw new ServiceException(
                "报告命名模板不支持 {% %}/#{ }/${ }，只支持 {{路径}}（如 {{sampleInfo.sampleCode}}）：" + pattern, 400);
        }
        Matcher matcher = PLACEHOLDER.matcher(pattern);
        Set<String> usedPaths = new LinkedHashSet<>();
        while (matcher.find()) {
            usedPaths.add(matcher.group(1));
        }
        String staticText = PLACEHOLDER.matcher(pattern).replaceAll("");
        if (staticText.contains("{{") || staticText.contains("}}")) {
            throw new ServiceException("报告命名模板存在未闭合或非法占位符：" + pattern, 400);
        }
        for (String path : usedPaths) {
            assertPathUsable(path, catalog);
        }
    }

    /** 单个变量必须存在于字典，且不能是对象/数组 */
    private static void assertPathUsable(String path, ReportNameCatalog catalog) {
        if (catalog.getAggregates().contains(path)) {
            throw new ServiceException("报告命名模板不能引用对象/数组字段：" + path + "；" + catalog.hint(path), 400);
        }
        if (!catalog.getPaths().contains(path)) {
            throw new ServiceException("报告命名模板变量不存在：" + path + "；" + catalog.hint(path), 400);
        }
    }

    /**
     * 渲染报告名：静态文本与变量值原样拼接，只做 trim（文件名安全化见 {@link #toFileName}）。
     *
     * @param pattern 命名模板
     * @param vars    变量值（点号路径 → 值；字典里有但本次数据没有的按空串处理）
     * @param catalog 变量字典
     * @return 报告名；未配置命名模板时返回空串（调用方据此回退默认命名）
     */
    public static String render(String pattern, Map<String, String> vars, ReportNameCatalog catalog) {
        if (pattern == null || pattern.isBlank()) {
            return ""; // 未配置命名模板 = 空名，调用方据此回退默认命名
        }
        validate(pattern, catalog);
        assertRequiredPathsPresent(vars);
        String normalized = PLACEHOLDER.matcher(pattern).replaceAll(match -> "{{" + match.group(1) + "}}");
        String rendered = PLACEHOLDER_HELPER.replacePlaceholders(normalized, key -> vars.getOrDefault(key, ""));
        return rendered.trim();
    }

    /** 必填路径取值为空时直接报错，避免静默产出残缺报告名 */
    private static void assertRequiredPathsPresent(Map<String, String> vars) {
        for (String path : REQUIRED_PATHS) {
            String value = vars.get(path);
            if (value == null || value.isBlank()) {
                throw new ServiceException("报告命名模板变量 " + path + " 取值为空，无法生成报告名");
            }
        }
    }

    /**
     * 报告名 → 落盘文件名：非法字符替换、压缩连续下划线、去首尾空白与点、限长。
     *
     * @param reportName 报告名（可含中文、空格、: 等静态文本）
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
