package org.dromara.report.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 「进入解读」入参
 *
 * @author <你的名字>
 */
@Data
public class InterpretationEnterBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分析数据ID（列表行上的 analysisId） */
    @NotNull(message = "分析数据ID不能为空")
    private Long analysisId;

    /**
     * 只读查看：已完成（62）的报告「查看解读记录」用。
     * true = 不新建、不补快照、不校验流转状态，直接返回该批次已有报告。
     */
    private Boolean readonly;
}
