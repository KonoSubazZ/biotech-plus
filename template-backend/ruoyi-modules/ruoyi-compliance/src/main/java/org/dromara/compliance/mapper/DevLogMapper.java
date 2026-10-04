package org.dromara.compliance.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.compliance.domain.DevLog;
import org.dromara.compliance.domain.vo.DevLogVo;

/**
 * 开发记录 数据层
 * <p>
 * 单表增删改查、简单条件组合、排序、计数一律用继承来的 BaseMapperPlus 能力，
 * 不写自定义 SQL。只有复杂联表 / 子查询 / 聚合报表才在本接口声明方法，
 * 实现写在 resources/mapper/compliance/DevLogMapper.xml。
 *
 * @author liushangzhi
 */
public interface DevLogMapper extends BaseMapperPlus<DevLog, DevLogVo> {
}
