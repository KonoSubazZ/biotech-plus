package org.dromara.report.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 报告预览/生成入参（对齐设计书 8.1 POST /admin/report/template/data）
 * <p>
 * 模板不在入参里传：由报告的产品经 product_template 决定
 * （见 ReportTemplateDataService.requireTemplateByProduct）。
 *
 * @author <你的名字>
 */
@Data
public class InterpretationPreviewBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分析数据ID */
    @NotNull(message = "分析数据ID不能为空")
    private Long analysisId;

    /** 报告ID */
    @NotNull(message = "报告ID不能为空")
    private Long reportId;
}
