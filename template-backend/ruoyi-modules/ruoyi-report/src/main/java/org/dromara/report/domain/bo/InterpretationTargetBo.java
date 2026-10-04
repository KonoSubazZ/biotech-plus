package org.dromara.report.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 「改靶 / 调整改靶」入参（体细胞与胚系共用）
 * <p>
 * 改靶 = 给位点人工指定**一个或多个 NKB 父级节点**（父级由知识库查到，见
 * {@code GET /report/interpretation/parent-candidates}），值写进源位点表的
 * {@code parent_mutation_id}（逗号分隔），从而：
 * <ul>
 *   <li>参与技术匹配键 → 产生新的历史记录（下次预览走继承逻辑）；</li>
 *   <li>追加进匹配的节点集合 → 证据来自人工指定的父级（en7 `parent_mutID` 口径）。</li>
 * </ul>
 * 传空列表表示取消改靶。
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

    /** 报告ID（改靶要按癌种范围校验「改靶后是否有药物证据」） */
    @NotNull(message = "报告ID不能为空")
    private Long reportId;

    /** 源位点表（SNP_INDEL / CNV / FUSION / CR_ALL） */
    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    /** 位点ID（对应源位点表的主键） */
    @NotNull(message = "位点ID不能为空")
    private Long sourceId;

    /** 人工父级节点ID列表（NKB gene_variant_id）；空 = 取消改靶 */
    private List<Long> parentMutationIds;
}
