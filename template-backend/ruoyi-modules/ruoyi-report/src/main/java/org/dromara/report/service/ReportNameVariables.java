package org.dromara.report.service;

import lombok.Getter;
import org.dromara.report.domain.vo.ReportTemplateData;
import org.dromara.report.domain.vo.ReportTemplateVo;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 报告命名的字段变量：名字 → 取值来源（一处定义，校验、生成、前端提示都用它）。
 * <p>
 * 刻意保持扁平：变量名就是 {@code {{client}}} 里替换用的名字，没有点号路径、没有命名空间、没有外部字典文件。
 * 值分别来自模板行（report_template）、报告行（analysis_report）和渲染 JSON 的公共字段（sampleInfo / reportInfo）。
 *
 * @author <你的名字>
 */
@Component
public class ReportNameVariables {

    /** 前端「可用变量」提示与试算用的样例值（值只为演示口径，不是某份报告的真实数据） */
    private static final Map<String, String> SAMPLE_VALUES = Map.ofEntries(
        Map.entry("client", "圣域"),
        Map.entry("templateCode", "pharma-shengyu"),
        Map.entry("templateName", "同源重组修复（HRR）通路基因检测报告-圣域"),
        Map.entry("templateVersion", "v1"),
        Map.entry("reportType", "HRR"),
        Map.entry("reportId", "3"),
        Map.entry("analysisId", "1"),
        Map.entry("subbarcode", "YKHS260031036-1A"),
        Map.entry("sampleCode", "YKHS260031036-1A"),
        Map.entry("product", "novopm2_tis_1238_shengyu"),
        Map.entry("disease", "结直肠癌"),
        Map.entry("gender", "男"),
        Map.entry("birthYear", "1980"),
        Map.entry("researchCenterName", "示例医院"),
        Map.entry("participantNumber", "P001"),
        Map.entry("specimenType", "tissue"),
        Map.entry("reportDate", "2026-10-03"),
        Map.entry("analysisDate", "20261003"));

    /** 可用变量清单（顺序用于前端提示） */
    @Getter
    private final List<String> variables = ReportNameResolver.ALLOWED_VARIABLES;

    /** 样例值（前端试算） */
    @Getter
    private final Map<String, String> sample = SAMPLE_VALUES;

    /** 保存模板时校验命名模板（Service 直接调，校验入口只有这里） */
    public void validate(String pattern) {
        ReportNameResolver.validate(pattern);
    }

    /**
     * 组装本次生成的变量值。
     *
     * @param template 模板行（可空）
     * @param data     渲染 JSON 公共字段（可空）
     * @param reportRow 报告行快照（可空）
     * @return 变量名 → 值（值为 null 的键会被剔除，渲染时按空串处理）
     */
    public Map<String, String> build(ReportTemplateVo template, ReportTemplateData data,
                                     Map<String, Object> reportRow) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("client", template == null ? null : template.getCustomerCode());
        variables.put("templateCode", template == null ? null : template.getTemplateCode());
        variables.put("templateName", template == null ? null : template.getTemplateName());
        variables.put("templateVersion", template == null ? null : template.getTemplateVersion());
        variables.put("reportType", template == null ? null : template.getReportType());

        ReportTemplateData.ReportInfo reportInfo = data == null ? null : data.getReportInfo();
        ReportTemplateData.SampleInfo sampleInfo = data == null ? null : data.getSampleInfo();
        variables.put("sampleCode", sampleInfo == null ? null : sampleInfo.getSampleCode());
        variables.put("gender", sampleInfo == null ? null : sampleInfo.getGender());
        variables.put("birthYear", sampleInfo == null ? null : sampleInfo.getBirthYear());
        variables.put("researchCenterName", sampleInfo == null ? null : sampleInfo.getResearchCenterName());
        variables.put("participantNumber", sampleInfo == null ? null : sampleInfo.getParticipantNumber());
        variables.put("disease", sampleInfo == null ? null : sampleInfo.getDisease());
        variables.put("specimenType", reportInfo == null ? null : reportInfo.getSpecimentType());
        variables.put("reportDate", reportInfo == null ? null : reportInfo.getReportDate());

        variables.put("reportId", text(data == null ? null : data.getReportId()));
        variables.put("analysisId", text(data == null ? null : data.getAnalysisId()));
        variables.put("subbarcode", rowText(reportRow, "subbarcode"));
        variables.put("product", rowText(reportRow, "product"));
        variables.put("analysisDate", rowText(reportRow, "analysisDate"));
        // 缺失的键统一剔除：render 用 getOrDefault(key, "") 兜空串，
        // 但 key 存在且值为 null 时 getOrDefault 会返回 null，把 "null" 写进名字
        variables.values().removeIf(value -> value == null);
        return variables;
    }

    /** 报告名 = 配置的命名模板或默认规则渲染结果；结果为空时返回 null（调用方回退默认文件名） */
    public String render(String pattern, Map<String, String> variables) {
        String reportName = ReportNameResolver.render(pattern, variables);
        return reportName.isBlank() ? null : reportName;
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String rowText(Map<String, Object> row, String key) {
        return row == null ? null : text(row.get(key));
    }
}
