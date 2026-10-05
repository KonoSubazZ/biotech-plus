package org.dromara.report.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.report.domain.bo.ReportVariantHistoryBo;
import org.dromara.report.domain.vo.ReportVariantHistoryExcelRow;
import org.dromara.report.domain.vo.ReportVariantHistoryVo;

import java.util.List;

/**
 * 报告位点检出历史 业务接口（只读：写入只在「提交审核」链路里发生）
 *
 * @author <你的名字>
 */
public interface IReportVariantHistoryService {

    /**
     * 分页查询检出历史
     *
     * @param bo        查询条件（样本编号 / 解读癌种 / 产品 / 客户 + params 里的报告时间范围）
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    TableDataInfo<ReportVariantHistoryVo> selectPageList(ReportVariantHistoryBo bo, PageQuery pageQuery);

    /**
     * 查询导出用的行（同一套查询条件，不分页）
     *
     * @param bo 查询条件（与列表页一致）
     * @return 导出行（用药信息按分组展开成 8 列）
     */
    List<ReportVariantHistoryExcelRow> selectExcelList(ReportVariantHistoryBo bo);
}
