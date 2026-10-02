package org.dromara.project.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.project.domain.bo.ProductGeneBo;
import org.dromara.project.domain.bo.ProductGeneImportBo;
import org.dromara.project.domain.vo.NcbiGeneVo;
import org.dromara.project.domain.vo.ProductGeneImportResultVo;
import org.dromara.project.domain.vo.ProductGeneVo;

import java.util.List;

/**
 * 产品关联基因 业务接口
 *
 * @author <你的名字>
 */
public interface IProductGeneService {

    /**
     * 分页查询某产品（或按 symbol 过滤）的关联基因。
     *
     * @param bo        查询条件（productId 必传）
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    TableDataInfo<ProductGeneVo> selectPageList(ProductGeneBo bo, PageQuery pageQuery);

    /**
     * 统计某产品已关联的基因数（产品列表页/详情头部展示用）。
     *
     * @param productId 产品配置 id
     * @return 关联数
     */
    Long countByProductId(Long productId);

    /**
     * 批量导入关联基因。三种添加方式共用：
     * 从基因库搜索勾选（带 geneId）、粘贴 symbol 列表、上传 Excel（一列 symbol）。
     *
     * @param bo 导入参数
     * @return 导入结果（成功数 / 跳过数 / 未匹配 / 有歧义的）
     */
    ProductGeneImportResultVo importGenes(ProductGeneImportBo bo);

    /**
     * 从基因库（nkb.ncbi_gene）搜索基因，供「添加基因」选择器使用。
     *
     * @param keyword 关键词（基因符号，支持部分匹配）
     * @param limit   条数上限
     * @return 命中的基因
     */
    List<NcbiGeneVo> searchGene(String keyword, int limit);

    /**
     * 按主键批量删除关联关系（逻辑删除）。
     *
     * @param ids 主键集合
     * @return 是否成功
     */
    Boolean deleteByIds(List<Long> ids);
}
