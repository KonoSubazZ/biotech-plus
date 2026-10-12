package org.dromara.report.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.report.domain.AnalysisReportEmailLog;

/**
 * 报告邮件发送审计日志 Mapper（analysis_report_email_log）
 * <p>
 * 只有「插入 + 按 id 更新状态」两种动作，全部走 BaseMapperPlus，不写自定义 SQL。
 *
 * @author <你的名字>
 */
public interface AnalysisReportEmailLogMapper extends BaseMapperPlus<AnalysisReportEmailLog, AnalysisReportEmailLog> {
}
