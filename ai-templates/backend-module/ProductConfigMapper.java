package org.dromara.biotech.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.biotech.domain.ProductConfig;
import org.dromara.biotech.domain.vo.ProductConfigVo;

/**
 * 产品配置 数据层
 * <p>
 * 单表增删改查、简单条件组合、排序、计数一律用继承来的 BaseMapperPlus 能力，
 * 不写自定义 SQL。只有复杂联表 / 子查询 / 聚合报表才在本接口声明方法，
 * 实现写在 resources/mapper/biotech/ProductConfigMapper.xml。
 *
 * @author <你的名字>
 */
public interface ProductConfigMapper extends BaseMapperPlus<ProductConfig, ProductConfigVo> {
}
