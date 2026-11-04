package org.dromara.report.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.report.domain.CmsSampleChangeLog;
import org.dromara.report.domain.vo.CmsSampleChangeLogVo;

/**
 * 样本信息变更流水 数据层（cms_sample_change_log）
 * <p>
 * 只有追加与按样本编号查询两种用法，BaseMapperPlus 已够用，无手写 SQL。
 *
 * @author <你的名字>
 */
public interface CmsSampleChangeLogMapper extends BaseMapperPlus<CmsSampleChangeLog, CmsSampleChangeLogVo> {
}
