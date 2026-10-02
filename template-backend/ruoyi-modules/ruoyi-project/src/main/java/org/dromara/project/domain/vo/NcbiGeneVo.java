package org.dromara.project.domain.vo;

import lombok.Data;

/**
 * 基因库（nkb.ncbi_gene）的搜索结果。
 * <p>
 * 只读、不落库：只服务于「添加基因」时的选择器。字段按需精简 ——
 * geneId/geneSymbol 是必须的，description/synonyms 用来帮人工确认没选错基因
 * （symbol 在基因库里会重复，比如 TRNAN-GUU 对应 19 个 gene_id，得让用户看得见区别）。
 *
 * @author <你的名字>
 */
@Data
public class NcbiGeneVo {

    /** 基因 id（nkb.ncbi_gene.gene_id） */
    private Integer geneId;

    /** 基因符号 */
    private String geneSymbol;

    /** 基因描述，便于人工确认 */
    private String description;

    /** 别名（竖线分隔），便于人工确认 */
    private String synonyms;
}
