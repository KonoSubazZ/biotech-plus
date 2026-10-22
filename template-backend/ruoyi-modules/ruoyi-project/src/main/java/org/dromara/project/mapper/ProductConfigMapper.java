package org.dromara.project.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.project.domain.ProductConfig;
import org.dromara.project.domain.vo.ProductConfigVo;

/**
 * 产品配置 数据层
 * <p>
 * 单表增删改查、简单条件组合、排序、计数一律用继承来的 BaseMapperPlus 能力，
 * 不写自定义 SQL。只有复杂联表 / 子查询 / 聚合报表才在本接口声明方法，
 * 实现写在 resources/mapper/project/ProductConfigMapper.xml。
 *
 * @author <你的名字>
 */
public interface ProductConfigMapper extends BaseMapperPlus<ProductConfig, ProductConfigVo> {

    /**
     * 统计引用了该产品的质控标准条数（删除保护用，设计文档 product-config.md §3）。
     * <p>
     * 跨模块只读查询：ruoyi-project 不依赖 ruoyi-qc，所以这里走一条只读 SQL。
     * 租户条件由租户拦截器自动附加，只会统计当前租户的引用。
     *
     * @param productId 产品主键
     * @return 引用条数
     */
    Long countQcStandardByProductId(@Param("productId") Long productId);

    /**
     * 统计同产品编码的行数，<b>含逻辑删除行</b>（编码唯一校验用）。
     * <p>
     * 唯一键 uk_product_config_product_code 不含 del_flag —— 软删的产品仍占着编码。
     * 而 MyBatis-Plus 的 @TableLogic 会给普通查询自动补 del_flag='0'，看不到这些行，
     * 于是「删了再用同编码新建」会通过校验、然后在 insert 撞唯一键报 500。
     * 产品编码是业务唯一标识、不复用，所以这里手写一条不带 del_flag 条件的计数。
     *
     * @param productCode 产品编码
     * @param excludeId   要排除的主键（修改场景传自身 id，新增传 null）
     * @return 命中的行数
     */
    Long countByProductCodeIncludeDeleted(@Param("productCode") String productCode,
                                          @Param("excludeId") Long excludeId);
}
