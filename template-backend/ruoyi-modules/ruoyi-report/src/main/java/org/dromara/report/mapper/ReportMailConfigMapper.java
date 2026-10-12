package org.dromara.report.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.report.domain.ReportMailConfig;
import org.dromara.report.domain.vo.ReportMailConfigVo;

/**
 * 报告邮件配置 数据层
 * <p>
 * 单表增删改查一律用继承来的 BaseMapperPlus 能力，不写自定义 SQL、不写 XML。
 * 租户条件由租户拦截器自动附加（表未进共享白名单），所以这里查到的永远是本租户那一行。
 *
 * @author <你的名字>
 */
public interface ReportMailConfigMapper extends BaseMapperPlus<ReportMailConfig, ReportMailConfigVo> {
}
