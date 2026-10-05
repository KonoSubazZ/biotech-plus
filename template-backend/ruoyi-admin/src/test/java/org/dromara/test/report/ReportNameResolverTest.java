package org.dromara.test.report;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.service.ReportNameResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * 报告命名解析单测（扁平字段变量 + 默认拼接规则）。
 * <p>
 * 只跑本类：mvn -Plocal -DskipTests=false -Dtest=ReportNameResolverTest test
 *
 * @author <你的名字>
 */
@Tag("local")
@DisplayName("报告命名解析")
public class ReportNameResolverTest {

    private Map<String, String> variables() {
        Map<String, String> variables = new HashMap<>();
        variables.put("client", "圣域");
        variables.put("templateCode", "pharma-shengyu");
        variables.put("templateName", "同源重组修复（HRR）通路基因检测报告-圣域");
        variables.put("templateVersion", "v1");
        variables.put("reportId", "3");
        variables.put("analysisId", "1");
        variables.put("subbarcode", "YKHS260031036-1A");
        variables.put("sampleCode", "YKHS260031036-1A");
        variables.put("gender", "男");
        variables.put("disease", "结直肠癌");
        variables.put("specimenType", "tissue");
        variables.put("reportDate", "2026-10-03");
        return variables;
    }

    @Test
    @DisplayName("同名变量直接替换 + 占位符内空格归一化")
    void renderByVariableName() {
        String reportName = ReportNameResolver.render("{{subbarcode}}_{{ client }}_{{ reportId }}", variables());
        Assertions.assertEquals("YKHS260031036-1A_圣域_3", reportName);
    }

    @Test
    @DisplayName("模板为空 → 用默认拼接规则 {{subbarcode}}{{client}}{{template_name}}{{report_id}}")
    void defaultPatternWhenBlank() {
        Assertions.assertEquals("{{subbarcode}}{{client}}{{templateName}}{{reportId}}",
            ReportNameResolver.DEFAULT_PATTERN);
        String expected = "YKHS260031036-1A圣域同源重组修复（HRR）通路基因检测报告-圣域3";
        Assertions.assertEquals(expected, ReportNameResolver.render(null, variables()));
        Assertions.assertEquals(expected, ReportNameResolver.render("   ", variables()));
    }

    @Test
    @DisplayName("变量取不到值 → 按空串拼接（不报错）")
    void missingVariableRendersEmpty() {
        Assertions.assertEquals("圣域_", ReportNameResolver.render("{{client}}_{{birthYear}}", variables()));
    }

    @Test
    @DisplayName("未知变量 → 报错并列出可用变量")
    void unknownVariable() {
        ServiceException exception = Assertions.assertThrows(ServiceException.class,
            () -> ReportNameResolver.validate("{{client}}_{{客户}}"));
        Assertions.assertTrue(exception.getMessage().contains("客户"));
        Assertions.assertTrue(exception.getMessage().contains("subbarcode"));
    }

    @Test
    @DisplayName("拒绝 Jinja/SpEL/表达式写法")
    void forbiddenSyntax() {
        Assertions.assertThrows(ServiceException.class,
            () -> ReportNameResolver.validate("{% if client %}a{% endif %}"));
        Assertions.assertThrows(ServiceException.class, () -> ReportNameResolver.validate("${client}"));
        Assertions.assertThrows(ServiceException.class, () -> ReportNameResolver.validate("#{client}"));
        Assertions.assertThrows(ServiceException.class, () -> ReportNameResolver.validate("{{client|upper}}"));
        Assertions.assertThrows(ServiceException.class, () -> ReportNameResolver.validate("圣域_{{client"));
    }

    @Test
    @DisplayName("静态文本原样保留；落盘文件名才做安全化与截断")
    void fileNameSanitize() {
        String reportName = ReportNameResolver.render("圣域:1238 报告 #3_{{reportId}}", variables());
        Assertions.assertEquals("圣域:1238 报告 #3_3", reportName);
        Assertions.assertEquals("圣域_1238 报告 #3_3", ReportNameResolver.toFileName(reportName));
        Assertions.assertEquals("a_b_c.._c", ReportNameResolver.toFileName("a/b:c..__c "));
        Assertions.assertEquals("报告", ReportNameResolver.toFileName(" 报告. "));
    }

    @Test
    @DisplayName("落盘文件名按 80 字符截断（报告名不截断）")
    void longNameTruncatedOnFileName() {
        Map<String, String> variables = variables();
        variables.put("subbarcode", "甲".repeat(200));
        String reportName = ReportNameResolver.render("{{subbarcode}}", variables);
        Assertions.assertEquals(200, reportName.length());
        Assertions.assertTrue(ReportNameResolver.toFileName(reportName).length() <= 80);
    }
}
