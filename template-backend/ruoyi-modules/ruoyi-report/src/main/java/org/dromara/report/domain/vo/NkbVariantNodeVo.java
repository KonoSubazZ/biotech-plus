package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 改靶候选：NKB 位点节点（供人工指定父级）
 *
 * @author <你的名字>
 */
@Data
public class NkbVariantNodeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** NKB 节点ID（gene_variant_id） */
    private Long mutationId;

    /** 基因符号 */
    private String gene;

    /** 节点名（如 V559D / Exon11 Mutation / Active Mutation） */
    private String variantName;

    /** 功能判定（激活/失活/未知/无影响） */
    private String effectText;
}
