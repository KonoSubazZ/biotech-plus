package org.dromara.project.domain.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.project.domain.ProductGene;

/**
 * 产品关联基因视图对象 product_gene
 *
 * @author <你的名字>
 */
@Data
@NoArgsConstructor
// mapstruct-plus：生成 Vo ↔ 实体的双向转换器（查询结果 Entity → Vo 靠它）
@AutoMapper(target = ProductGene.class)
public class ProductGeneVo {

    /** 主键 */
    private Long id;

    // @fields:start
    /** 产品ID */
    private Long productId;

    /** 基因ID */
    private Integer geneId;

    /** 基因符号 */
    private String geneSymbol;

    /** 备注 */
    private String remark;
    // @fields:end

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
