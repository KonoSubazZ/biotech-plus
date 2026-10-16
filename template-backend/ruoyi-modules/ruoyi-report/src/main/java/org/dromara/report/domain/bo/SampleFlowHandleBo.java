package org.dromara.report.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 样本流转「处理」入参。
 *
 * @author <你的名字>
 */
@Data
public class SampleFlowHandleBo {

    /** 页签（决定处理动作：跳转报告解读某个页签 / 认领 / 暂无动作） */
    private String tab;

    /** 分析数据ID */
    @NotNull(message = "分析数据ID不能为空")
    private Long analysisId;
}
