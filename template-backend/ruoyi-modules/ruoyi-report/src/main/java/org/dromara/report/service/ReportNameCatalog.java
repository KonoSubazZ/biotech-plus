package org.dromara.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.dromara.common.core.exception.ServiceException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 报告命名变量字典（report_template.report_name 用）。
 * <p>
 * 把样例 JSON（真实渲染 JSON 的一份快照）递归压平成点号路径，再补上
 * template.* / report.* / now.* 三个非 JSON 命名空间的固定路径。
 * <p>
 * 为什么不手写变量清单：变量随 JSON 契约变化，手写必然漂移。同一份字典同时供
 * 「保存模板时校验」「前端可用变量提示」「前端试算样例」三处使用，
 * 前端通过 {@code GET /report/template/name-vars} 取，不再自己维护一份。
 *
 * @author <你的名字>
 */
@Component
public class ReportNameCatalog {

    /** 样例 JSON（classpath）；字段名必须与真实渲染 JSON 一致，新增个性化字段时同步补一条 */
    private static final String SAMPLE_JSON = "report-samples/report-name-sample.json";

    /** 非 JSON 命名空间的固定路径：模板行 / 报告行（批次快照）/ 生成时刻 */
    public static final Set<String> FIXED_PATHS = Set.of(
        "template.customerCode", "template.templateName", "template.templateVersion",
        "template.reportType", "template.templatePath",
        "report.subbarcode", "report.product", "report.reportType", "report.analysisDate",
        "report.disease", "report.cancerType",
        "now.date", "now.time", "now.iso");

    /** 可引用的叶子路径（点号形式） */
    @Getter
    private final Set<String> paths = new TreeSet<>();

    /** 对象/数组路径：不能引用，用来把「用错」与「拼错」分开报错 */
    @Getter
    private final Set<String> aggregates = new TreeSet<>();

    /** 样例值（前端试算用；直接返回 JSON 树，前端拿到的就是渲染 JSON 的形状） */
    @Getter
    private final JsonNode sample;

    public ReportNameCatalog(ObjectMapper objectMapper) {
        this.sample = readSample(objectMapper);
        flatten(this.sample, "", this.paths, this.aggregates);
        this.paths.addAll(FIXED_PATHS);
    }

    /**
     * 保存模板时校验命名模板。
     *
     * @param pattern 命名模板；空值合法（表示用默认命名）
     */
    public void validate(String pattern) {
        ReportNameResolver.validate(pattern, this);
    }

    /**
     * 变量不可用时的提示文案：优先给同前缀的可用路径（错别字场景），其次给前 20 个路径。
     *
     * @param path 出问题的路径
     * @return 提示文案
     */
    public String hint(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        if (!path.contains(".")) {
            return "可用命名空间：template.*、report.*、now.*，以及渲染 JSON 的根字段（如 sampleInfo.sampleCode）";
        }
        String prefix = path.substring(0, path.lastIndexOf('.') + 1);
        List<String> samePrefix = paths.stream().filter(item -> item.startsWith(prefix)).limit(10).toList();
        if (!samePrefix.isEmpty()) {
            return "该前缀下可用：" + String.join(", ", samePrefix);
        }
        List<String> firstPaths = paths.stream().limit(20).toList();
        return "可用变量（前 20 个）：" + String.join(", ", firstPaths);
    }

    /** 读样例 JSON；读不到直接失败（字典缺失会让保存期校验失效，不能静默降级） */
    private static JsonNode readSample(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource(SAMPLE_JSON);
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readTree(input);
        } catch (IOException e) {
            throw new ServiceException("报告命名变量字典样例读取失败：" + SAMPLE_JSON + "，" + e.getMessage());
        }
    }

    /**
     * 递归压平：对象继续下钻，数组只登记为不可引用的 aggregate，
     * null 叶子也登记为可用路径（生成时按空串处理）。
     */
    private static void flatten(JsonNode node, String prefix, Set<String> paths, Set<String> aggregates) {
        if (node.isObject()) {
            if (!prefix.isEmpty()) {
                aggregates.add(prefix);
            }
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                flatten(field.getValue(), childPrefix(prefix, field.getKey()), paths, aggregates);
            }
            return;
        }
        if (node.isArray()) {
            if (!prefix.isEmpty()) {
                aggregates.add(prefix);
            }
            return;
        }
        if (!prefix.isEmpty()) {
            paths.add(prefix);
        }
    }

    /** 点号路径拼接：顶层字段没有前缀 */
    private static String childPrefix(String prefix, String key) {
        if (prefix.isEmpty()) {
            return key;
        }
        return prefix + "." + key;
    }
}
