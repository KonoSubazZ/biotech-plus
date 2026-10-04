package org.dromara.report.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 人工确认胚系五级临床意义（设计书 §8：保存前要求该位点已经通过预览建立历史记录）
 *
 * @author <你的名字>
 */
@Data
public class InterpretationGermlineSignificanceBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分析数据ID */
    @NotNull(message = "分析数据ID不能为空")
    private Long analysisId;

    /** 报告ID */
    @NotNull(message = "报告ID不能为空")
    private Long reportId;

    /** 胚系位点ID（file_CR_ALL.id） */
    @NotNull(message = "位点ID不能为空")
    private Long sourceId;

    /** 临床意义：1致病 / 2可能致病 / 3未知临床意义 / 4可能良性 / 5良性 */
    @NotNull(message = "临床意义不能为空")
    private Integer clinicalSignificance;
}
