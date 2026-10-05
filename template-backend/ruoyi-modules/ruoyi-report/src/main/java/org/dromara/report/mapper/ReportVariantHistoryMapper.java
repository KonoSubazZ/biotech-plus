package org.dromara.report.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.report.domain.ReportVariantHistory;
import org.dromara.report.domain.vo.ReportVariantHistoryVo;

import java.util.List;

/**
 * 报告位点检出历史 数据层
 * <p>
 * 分页/条件查询用继承来的 BaseMapperPlus 能力；只有「幂等批量写入」要手写 SQL
 * （MyBatis-Plus 的 insertBatch 不带 ON DUPLICATE KEY UPDATE），实现写在
 * resources/mapper/report/ReportVariantHistoryMapper.xml。
 *
 * @author <你的名字>
 */
public interface ReportVariantHistoryMapper extends BaseMapperPlus<ReportVariantHistory, ReportVariantHistoryVo> {

    /**
     * 幂等批量写入：按 uk(tenant_id, report_id, source_type, source_id) 命中即覆盖展示字段
     * （驳回后重新提交会走这里，行数不翻倍）。
     *
     * @param rows 待写入行（非空）
     * @return 影响行数
     */
    int upsertBatch(@Param("rows") List<ReportVariantHistory> rows);
}
