package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 报告预览 JSON（对齐设计书 7.6 的 ReportTemplateData 短路径契约）
 * <p>
 * 只返回、不落库、不写文件；同输入必须产生相同结果（不写时间戳/随机值）。
 *
 * @author <你的名字>
 */
@Data
public class InterpretationPreviewVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 契约版本，与设计书一致 */
    private String schemaVersion = "1.2";

    /** 模板编码 */
    private String templateCode;

    /** 模板版本 */
    private String templateVersion;

    /** 分析数据ID */
    private Long analysisId;

    /** 报告ID */
    private Long reportId;

    /** 报告基础信息 */
    private Map<String, Object> reportInfo = new LinkedHashMap<>();

    /** 样本信息（LIMS 映射，与 Tab① 同口径） */
    private Map<String, Object> sampleInfo = new LinkedHashMap<>();

    /** 体细胞变异解析（公共字段） */
    private PreviewSectionVo somaticVariants;

    /** 肿瘤遗传风险：胚系变异（公共字段） */
    private PreviewSectionVo germlineVariants;

    /** 圣域个性化体细胞列表（模板 module_code 命中才产出，否则空） */
    private List<Map<String, Object>> shengyuSomaticVariants;

    /** 圣域个性化胚系列表 */
    private List<Map<String, Object>> shengyuGermlineVariants;

    /** 质控（本页暂未实现，保留字段对齐契约） */
    private Map<String, Object> qualityControl = new LinkedHashMap<>();

    /** 非关键缺失等告警；不阻断预览 */
    private List<String> warnings;
}
