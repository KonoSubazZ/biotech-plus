package org.dromara.project.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.project.domain.ProductGene;

/**
 * 产品关联基因业务对象 product_gene
 * <p>
 * 一条记录 = 某个产品关联了某个基因。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ProductGene.class, reverseConvertGenerate = false)
public class ProductGeneBo extends BaseEntity {

    /** 主键 */
    private Long id;

    /** 产品配置 id（product_config.id） */
    @NotNull(message = "产品不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long productId;

    /** 基因 id（nkb.ncbi_gene.gene_id） */
    @NotNull(message = "基因不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer geneId;

    /** 基因符号（冗余自 nkb.ncbi_gene，展示时不必跨库 join） */
    @NotBlank(message = "基因符号不能为空", groups = AddGroup.class)
    @Size(max = 24, message = "基因符号长度不能超过 24")
    private String geneSymbol;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
