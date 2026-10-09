package org.dromara.report.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.report.domain.bo.InterpretationGermlineSignificanceBo;
import org.dromara.report.domain.bo.InterpretationPreviewBo;
import org.dromara.report.domain.bo.InterpretationTargetBo;
import org.dromara.report.domain.vo.InterpretationContextVo;
import org.dromara.report.domain.vo.InterpretationPreviewVo;
import org.dromara.report.domain.vo.NkbVariantNodeVo;
import org.dromara.report.domain.vo.PreviewVariantVo;
import org.dromara.report.domain.vo.ReportTemplateData;
import org.dromara.report.domain.vo.ReportTemplateVo;
import org.dromara.report.service.IInterpretationPreviewService;
import org.dromara.report.service.IInterpretationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.dromara.report.service.impl.PreviewSupport.asLong;
import static org.dromara.report.service.impl.PreviewSupport.asString;

/**
 * 报告预览：查数据、出 JSON（设计书 §7.6 JSON 契约 + §8 预览接口）。
 * <p>
 * 这个类只做「入口 + 组装」，具体分工都在同包的协作类里：
 * <ul>
 *   <li>{@link PreviewContextBuilder}：报告行 + LIMS 上下文 + 模板 → 预览上下文。</li>
 *   <li>{@link VariantMatcher}：体细胞/胚系位点匹配知识库 + 匹配历史冻结/复用。</li>
 *   <li>{@link VariantEditService}：人工干预（改靶、胚系临床意义、父级候选）。</li>
 *   <li>{@link ReportTemplateDataService}：公共字段 + 模板专属模块的最终组装。</li>
 * </ul>
 * 关键行为：
 * <ul>
 *   <li><b>预览优先读已生成的 JSON 制品</b>：报告生成过、制品文件在、且生成时用的模板与本次生效模板一致时，
 *       直接读那份 JSON 渲染 —— 多人进页面读同一份文件即天然共享，不用重复匹配；
 *       任一条件不满足（还没生成过 / 换了模板 / 文件被删）就实时查库重新组装。</li>
 *   <li>只查「报出（is_reported=1）」的位点；确定性：同输入产生相同 JSON，不写时间戳/随机值。</li>
 * </ul>
 *
 * @author <你的名字>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterpretationPreviewServiceImpl implements IInterpretationPreviewService {

    /** 预览来源：读已生成的 JSON 制品 */
    private static final String SOURCE_ARTIFACT = "artifact";

    /** 预览来源：实时查库重新匹配组装 */
    private static final String SOURCE_REALTIME = "realtime";

    private final IInterpretationService interpretationService;

    private final ReportTemplateDataService reportTemplateDataService;

    private final PreviewContextBuilder contextBuilder;

    private final VariantMatcher variantMatcher;

    private final VariantEditService variantEditService;

    private final ObjectMapper objectMapper;

    /** JSON/DOCX 制品根目录（与 ReportGenerationService 同一配置项） */
    @Value("${report.artifact.root:/tmp/report-artifacts}")
    private String artifactRoot;

    /**
     * 人工选模板用：报告产品对应的候选模板列表（默认模板排最前）。
     *
     * @param analysisId 分析数据ID
     * @param reportId   报告ID
     * @return 每项 {templateId, templateName, templateVersion, defaultTemplate}；产品没配模板返回空列表
     */
    @Override
    public List<Map<String, Object>> templateOptions(Long analysisId, Long reportId) {
        Map<String, Object> report = contextBuilder.loadReport(reportId, analysisId);
        List<ReportTemplateVo> options = reportTemplateDataService.optionsByProduct(
            asLong(report.get("productId")), asString(report.get("product")));
        List<Map<String, Object>> result = new ArrayList<>(options.size());
        for (int index = 0; index < options.size(); index++) {
            ReportTemplateVo option = options.get(index);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("templateId", option.getTemplateId());
            item.put("templateName", option.getTemplateName());
            item.put("templateVersion", option.getTemplateVersion());
            // 候选按「默认优先」排序，第一条就是产品默认模板
            item.put("defaultTemplate", index == 0);
            result.add(item);
        }
        return result;
    }

    /**
     * 页面预览：优先读已生成的 JSON 制品，没有才实时组装。
     * <p>
     * 事务开在这里：实时组装首次匹配会写匹配历史（insert 冻结行 + 复用留痕），
     * 必须整体提交或整体回滚（注意不能改成内部自调用 buildPreview，否则事务注解不生效）。
     *
     * @param bo 入参（analysisId / reportId / templateId：人工选模板，可空）
     * @return 预览数据 + 来源标记
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterpretationPreviewVo preview(InterpretationPreviewBo bo) {
        Map<String, Object> report = contextBuilder.loadReport(bo.getReportId(), bo.getAnalysisId());
        ReportTemplateVo template = reportTemplateDataService.requireTemplateByProduct(
            asLong(report.get("productId")), asString(report.get("product")), bo.getTemplateId());
        Optional<InterpretationPreviewVo> generated = readGeneratedArtifact(report, template);
        if (generated.isPresent()) {
            return generated.get();
        }
        InterpretationPreviewVo vo = new InterpretationPreviewVo();
        vo.setSource(SOURCE_REALTIME);
        vo.setData(buildPreview(bo));
        return vo;
    }

    /**
     * 读最近一次生成的 JSON 制品：报告生成过、制品文件在、且生成时用的模板与本次生效模板一致时才用。
     * <p>
     * 任一条不满足都返回空，由调用方走实时组装。路径必须落在制品目录内（库里存的是绝对路径，
     * 防止被改成任意文件）。
     *
     * @param report   报告行（含 reportJsonPath / reportGeneratedAt / templateId）
     * @param template 本次生效模板
     * @return 预览结果（source=artifact）；不可用时为空
     */
    private Optional<InterpretationPreviewVo> readGeneratedArtifact(Map<String, Object> report,
                                                                    ReportTemplateVo template) {
        String storedPath = asString(report.get("reportJsonPath"));
        Long generatedTemplateId = asLong(report.get("templateId"));
        if (storedPath == null || !Objects.equals(generatedTemplateId, template.getTemplateId())) {
            return Optional.empty();
        }
        Path file = Paths.get(storedPath).toAbsolutePath().normalize();
        Path root = Paths.get(artifactRoot).toAbsolutePath().normalize();
        if (!file.startsWith(root)) {
            log.warn("报告制品路径不在制品目录内，忽略：{}", storedPath);
            return Optional.empty();
        }
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            ReportTemplateData data = objectMapper.readValue(
                Files.readString(file, StandardCharsets.UTF_8), ReportTemplateData.class);
            InterpretationPreviewVo vo = new InterpretationPreviewVo();
            vo.setSource(SOURCE_ARTIFACT);
            vo.setArtifactGeneratedAt(displayTime(report.get("reportGeneratedAt")));
            vo.setData(data);
            return Optional.of(vo);
        } catch (Exception e) {
            log.warn("读取已生成报告 JSON 失败，退回实时组装：{}，{}", storedPath, e.getMessage());
            return Optional.empty();
        }
    }

    /** 制品时间给前端看：把 ISO 的 T 换成空格（2026-10-05T13:52:06 → 2026-10-05 13:52:06） */
    private String displayTime(Object value) {
        String time = asString(value);
        return time == null ? null : time.replace('T', ' ');
    }

    /**
     * 实时组装报告 JSON：上下文 → 匹配体细胞/胚系 → 交给 {@link ReportTemplateDataService} 出最终结构。
     * <p>
     * 生成（{@code ReportGenerationService}）也走这里 —— 生成时不复用页面预览的结果，
     * 而是重新组装，保证落盘的 JSON 与当时的库数据一致。
     *
     * @param bo 入参（analysisId / reportId / templateId）
     * @return 报告 JSON（ReportTemplateData）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportTemplateData buildPreview(InterpretationPreviewBo bo) {
        Map<String, Object> report = contextBuilder.loadReport(bo.getReportId(), bo.getAnalysisId());
        InterpretationContextVo context = interpretationService.loadContext(bo.getReportId(), bo.getAnalysisId());
        ReportTemplateVo template = reportTemplateDataService.requireTemplateByProduct(
            asLong(report.get("productId")), asString(report.get("product")), bo.getTemplateId());
        PreviewContext pc = contextBuilder.build(bo.getAnalysisId(), bo.getReportId(), report, context);
        contextBuilder.applyTemplate(pc, template);

        List<PreviewVariantVo> somatic = variantMatcher.matchSomatic(pc);
        List<PreviewVariantVo> germline = variantMatcher.matchGermline(pc);

        ReportBuildInput input = new ReportBuildInput();
        input.setContext(pc);
        input.setReport(report);
        input.setLims(context.getLims());
        input.setSomaticVariants(somatic);
        input.setGermlineVariants(germline);
        input.setTemplate(template);
        input.setNamedDiseaseIds(contextBuilder.namedDiseaseIds());
        input.setWarnings(collectWarnings(context, pc, somatic, germline));
        return reportTemplateDataService.build(input);
    }

    /** 预览告警：分析批次里的共性问题（不阻断预览） */
    private List<String> collectWarnings(InterpretationContextVo context, PreviewContext pc,
                                         List<PreviewVariantVo> somatic, List<PreviewVariantVo> germline) {
        List<String> warnings = new ArrayList<>(context.getWarnings());
        if (pc.getDiseaseIds().isEmpty()) {
            warnings.add("NKB 未识别到癌种「" + pc.getDisease() + "」，药物证据未按癌种过滤");
        }
        if (somatic.isEmpty() && germline.isEmpty()) {
            warnings.add("该分析批次没有「报出」的位点，请先在『筛选位点』页设置报出");
        }
        return warnings;
    }

    /** 人工确认胚系五级临床意义 → {@link VariantEditService} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGermlineSignificance(InterpretationGermlineSignificanceBo bo) {
        variantEditService.updateGermlineSignificance(bo);
    }

    /** 改靶（体细胞 / 胚系共用）→ {@link VariantEditService} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVariantTarget(InterpretationTargetBo bo) {
        variantEditService.updateVariantTarget(bo);
    }

    /** 改靶候选（NKB Approved 节点）→ {@link VariantEditService} */
    @Override
    public List<NkbVariantNodeVo> searchParentNodes(String keyword) {
        return variantEditService.searchParentNodes(keyword);
    }
}
