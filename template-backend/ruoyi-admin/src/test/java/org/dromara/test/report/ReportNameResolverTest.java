package org.dromara.test.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.service.ReportNameCatalog;
import org.dromara.report.service.ReportNameResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * 报告命名模板解析单测（用例清单见各 @DisplayName）。
 * <p>
 * 只跑本类：mvn -Plocal -DskipTests=false -Dtest=ReportNameResolverTest test
 *
 * @author <你的名字>
 */
@Tag("local")
@DisplayName("报告命名模板解析")
public class ReportNameResolverTest {

    /** 用真实样例 JSON 建字典：顺带验证 ReportNameCatalog 的压平口径 */
    private static final ReportNameCatalog CATALOG = new ReportNameCatalog(new ObjectMapper());

    private Map<String, String> vars() {
        Map<String, String> vars = new HashMap<>();
        vars.put("reportId", "456");
        vars.put("templateCode", "pharma-shengyu");
        vars.put("template.templateName", "圣域 1238 报告");
        vars.put("template.customerCode", "shengyu");
        vars.put("sampleInfo.sampleCode", "YKHS260031036-1AT");
        vars.put("now.date", "20261004");
        return vars;
    }

    @Test
    @DisplayName("字典压平：多层路径可用，数组/对象只登记为不可引用")
    void catalogFlatten() {
        Assertions.assertTrue(CATALOG.getPaths().contains("sampleInfo.sampleCode"));
        Assertions.assertTrue(CATALOG.getPaths().contains("qualityControl.sampleQc.meanDepth"));
        Assertions.assertTrue(CATALOG.getPaths().contains("reportInfo.reportDate"));
        Assertions.assertTrue(CATALOG.getPaths().contains("sampleInfo.visitCycle"));
        Assertions.assertTrue(CATALOG.getPaths().contains("template.templateName"));
        Assertions.assertFalse(CATALOG.getPaths().contains("somaticVariants.items"));
        Assertions.assertTrue(CATALOG.getAggregates().contains("somaticVariants.items"));
        Assertions.assertTrue(CATALOG.getAggregates().contains("sampleInfo"));
    }

    @Test
    @DisplayName("多层路径渲染：{{a.b}} 取值 + 内部空格归一化")
    void renderNestedPath() {
        String reportName = ReportNameResolver.render(
            "圣域_{{template.customerCode}}_{{ sampleInfo.sampleCode }}_{{reportId}}", vars(), CATALOG);
        Assertions.assertEquals("圣域_shengyu_YKHS260031036-1AT_456", reportName);
    }

    @Test
    @DisplayName("未配置命名模板 → 空串（调用方回退默认命名）")
    void blankPattern() {
        Assertions.assertEquals("", ReportNameResolver.render(null, vars(), CATALOG));
        Assertions.assertEquals("", ReportNameResolver.render("  ", vars(), CATALOG));
    }

    @Test
    @DisplayName("静态文本原样保留：报告名不动，落盘文件名才替换非法字符")
    void staticTextAndFileName() {
        String reportName = ReportNameResolver.render("圣域:1238 报告 #3_{{reportId}}", vars(), CATALOG);
        Assertions.assertEquals("圣域:1238 报告 #3_456", reportName);
        Assertions.assertEquals("圣域_1238 报告 #3_456", ReportNameResolver.toFileName(reportName));
    }

    @Test
    @DisplayName("未知命名空间 → 报错并给出可用变量提示")
    void unknownNamespace() {
        ServiceException exception = Assertions.assertThrows(ServiceException.class,
            () -> ReportNameResolver.validate("圣域_{{client}}_{{reportId}}", CATALOG));
        Assertions.assertTrue(exception.getMessage().contains("client"));
        Assertions.assertTrue(exception.getMessage().contains("可用命名空间"));
    }

    @Test
    @DisplayName("路径拼错 → 报错并提示同前缀的可用路径")
    void typoPath() {
        ServiceException exception = Assertions.assertThrows(ServiceException.class,
            () -> ReportNameResolver.validate("{{sampleInfo.sampleCodee}}", CATALOG));
        Assertions.assertTrue(exception.getMessage().contains("sampleInfo.sampleCode"));
    }

    @Test
    @DisplayName("对象/数组路径不可引用")
    void aggregatePathForbidden() {
        Assertions.assertThrows(ServiceException.class,
            () -> ReportNameResolver.validate("{{sampleInfo}}", CATALOG));
        Assertions.assertThrows(ServiceException.class,
            () -> ReportNameResolver.validate("{{somaticVariants.items}}", CATALOG));
        Assertions.assertThrows(ServiceException.class,
            () -> ReportNameResolver.validate("{{qualityControl.sampleQc}}", CATALOG));
    }

    @Test
    @DisplayName("拒绝 Jinja/SpEL/表达式风格写法")
    void forbiddenSyntax() {
        Assertions.assertThrows(ServiceException.class,
            () -> ReportNameResolver.validate("{% if a %}x{% endif %}", CATALOG));
        Assertions.assertThrows(ServiceException.class,
            () -> ReportNameResolver.validate("${reportId}", CATALOG));
        Assertions.assertThrows(ServiceException.class,
            () -> ReportNameResolver.validate("#{reportId}", CATALOG));
        Assertions.assertThrows(ServiceException.class,
            () -> ReportNameResolver.validate("{{sampleInfo.sampleCode|upper}}", CATALOG));
    }

    @Test
    @DisplayName("未闭合占位符 → 报错")
    void unclosedPlaceholder() {
        Assertions.assertThrows(ServiceException.class,
            () -> ReportNameResolver.validate("圣域_{{reportId", CATALOG));
    }

    @Test
    @DisplayName("必填路径取值为空 → 报错")
    void requiredPathBlank() {
        Map<String, String> vars = vars();
        vars.put("reportId", "  ");
        Assertions.assertThrows(ServiceException.class,
            () -> ReportNameResolver.render("{{templateCode}}_{{reportId}}", vars, CATALOG));
    }

    @Test
    @DisplayName("落盘文件名安全化：非法字符替换、连续下划线压缩、首尾点去净")
    void fileNameSanitize() {
        Assertions.assertEquals("a_b_c.._c", ReportNameResolver.toFileName("a/b:c..__c "));
        Assertions.assertEquals("报告", ReportNameResolver.toFileName(" 报告. "));
    }

    @Test
    @DisplayName("报告名不截断，落盘文件名按 80 字符截断")
    void longNameTruncatedOnFileName() {
        Map<String, String> vars = vars();
        vars.put("sampleInfo.sampleCode", "甲".repeat(200));
        String reportName = ReportNameResolver.render("{{sampleInfo.sampleCode}}", vars, CATALOG);
        Assertions.assertEquals(200, reportName.length());
        Assertions.assertTrue(ReportNameResolver.toFileName(reportName).length() <= 80);
    }
}
