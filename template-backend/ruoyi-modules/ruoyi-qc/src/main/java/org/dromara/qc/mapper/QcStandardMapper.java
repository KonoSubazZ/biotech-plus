package org.dromara.qc.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.qc.domain.QcStandard;
import org.dromara.qc.domain.vo.QcStandardVo;

/**
 * 质控标准 数据层
 * <p>
 * 单表增删改查、简单条件组合、排序、计数一律用继承来的 BaseMapperPlus 能力，
 * 不写自定义 SQL。只有复杂联表 / 子查询 / 聚合报表才在本接口声明方法，
 * 实现写在 resources/mapper/qc/qc-standardMapper.xml。
 *
 * @author <你的名字>
 */
public interface QcStandardMapper extends BaseMapperPlus<QcStandard, QcStandardVo> {
}
