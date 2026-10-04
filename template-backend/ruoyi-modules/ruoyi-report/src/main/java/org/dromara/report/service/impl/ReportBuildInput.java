package org.dromara.report.service.impl;

import lombok.Data;
import org.dromara.report.domain.vo.InterpretationLimsVo;
import org.dromara.report.domain.vo.PreviewVariantVo;
import org.dromara.report.domain.vo.ReportTemplateVo;

import java.util.List;
import java.util.Map;

/**
 * 报告 JSON 组装入参：预览服务把已经算好的东西一次性交给 {@link ReportTemplateDataService}，
 * 避免「组装服务反过来再查一遍匹配」。
 * <p>
 * 用 @Data 类而不是 record：字段偏多，record 的构造参数会被可读性规则判为「参数过多」。
 *
 * @author <你的名字>
 */
@Data
class ReportBuildInput {

    /** 预览上下文（分析批次、报告、癌种、性别、产品基因…） */
    private PreviewContext context;

    /** 报告 + 分析行 */
    private Map<String, Object> report;

    /** LIMS 信息 */
    private InterpretationLimsVo lims;

    /** 已算好的体细胞报出位点 */
    private List<PreviewVariantVo> somaticVariants;

    /** 已算好的胚系报出位点 */
    private List<PreviewVariantVo> germlineVariants;

    /** 本次使用的模板（可为空：未绑定模板时只输出公共字段） */
    private ReportTemplateVo template;

    /** 若干白名单癌种名 → NKB disease id（圣域癌种判定用） */
    private Map<String, Long> namedDiseaseIds;

    /** 告警（会被后续组装继续追加） */
    private List<String> warnings;
}
