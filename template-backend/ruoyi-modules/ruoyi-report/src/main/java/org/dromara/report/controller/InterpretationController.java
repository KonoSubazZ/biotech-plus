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
import org.dromara.report.domain.bo.InterpretationGermlineSignificanceBo;
import org.dromara.report.domain.bo.InterpretationFileQueryBo;
import org.dromara.report.domain.bo.InterpretationPreviewBo;
import org.dromara.report.domain.bo.InterpretationQueryBo;
import org.dromara.report.domain.bo.InterpretationTargetBo;
import org.dromara.report.domain.bo.InterpretationVariantQueryBo;
import org.dromara.report.domain.bo.InterpretationVariantStatusBo;
import org.dromara.report.domain.vo.AnalysisReportVo;
import org.dromara.report.domain.vo.InterpretationContextVo;
import org.dromara.report.domain.vo.InterpretationPreviewVo;
import org.dromara.report.domain.vo.InterpretationFileContentVo;
import org.dromara.report.domain.vo.InterpretationFileVo;
import org.dromara.report.domain.vo.InterpretationRowVo;
import org.dromara.report.domain.vo.NkbVariantNodeVo;
import org.dromara.report.domain.vo.ReportTemplateData;

import org.dromara.report.service.IInterpretationPreviewService;
import org.dromara.report.service.IInterpretationService;
import org.dromara.report.service.impl.ReportGenerationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import java.util.Map;

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

    private final IInterpretationPreviewService previewService;

    private final ReportGenerationService reportGenerationService;

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

    /**
     * Tab④ 报告预览：报告已生成过且生成时用的模板与本次一致 → 直接读那份已落库的 JSON 渲染
     * （多人进页面读同一份，不用重复匹配）；否则实时查库组装。
     * <p>
     * 只返回、不落库、不写文件；返回体的 data 就是生成时那份 JSON 契约，前端直接按它渲染。
     *
     * @param bo 入参（analysisId / reportId / templateId：人工选模板，可空）
     * @return 预览数据 + 来源标记（artifact / realtime）
     */
    @SaCheckPermission("report:interpretation:preview")
    @PostMapping("/preview")
    public R<InterpretationPreviewVo> preview(@RequestBody @Validated InterpretationPreviewBo bo) {
        return R.ok(previewService.preview(bo));
    }

    /**
     * 人工选模板：报告产品对应的候选模板列表（默认模板排最前）。
     * <p>
     * 一个产品可能配多个模板，前端用它在「报告预览/生成」处给下拉，选中后把 templateId 传回预览/生成接口。
     *
     * @param analysisId 分析数据ID
     * @param reportId   报告ID
     * @return [{templateId, templateName, templateVersion, defaultTemplate}]
     */
    @SaCheckPermission("report:interpretation:preview")
    @GetMapping("/template-options")
    public R<List<Map<String, Object>>> templateOptions(@RequestParam Long analysisId, @RequestParam Long reportId) {
        return R.ok(previewService.templateOptions(analysisId, reportId));
    }

    /**
     * 正式生成：重新组装 JSON → 写 JSON 制品 → 按「产品 → 模板 → 固定目录/<模板名>.docx」渲染 DOCX
     * → 登记 analysis_report 的模板、两条制品路径与报告名。不接受前端回传的预览 JSON。
     *
     * @param bo 入参（analysisId / reportId）
     * @return 含 reportId / templateCode / reportName / jsonPath / docxPath / 渲染信息的结果
     */
    @SaCheckPermission("report:interpretation:generate")
    @Log(title = "报告生成", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/generate")
    public R<Map<String, Object>> generate(@RequestBody @Validated InterpretationPreviewBo bo) {
        return R.ok(reportGenerationService.generate(bo));
    }

    /**
     * 人工确认胚系五级临床意义（保存后重新预览即可看到按 Class 匹配的证据）
     *
     * @param bo 入参（analysisId / reportId / sourceId / clinicalSignificance 1~5）
     * @return 操作结果
     */
    /**
     * 改靶（体细胞 / 胚系共用）：给位点人工指定一个或多个 NKB 父级节点
     *
     * @param bo 入参（analysisId / reportId / sourceType / sourceId / parentMutationIds，空列表 = 取消改靶）
     * @return 操作结果
     */
    @SaCheckPermission("report:interpretation:edit")
    @RepeatSubmit()
    @Log(title = "报告解读-改靶", businessType = BusinessType.UPDATE)
    @PostMapping("/variant-target")
    public R<Void> variantTarget(@RequestBody @Validated InterpretationTargetBo bo) {
        previewService.updateVariantTarget(bo);
        return R.ok();
    }

    /**
     * 改靶候选：按关键词查 NKB 位点节点（父级由知识库查询到）
     *
     * @param keyword 关键词（基因符号 / 节点名片段）
     * @return 候选节点列表
     */
    @SaCheckPermission("report:interpretation:edit")
    @GetMapping("/parent-candidates")
    public R<List<NkbVariantNodeVo>> parentCandidates(@RequestParam("keyword") String keyword) {
        return R.ok(previewService.searchParentNodes(keyword));
    }

    @SaCheckPermission("report:interpretation:edit")
    @RepeatSubmit()
    @Log(title = "报告解读-胚系临床意义", businessType = BusinessType.UPDATE)
    @PostMapping("/germline-clinical-significance")
    public R<Void> germlineSignificance(@RequestBody @Validated InterpretationGermlineSignificanceBo bo) {
        previewService.updateGermlineSignificance(bo);
        return R.ok();
    }
}
