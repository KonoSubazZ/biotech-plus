package org.dromara.report.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 胚系「改靶 / 调整改靶」入参
 * <p>
 * 改靶更新 {@code file_CR_ALL.parent_mutation_id}，从而产生新的技术匹配键；
 * 新历史记录会继承同一客户/产品/癌种/性别/位点/合子状态下最近一条临床意义（设计书 0.3 / 4.8）。
 *
 * @author <你的名字>
 */
@Data
public class InterpretationTargetBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分析数据ID */
    @NotNull(message = "分析数据ID不能为空")
    private Long analysisId;

    /** 胚系位点ID（file_CR_ALL.id） */
    @NotNull(message = "位点ID不能为空")
    private Long sourceId;

    /** 人工父级（NKB mutation id）；传 null 表示取消改靶 */
    private Long parentMutationId;
}
