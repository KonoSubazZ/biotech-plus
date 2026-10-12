package org.dromara.report.service;

import lombok.Getter;
import org.dromara.report.domain.vo.InterpretationLimsVo;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.dromara.report.utils.ReportNameResolver;
import org.dromara.report.utils.ReportMailDefaults;

/**
 * 报告邮件的字段变量：名字 → 取值来源（一处定义，保存期校验、前端提示、发送渲染都用它）。
 * <p>
 * 渲染与校验都复用 {@link ReportNameResolver}（{@code {{变量}}} + 白名单 + 未闭合/外来语法报错），
 * 所以邮件模板和「报告命名」是同一套写法与同一份变量白名单，没有第二个模板引擎。
 * <p>
 * 值分别来自：报告行（{@code interpretationMapper.selectReportRow} 的 Map）、LIMS 信息（Tab① 同一份对象）。
 *
 * @author <你的名字>
 */
@Component
public class ReportMailVariables {

    /** 可用变量（前端提示用；名字必须在 ReportNameResolver 的白名单里） */
    @Getter
    private final List<String> variables = List.of(
        "patientName", "hospitalName", "doctorName", "subbarcode", "product",
        "disease", "sampleCode", "gender", "analysisId", "reportId", "currentDate");

    /** 样例值（页面「试算」用；值只为演示口径，不是某份报告的真实数据） */
    private static final Map<String, String> SAMPLE_VALUES = Map.ofEntries(
        Map.entry("patientName", "金启学"),
        Map.entry("hospitalName", "示例医院"),
        Map.entry("doctorName", "示例医生"),
        Map.entry("subbarcode", "YKHS260037665-1A"),
        Map.entry("product", "novopm2_tis_1238_shengyu"),
        Map.entry("disease", "结直肠癌"),
        Map.entry("sampleCode", "YKHS260037665-1A"),
        Map.entry("gender", "男"),
        Map.entry("analysisId", "1"),
        Map.entry("reportId", "3"),
        Map.entry("currentDate", "2026-10-05"));

    /** 样例值（前端试算） */
    @Getter
    private final Map<String, String> sample = SAMPLE_VALUES;

    /**
     * 组装某份报告的邮件变量值。
     *
     * @param reportRow 报告行（{@code selectReportRow} 的 Map，键见 InterpretationMapper.xml）
     * @param lims      LIMS 信息（Tab① 的同一份对象，可空）
     * @return 变量名 → 值（值为 null 的键会被剔除，渲染时按空串处理）
     */
    public Map<String, String> build(Map<String, Object> reportRow, InterpretationLimsVo lims) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("subbarcode", rowText(reportRow, "subbarcode"));
        values.put("product", rowText(reportRow, "product"));
        values.put("disease", rowText(reportRow, "disease"));
        values.put("analysisId", rowText(reportRow, "analysisId"));
        values.put("reportId", rowText(reportRow, "reportId"));
        if (lims != null) {
            values.put("patientName", lims.getPatientName());
            values.put("hospitalName", lims.getHospitalName());
            values.put("doctorName", lims.getDoctorName());
            values.put("sampleCode", lims.getSubbarcode());
            values.put("gender", lims.getGender());
        }
        values.put("currentDate", LocalDate.now().toString());
        // 缺失的键统一剔除：render 用 getOrDefault(key, "") 兜空串，
        // 但 key 存在且值为 null 时 getOrDefault 会返回 null，把 "null" 写进标题
        values.values().removeIf(value -> value == null);
        return values;
    }

    /**
     * 渲染邮件模板。pattern 为空时**不落到报告命名的默认规则**（{@link ReportNameResolver#DEFAULT_PATTERN}），
     * 由调用方决定兜底模板（邮件用 {@link ReportMailDefaults#SUBJECT} / {@link ReportMailDefaults#CONTENT}）。
     *
     * @param pattern 模板（非空）
     * @param values  变量值
     * @return 渲染结果
     */
    public String render(String pattern, Map<String, String> values) {
        return ReportNameResolver.render(pattern, values);
    }

    private static String rowText(Map<String, Object> row, String key) {
        Object value = row == null ? null : row.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
