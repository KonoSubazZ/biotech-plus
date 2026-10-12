package org.dromara.report.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 报告审核动作入参（驳回用）：定位报告 + 驳回原因。
 * <p>
 * 提交审核 / 审核通过沿用 {@link InterpretationPreviewBo}（要带生成参数）；
 * 驳回不需要生成参数，只需要原因，所以单独一个 BO。
 *
 * @author <你的名字>
 */
@Data
public class InterpretationReviewBo {

    /** 分析数据ID */
    @NotNull(message = "分析数据ID不能为空")
    private Long analysisId;

    /** 报告ID */
    @NotNull(message = "报告ID不能为空")
    private Long reportId;

    /** 驳回原因（必填） */
    private String reason;
}
