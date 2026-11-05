package org.dromara.report.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 生信环节的状态推进入参（筛选位点页签两个按钮共用）：
 * 「看点完成」20 → 21、「生信复核通过」21 → 30。
 *
 * @author <你的名字>
 */
@Data
public class InterpretationBioinfoBo {

    /** 分析数据ID（校验报告确实属于该批次） */
    @NotNull(message = "分析数据ID不能为空")
    private Long analysisId;

    /** 报告ID */
    @NotNull(message = "报告ID不能为空")
    private Long reportId;
}
