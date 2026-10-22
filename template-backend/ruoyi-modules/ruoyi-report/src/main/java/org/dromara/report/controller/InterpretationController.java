package org.dromara.report.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
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
import org.dromara.report.domain.bo.InterpretationBioinfoBo;
import org.dromara.report.domain.bo.InterpretationSignCallbackBo;
import org.dromara.report.domain.bo.InterpretationFileQueryBo;
import org.dromara.report.domain.bo.InterpretationFileRedriveBo;
import org.dromara.report.domain.bo.InterpretationPreviewBo;
import org.dromara.report.domain.bo.InterpretationReviewBo;
import org.dromara.report.domain.bo.InterpretationSendBo;
import org.dromara.report.domain.bo.InterpretationQueryBo;
import org.dromara.report.domain.bo.InterpretationTargetBo;
import org.dromara.report.domain.bo.InterpretationVariantQueryBo;
import org.dromara.report.domain.bo.InterpretationVariantStatusBo;
import org.dromara.report.domain.vo.AnalysisReportVo;
import org.dromara.report.domain.vo.InterpretationContextVo;
import org.dromara.report.domain.vo.InterpretationPreviewVo;
import org.dromara.report.domain.vo.FileRedriveVo;
import org.dromara.report.domain.vo.InterpretationFileContentVo;
import org.dromara.report.domain.vo.InterpretationFileVo;
import org.dromara.report.domain.vo.InterpretationMailPreviewVo;
import org.dromara.report.domain.vo.InterpretationRowVo;
import org.dromara.report.domain.vo.NkbVariantNodeVo;
import org.dromara.report.domain.vo.ReportTemplateData;

