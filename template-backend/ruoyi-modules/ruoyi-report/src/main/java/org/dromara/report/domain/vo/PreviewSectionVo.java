package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 报告预览：一个位点分节（体细胞 / 胚系），= JSON 契约里的 { summary, items }
 *
 * @author <你的名字>
 */
@Data
public class PreviewSectionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 汇总：报出数 / 命中数 / 未命中数 / 复用历史数 */
    private Map<String, Object> summary = new LinkedHashMap<>();

    /** 位点明细 */
    private List<PreviewVariantVo> items;

    // -------- 以下为对齐参考工程 ReportTemplateData 的富对象字段（模板可直接用） --------

    /** 分析批次ID */
    private Long analysisId;

    /** 报告ID */
    private Long reportId;

    /** 解读癌种在 NKB 的 ID */
    private Long diseaseId;

    /** 解读癌种 */
    private String diseaseName;

    /** 性别 */
    private String gender;

    /** 匹配时间（本仓为「同输入同 JSON」不写时间，固定 null，保留字段对齐契约） */
    private String matchedAt;
}
