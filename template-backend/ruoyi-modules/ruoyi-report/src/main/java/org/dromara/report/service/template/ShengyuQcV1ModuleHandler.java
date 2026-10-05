package org.dromara.report.service.template;

import lombok.RequiredArgsConstructor;
import org.dromara.report.domain.dto.ReportModuleContext;
import org.dromara.report.domain.vo.ReportTemplateData;
import org.dromara.report.mapper.InterpretationMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 圣域个性化：质控（module_code = {@value #MODULE_CODE}）。
 * <p>
 * 口径（用户 2026-10-04 拍板）：<b>只输出指标值</b>。
 * 阈值与「合格 / 不合格」判定文案<b>静态写在 DOCX 模板里</b>，不进 JSON ——
 * 所以没有 standard / status / assessment 字段。
 * <p>
 * 数据源：{@code file_qc}（样本质控）与 {@code file_qc_control}（对照质控），
 * 取该分析批次最新一条。
 *
 * @author <你的名字>
 */
@Component
@RequiredArgsConstructor
public class ShengyuQcV1ModuleHandler implements ReportModuleHandler {

    /** 模块编码（与 report_template.module_code 一致） */
    public static final String MODULE_CODE = "SHENGYU_QC_V1";

    /** 输出的顶层 JSON 字段名 */
    public static final String JSON_FIELD = "qualityControl";

    /** 指标输出顺序（key = SQL 别名 / JSON 字段名） */
    private static final String[] QC_KEYS = {
        "tumorCellContent", "dnaTotal", "dnaDegradation", "preLibraryTotal", "sequencingDataVolume",
        "meanDepth", "coverageUniformity", "targetRegionCoverage", "genomeAlignmentRate", "baseQualityQ30Rate"
    };

    private final InterpretationMapper interpretationMapper;

    @Override
    public String moduleCode() {
        return MODULE_CODE;
    }

    @Override
    public void apply(ReportModuleContext context, ReportTemplateData target) {
        ReportTemplateData.QualityControl qc = new ReportTemplateData.QualityControl();
        qc.setSpecimenType(context.getSpecimenType());
        qc.setSampleQc(metricValues(interpretationMapper.selectSampleQc(context.getAnalysisId())));
        qc.setControlQc(metricValues(interpretationMapper.selectControlQc(context.getAnalysisId())));
        target.putPersonalizedModule(JSON_FIELD, qc);
    }

    /** 把一行质控记录投影成「指标 → 值」（缺值保持 null，不补符号） */
    private Map<String, String> metricValues(Map<String, Object> row) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : QC_KEYS) {
            values.put(key, row == null ? null : text(row.get(key)));
        }
        return values;
    }

    private String text(Object value) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return StringUtils.hasText(text) ? text : null;
    }
}