import org.dromara.report.service.IInterpretationPreviewService;
import org.dromara.report.service.IInterpretationService;
import org.dromara.report.service.IReportSignService;
import org.dromara.report.service.impl.InterpretationReviewService;
import org.dromara.report.service.impl.ReportFileService;
import org.dromara.report.service.impl.ReportGenerationService;
import org.dromara.report.service.impl.ReportMailPreviewService;
import org.dromara.report.service.impl.ReportMailSendService;
import org.dromara.common.core.utils.file.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 报告解读（报告管理 › 报告解读）
 * <p>
 * 本页只提供列表与「进入解读」：
 * <ul>
 *   <li>GET /report/interpretation/list：analysis_data ⟕ 最新 analysis_report，一页一个分析批次。</li>
 *   <li>POST /report/interpretation/enter：创建/复用该批次的「解读中」报告，返回 reportId 供跳转详情页。</li>
 * </ul>
 * <p>
 * 权限口径（页签级）：详情页 6 个页签各一个权限点
 * report:interpretation:{lims|files|variants|preview|review|send}——
 * 「有页签权限 = 页签内按钮与接口都能用」。被多个页签复用的接口（context / files / preview /
 * report-file）用 SaMode.OR 放行，其余接口挂所属页签的权限点；context 另外放行 list（只有列表权限的
 * 账号进详情页至少要能看到报告头）。
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

    private final InterpretationReviewService reviewService;

    private final ReportFileService reportFileService;

    private final ReportMailPreviewService mailPreviewService;

    private final ReportMailSendService mailSendService;

    /** 报告签名通道（未对接期是假实现 ReportSignStubService） */
    private final IReportSignService reportSignService;

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
    @SaCheckPermission(
        value = {
            "report:interpretation:lims",
            "report:interpretation:list",
            "report:interpretation:files",
            "report:interpretation:variants",
            "report:interpretation:preview",
            "report:interpretation:review",
            "report:interpretation:send"
        },
        mode = SaMode.OR
    )
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
    @SaCheckPermission(value = {"report:interpretation:files", "report:interpretation:review"}, mode = SaMode.OR)
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
    @SaCheckPermission(value = {"report:interpretation:files", "report:interpretation:review"}, mode = SaMode.OR)
    @GetMapping("/file/content")
    public R<InterpretationFileContentVo> fileContent(@RequestParam Long fileId, @RequestParam Long analysisId) {
        return R.ok(interpretationService.queryFileContent(fileId, analysisId));
    }

    /**
     * 签名回调（**上上签未对接期间的占位接口**）：成功 → 50 → 60 报告待发送；失败 → 50 → 52 报告签名失败。
     * <p>
     * 接真实通道后由 {@code ISsqSignService} 的回调替代（同一个 {@code IReportSignService} 实现）。
     * 当前权限挂在「样本流转 › 报告签名」页签上（report:flow:signing），便于联调与断言测试。
     *
     * @param bo 入参（flowNo + success，失败可带 reason）
     * @return 空响应
     */
    @SaCheckPermission("report:flow:signing")
    @RepeatSubmit
    @Log(title = "报告签名-回调（未对接期占位）", businessType = BusinessType.UPDATE)
    @PostMapping("/sign/callback")
    public R<Void> signCallback(@RequestBody @Validated InterpretationSignCallbackBo bo) {
        if (Boolean.FALSE.equals(bo.getSuccess())) {
            reportSignService.handleFailure(bo.getFlowNo(), bo.getReason());
            return R.ok();
        }
        reportSignService.handleCallback(bo.getFlowNo(), bo.getSignedPdfUrl());
        return R.ok();
    }

    /**
     * Tab③ 筛选位点 - 看点完成：20 生信看点 → 21 生信审核。
     * <p>
     * 权限 = 筛选位点页签的权限（页签权限 = 页签内按钮权限，与全站口径一致）。
     *
     * @param bo 入参（analysisId / reportId）
     * @return 流转后的报告状态
     */
    @SaCheckPermission("report:interpretation:variants")
    @RepeatSubmit()
    @Log(title = "筛选位点-看点完成", businessType = BusinessType.UPDATE)
    @PostMapping("/bioinfo/view-complete")
    public R<Integer> finishBioinfoView(@RequestBody @Validated InterpretationBioinfoBo bo) {
        return R.ok(interpretationService.finishBioinfoView(bo.getAnalysisId(), bo.getReportId()));
    }

    /**
     * Tab③ 筛选位点 - 生信复核通过：21 生信审核 → 30 报告待解读。
     * <p>
     * 不能复核「自己看点」的报告（服务端按 report_bioinfo_viewed_by 校验）；
     * 权限同筛选位点页签。
     *
     * @param bo 入参（analysisId / reportId）
     * @return 流转后的报告状态（30 报告待解读）
     */
    @SaCheckPermission("report:interpretation:variants")
    @RepeatSubmit()
    @Log(title = "筛选位点-生信复核通过", businessType = BusinessType.UPDATE)
    @PostMapping("/bioinfo/review-complete")
    public R<Integer> finishBioinfoReview(@RequestBody @Validated InterpretationBioinfoBo bo) {
        return R.ok(interpretationService.finishBioinfoReview(bo.getAnalysisId(), bo.getReportId()));
    }

    /**
     * 重新驱动单个文件（「集群对接」页签的文件行按钮）
     * <p>
     * 只允许该批次内的文件；驱动在 ruoyi-drive 里同步执行：同批次重跑 ——
     * 该文件老的 analysis_file_status 行置 del_flag='1'，再插一条新行（新 file_id），返回最新状态。
     *
     * @param bo 入参（analysisId / fileId）
     * @return 文件最新状态 + 批次级驱动状态 + 处理说明
     */
    @SaCheckPermission("report:interpretation:files")
    @RepeatSubmit()
    @Log(title = "集群对接-重新驱动", businessType = BusinessType.UPDATE)
    @PostMapping("/file/redrive")
    public R<FileRedriveVo> redriveFile(@RequestBody @Validated InterpretationFileRedriveBo bo) {
        return R.ok(interpretationService.redriveFile(bo.getAnalysisId(), bo.getFileId()));
    }

    /**
     * Tab③ 筛选位点：按类型分页查询位点
     *
     * @param analysisId 分析数据ID
     * @param bo         查询条件（sourceType 必填 / gene / isReported）
     * @param pageQuery  分页参数
     * @return 分页列表
     */
    @SaCheckPermission("report:interpretation:variants")
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
    @SaCheckPermission("report:interpretation:variants")
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
    @SaCheckPermission(value = {"report:interpretation:preview", "report:interpretation:review"}, mode = SaMode.OR)
    @PostMapping("/preview")
    public R<InterpretationPreviewVo> preview(@RequestBody @Validated InterpretationPreviewBo bo) {
        return R.ok(previewService.preview(bo));
    }

    /**
     * 报告审核通过：置 43（报告审核成功）并把该报告的位点匹配结果冻结进 history_*
     * （匹配历史只在审核通过时落库，预览与生成都是只读）。
     * <p>
     * 只允许「报告待审核 / 报告审核中」（40/41）流转过来；产出人不能审核自己的报告；重复审核会直接报错。
     *
     * @param bo 入参（analysisId / reportId / templateId：人工选模板，可空）
     */
    @SaCheckPermission("report:interpretation:review")
    @Log(title = "报告解读-审核通过", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/approve")
    public R<Void> approve(@RequestBody @Validated InterpretationPreviewBo bo) {
        reviewService.approve(bo);
        return R.ok();
    }

    /**
     * 报告驳回：40/41 回退到 31（报告解读中，同一条记录），驳回原因必填，写入 comment 与回退字段。
     * <p>
     * 只有待审核能驳回；产出人不能驳回自己的报告；状态更新带来源状态条件防重复驳回。
     *
     * @param bo 入参（analysisId / reportId / reason）
     * @return 操作结果
     */
    @SaCheckPermission("report:interpretation:review")
    @Log(title = "报告解读-驳回", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/reject")
    public R<Void> reject(@RequestBody @Validated InterpretationReviewBo bo) {
        reviewService.reject(bo);
        return R.ok();
    }

    /**
     * 报告生成参数 · 解读癌种候选（NKB 疾病分类，对齐 en7 报告生成页「原发癌种」的下拉）。
     *
     * @return [{diseaseId, diseaseName}]
     */
    @SaCheckPermission("report:interpretation:preview")
    @GetMapping("/disease-options")
    public R<List<Map<String, Object>>> diseaseOptions() {
        return R.ok(previewService.diseaseOptions());
    }

    /**
     * 报告生成参数 · 解读人候选（启用中的系统用户）。
     *
     * @return [{userId, userName, nickName}]
     */
    @SaCheckPermission("report:interpretation:preview")
    @GetMapping("/analyzer-options")
    public R<List<Map<String, Object>>> analyzerOptions() {
        return R.ok(previewService.analyzerOptions());
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
    @SaCheckPermission("report:interpretation:preview")
    @Log(title = "报告生成", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/generate")
    public R<Map<String, Object>> generate(@RequestBody @Validated InterpretationPreviewBo bo) {
        return R.ok(reportGenerationService.generate(bo));
    }

    /**
     * 提交解读：冻结本次解读的位点匹配历史（history_*），再把报告状态置为 40（报告待审核）。
     * <p>
     * 入参与生成接口同一套（模板 / 解读癌种 / 报告日期 / 解读人）：冻结结果依赖癌种范围等参数，
     * 必须与生成时一致，否则冻出来的历史跟报告内容对不上。只有已生成报告才能提交。
     *
     * @param bo 入参（analysisId / reportId / 生成参数）
     * @return 操作结果
     */
    @SaCheckPermission("report:interpretation:preview")
    @RepeatSubmit()
    @Log(title = "报告解读-提交审核", businessType = BusinessType.UPDATE)
    @PostMapping("/submit")
    public R<Void> submit(@RequestBody @Validated InterpretationPreviewBo bo) {
        reviewService.submit(bo);
        return R.ok();
    }

    /**
     * 报告文件下载：把已生成的成品文件（当前是 DOCX，PDF 就绪后优先返回 PDF）写回响应。
     * <p>
     * 只读报告行里登记的路径，且必须落在固定输出目录内；直接写响应流（与合规模块的附件下载同一写法），
     * 避免走 JSON 消息转换器多出一个 {@code application/json} 头把前端下载 hooks 带偏。
     * 前端不能直接 {@code <a href>}（要带令牌），用带 Authorization 的请求取 blob；
     * 文件名从 {@code download-filename} / {@code Content-Disposition} 头取。
     *
     * @param reportId   报告ID
     * @param analysisId 分析数据ID
     * @param response   响应
     */
    @SaCheckPermission(value = {"report:interpretation:preview", "report:interpretation:review"}, mode = SaMode.OR)
    @GetMapping("/report-file")
    public void reportFile(@RequestParam Long reportId, @RequestParam Long analysisId,
                           HttpServletResponse response) throws IOException {
        File file = reportFileService.loadReportFile(reportId, analysisId);
        FileUtils.setAttachmentResponseHeader(response, file.getName());
        response.setContentType(reportFileMediaType(file.getName()).toString());
        Files.copy(file.toPath(), response.getOutputStream());
    }

    /** 按扩展名给媒体类型：PDF 就绪后同一个端点直接返回 PDF */
    private MediaType reportFileMediaType(String fileName) {
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        }
        return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    /**
     * Tab⑥ 报告发送：发送页参数（收件/抄送候选 + 渲染后的标题正文 + 附件名 + 可否发送）。
     * <p>
     * 收件候选取该报告 LIMS 的邮箱字段；标题正文用「邮件配置」的模板按本报告变量渲染成默认值，页面可改。
     *
     * @param reportId   报告ID
     * @param analysisId 分析数据ID
     * @return 发送页参数
     */
    @SaCheckPermission("report:interpretation:send")
    @GetMapping("/mail-preview")
    public R<InterpretationMailPreviewVo> mailPreview(@RequestParam Long reportId, @RequestParam Long analysisId) {
        return R.ok(mailPreviewService.preview(reportId, analysisId));
    }

    /**
     * Tab⑥ 报告发送：把已审核的报告作为附件发出，成功把报告置 SENT。
     * <p>
     * 失败也留痕（analysis_report_email_log 写 FAILED + 原因），并向上抛业务异常给页面提示。
     *
     * @param bo 入参（analysisId / reportId / to / cc / subject / content / attachReport）
     * @return {traceId, messageId, recipientCount}
     */
    @SaCheckPermission("report:interpretation:send")
    @RepeatSubmit()
    @Log(title = "报告发送", businessType = BusinessType.OTHER)
    @PostMapping("/send")
    public R<Map<String, Object>> send(@RequestBody @Validated InterpretationSendBo bo) {
        return R.ok(mailSendService.send(bo));
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
    @SaCheckPermission("report:interpretation:preview")
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
    @SaCheckPermission("report:interpretation:preview")
    @GetMapping("/parent-candidates")
    public R<List<NkbVariantNodeVo>> parentCandidates(@RequestParam("keyword") String keyword) {
        return R.ok(previewService.searchParentNodes(keyword));
    }

    @SaCheckPermission("report:interpretation:preview")
    @RepeatSubmit()
    @Log(title = "报告解读-胚系临床意义", businessType = BusinessType.UPDATE)
    @PostMapping("/germline-clinical-significance")
    public R<Void> germlineSignificance(@RequestBody @Validated InterpretationGermlineSignificanceBo bo) {
        previewService.updateGermlineSignificance(bo);
        return R.ok();
    }
}
