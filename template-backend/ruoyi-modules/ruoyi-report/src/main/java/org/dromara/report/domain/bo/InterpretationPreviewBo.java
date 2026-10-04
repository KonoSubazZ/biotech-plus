package org.dromara.report.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 报告预览入参（对齐设计书 8.1 POST /admin/report/template/data）
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

    /** 模板编码；不传时取报告绑定的模板 */
    private String templateCode;
}
