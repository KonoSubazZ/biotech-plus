package org.dromara.report.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.report.domain.bo.InterpretationEnterBo;
import org.dromara.report.domain.bo.InterpretationQueryBo;
import org.dromara.report.domain.vo.AnalysisReportVo;
import org.dromara.report.domain.vo.InterpretationRowVo;
import org.dromara.report.service.IInterpretationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报告解读（报告管理 › 报告解读）
 * <p>
 * 本页只提供列表与「进入解读」：
 * <ul>
 *   <li>GET /report/interpretation/list：analysis_data ⟕ 最新 analysis_report，一页一个分析批次。</li>
 *   <li>POST /report/interpretation/enter：创建/复用该批次的「解读中」报告，返回 reportId 供跳转详情页。</li>
 * </ul>
 * 位点筛选 / 预览 / 审核 / 发送在后续页面按 permission 点位逐个开放。
 *
 * @author <你的名字>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/report/interpretation")
public class InterpretationController extends BaseController {

    private final IInterpretationService interpretationService;

    /**
     * 分页查询报告解读列表
     *
     * @param bo        查询条件（样本编号 / 产品 / 报告状态 + params 里的分析日期范围）
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    @SaCheckPermission("report:interpretation:list")
    @GetMapping("/list")
    public TableDataInfo<InterpretationRowVo> list(InterpretationQueryBo bo, PageQuery pageQuery) {
        return interpretationService.selectPageList(bo, pageQuery);
    }

    /**
     * 进入解读：复用该批次「解读中」的报告，没有则新建一条并返回 reportId
     *
     * @param bo 入参（analysisId）
     * @return 报告头信息
     */
    @SaCheckPermission("report:interpretation:enter")
    @RepeatSubmit()
    @Log(title = "报告解读", businessType = BusinessType.INSERT)
    @PostMapping("/enter")
    public R<AnalysisReportVo> enter(@RequestBody @Validated InterpretationEnterBo bo) {
        return R.ok(interpretationService.enter(bo.getAnalysisId()));
    }
}
