package org.dromara.project.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.project.domain.ProductGene;
import org.dromara.project.domain.vo.NcbiGeneVo;
import org.dromara.project.domain.vo.ProductGeneVo;

import java.util.Collection;
import java.util.List;

/**
 * 产品关联基因 数据层
 * <p>
 * 单表增删改查、计数一律用继承来的 BaseMapperPlus；只有跨库读基因库（nkb.ncbi_gene）
 * 和带条件的聚合才在这里声明，实现写在 resources/mapper/project/ProductGeneMapper.xml。
 * <p>
 * 注意：nkb.ncbi_gene 属于另一个库、且没有 tenant_id 列，
 * 已在 tenant.excludes 里忽略租户拦截（见 application.yml），否则 SQL 会被加上租户条件而报错。
 *
 * @author <你的名字>
 */
public interface ProductGeneMapper extends BaseMapperPlus<ProductGene, ProductGeneVo> {

    /**
     * 从基因库模糊搜索基因，供「添加基因」的选择器使用（只读、不落库）。
     * <p>
     * 限定人类（tax_id=9606）；精确匹配的排在前面，然后是短 symbol（更可能是用户想找的主符号）。
     *
     * @param keyword 基因符号关键词（支持部分匹配）
     * @param limit   返回条数上限
     * @return 基因列表
     */
    List<NcbiGeneVo> searchGeneFromNcbi(@Param("keyword") String keyword, @Param("limit") int limit);

    /**
     * 按 gene_symbol 批量取基因，用于「粘贴 / Excel 导入」时补全 geneId。
     * <p>
     * 同一个 symbol 可能对应多个 gene_id（基因库里 tRNA 类基因会重名），
     * 所以返回的是列表，由 Service 决定怎么处理歧义。
     *
     * @param symbols 基因符号集合
     * @return 命中的基因（可能一个 symbol 对应多条）
     */
    List<NcbiGeneVo> selectGenesBySymbolsFromNcbi(@Param("symbols") Collection<String> symbols);

    /**
     * 统计某产品已关联的（未逻辑删除的）基因数。
     *
     * @param productId 产品配置 id
     * @return 关联数
     */
    Long countByProductId(@Param("productId") Long productId);
}
