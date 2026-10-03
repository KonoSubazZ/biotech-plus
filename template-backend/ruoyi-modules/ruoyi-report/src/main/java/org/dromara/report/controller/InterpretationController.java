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
import org.dromara.report.domain.bo.InterpretationFileQueryBo;
import org.dromara.report.domain.bo.InterpretationQueryBo;
import org.dromara.report.domain.bo.InterpretationVariantQueryBo;
import org.dromara.report.domain.bo.InterpretationVariantStatusBo;
import org.dromara.report.domain.vo.AnalysisReportVo;
import org.dromara.report.domain.vo.InterpretationContextVo;
import org.dromara.report.domain.vo.InterpretationFileContentVo;
import org.dromara.report.domain.vo.InterpretationFileVo;
import org.dromara.report.domain.vo.InterpretationRowVo;

import java.util.Map;
import org.dromara.report.service.IInterpretationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /**
     * 解读页上下文：报告头 + LIMS 信息（Tab①）+ 生成前校验结论
     *
     * @param reportId   报告ID
     * @param analysisId 分析数据ID
     * @return 上下文
     */
    @SaCheckPermission("report:interpretation:query")
    @GetMapping("/context")
    public R<InterpretationContextVo> context(@RequestParam Long reportId, @RequestParam Long analysisId) {
        return R.ok(interpretationService.loadContext(reportId, analysisId));
    }

    /**
     * Tab② 集群对接：某分析批次的文件分页列表
     *
     * @param analysisId 分析数据ID
     * @param bo         查询条件（文件名 / 文件类型 / 状态）
     * @param pageQuery  分页参数
     * @return 分页列表
     */
    @SaCheckPermission("report:interpretation:query")
    @GetMapping("/files")
    public TableDataInfo<InterpretationFileVo> files(@RequestParam Long analysisId,
                                                    InterpretationFileQueryBo bo,
                                                    PageQuery pageQuery) {
        return interpretationService.selectFileList(analysisId, bo, pageQuery);
    }

    /**
     * Tab② 查看单个文件内容
     *
     * @param fileId     文件ID
     * @param analysisId 分析数据ID
     * @return 文件内容（超长已截断）
     */
    @SaCheckPermission("report:interpretation:query")
    @GetMapping("/file/content")
    public R<InterpretationFileContentVo> fileContent(@RequestParam Long fileId, @RequestParam Long analysisId) {
        return R.ok(interpretationService.queryFileContent(fileId, analysisId));
    }

    /**
     * Tab③ 筛选位点：按类型分页查询位点
     *
     * @param analysisId 分析数据ID
     * @param bo         查询条件（sourceType 必填 / gene / isReported）
     * @param pageQuery  分页参数
     * @return 分页列表
     */
    @SaCheckPermission("report:interpretation:query")
    @GetMapping("/variants")
    public TableDataInfo<Map<String, Object>> variants(@RequestParam Long analysisId,
                                                      InterpretationVariantQueryBo bo,
                                                      PageQuery pageQuery) {
        return interpretationService.selectVariantList(analysisId, bo, pageQuery);
    }

    /**
     * Tab③ 切换位点「入报告」状态（写共享的 file_*.is_reported）
     *
     * @param bo 入参（analysisId / sourceType / sourceId / isReported / filteredRationale）
     * @return 操作结果
     */
    @SaCheckPermission("report:interpretation:edit")
    @RepeatSubmit()
    @Log(title = "报告解读-入报告", businessType = BusinessType.UPDATE)
    @PostMapping("/variant-report-status")
    public R<Void> variantReportStatus(@RequestBody @Validated InterpretationVariantStatusBo bo) {
        interpretationService.updateVariantReportStatus(bo);
        return R.ok();
    }
}
