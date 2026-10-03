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
}
