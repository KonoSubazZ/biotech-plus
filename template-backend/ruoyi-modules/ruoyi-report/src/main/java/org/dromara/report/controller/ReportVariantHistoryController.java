package org.dromara.report.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.report.domain.bo.ReportVariantHistoryBo;
import org.dromara.report.domain.vo.ReportVariantHistoryExcelRow;
import org.dromara.report.domain.vo.ReportVariantHistoryVo;
import org.dromara.report.service.IReportVariantHistoryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 历史检出（报告管理 › 历史检出）
 * <p>
 * 只读列表 + 导出：数据在「提交审核」时由提交链路写入（{@code ReportVariantHistoryWriter}），页面不提供增删改。
 *
 * @author <你的名字>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/report/variantHistory")
public class ReportVariantHistoryController extends BaseController {

    private final IReportVariantHistoryService reportVariantHistoryService;

    /**
     * 分页查询历史检出
     *
     * @param bo        查询条件（样本编号 / 解读癌种 / 产品 / 客户 + params 里的报告时间范围）
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    @SaCheckPermission("report:variantHistory:list")
    @GetMapping("/list")
    public TableDataInfo<ReportVariantHistoryVo> list(ReportVariantHistoryBo bo, PageQuery pageQuery) {
        return reportVariantHistoryService.selectPageList(bo, pageQuery);
    }

    /**
     * 导出历史检出（xlsx）：查询条件与列表页同一套，导出当前筛选下的全部行（不分页）。
     * <p>
     * 列与页面一致（报告ID 页面已隐藏），用药信息按 8 个分组各占一列。
     *
     * @param bo       查询条件（与列表页一致）
     * @param response 响应体
     */
    @SaCheckPermission("report:variantHistory:export")
    @Log(title = "历史检出", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ReportVariantHistoryBo bo, HttpServletResponse response) {
        List<ReportVariantHistoryExcelRow> rows = reportVariantHistoryService.selectExcelList(bo);
        ExcelUtil.exportExcel(rows, "历史检出", ReportVariantHistoryExcelRow.class, response);
    }
}
