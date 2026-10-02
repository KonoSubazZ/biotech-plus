package org.dromara.project.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 产品关联基因的批量导入对象。
 * <p>
 * 前端三种添加方式都收敛到这里：
 * <ol>
 *   <li>从基因库搜索勾选 → 填 {@code genes}（geneId + geneSymbol 都已确定）</li>
 *   <li>粘贴一列 symbol → 填 {@code symbols}（后端去 nkb.ncbi_gene 补全 geneId）</li>
 *   <li>上传 Excel（一列 gene_symbol）→ 后端解析成 symbols 后走第 2 种</li>
 * </ol>
 * 两者可以同时给：先处理 genes，再按 symbols 去基因库补全。
 *
 * @author <你的名字>
 */
@Data
public class ProductGeneImportBo {

    /** 产品配置 id */
    @NotNull(message = "产品不能为空")
    private Long productId;

    /** 方式 1：已经带 geneId 的条目（从基因库搜索选择） */
    private List<ProductGeneBo> genes;

    /** 方式 2：只有 symbol 的条目（粘贴文本 / Excel 上传） */
    private List<String> symbols;
}
