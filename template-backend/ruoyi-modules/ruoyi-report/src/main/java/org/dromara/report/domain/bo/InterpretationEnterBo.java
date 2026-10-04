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
}
