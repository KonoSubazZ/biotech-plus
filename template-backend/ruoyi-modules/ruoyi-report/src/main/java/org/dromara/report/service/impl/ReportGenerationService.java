package org.dromara.report.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.report.domain.bo.InterpretationPreviewBo;
import org.dromara.report.domain.vo.ReportTemplateData;
import org.dromara.report.domain.vo.ReportTemplateVo;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.service.IInterpretationPreviewService;
import org.dromara.report.service.impl.ReportDocxRenderer;
import org.dromara.report.utils.ArtifactPermissions;
import org.dromara.report.utils.ReportArtifactPaths;
import org.dromara.report.utils.ReportNameResolver;
import org.dromara.report.utils.ReportStatus;
import org.dromara.report.service.impl.ReportNameVariables;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import org.dromara.report.service.template.ReportTemplateDataService;
import org.dromara.report.domain.dto.ReportArtifactAttempt;
import org.dromara.report.service.impl.revision.ReportArtifactStore;
import org.dromara.report.service.impl.revision.ReportOperationLock;
import org.dromara.report.service.impl.revision.ReportRevisionPolicy;
import org.dromara.report.service.impl.revision.ReportVersionAllocator;
import org.springframework.transaction.support.TransactionTemplate;

/** 报告生成：预留版本、临时渲染、整轮发布与事务登记；失败保留原制品并归档。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private final ReportArtifactStore artifactStore;
    private final ReportOperationLock operationLock;
    private final ReportVersionAllocator versionAllocator;
    private final TransactionTemplate transactions;


    private final IInterpretationPreviewService previewService;

    private final InterpretationMapper interpretationMapper;

    private final ReportTemplateDataService templateDataService;

    private final ReportNameVariables reportNameVariables;

    private final ReportDocxRenderer reportDocxRenderer;

    private final OnlyOfficePdfConverter pdfConverter;

    private final ObjectMapper objectMapper;


    /**
     * 生成完整 JSON/DOCX/PDF 后登记；任一生成步骤失败均归档并保留旧制品。
     *
     * @param bo 入参（analysisId / reportId / templateCode）
     * @return 含报告名、JSON/DOCX 路径、渲染结果的结果集
     */
    public Map<String, Object> generate(InterpretationPreviewBo bo) {
        return operationLock.call(bo.getReportId(), () -> generateLocked(bo));
    }

    private Map<String, Object> generateLocked(InterpretationPreviewBo bo) {
        Map<String, Object> report = interpretationMapper.selectReportRow(bo.getReportId());
        assertGeneratable(bo, report);
        ReportTemplateVo template = resolveTemplate(bo, report);
        bo.setTemplateId(template.getTemplateId());
        versionAllocator.begin(report, template);
        report = interpretationMapper.selectReportRow(bo.getReportId());
        ReportArtifactAttempt attempt = null;
        boolean published = false;
        try {
            attempt = artifactStore.prepare(report, "report-" + bo.getReportId());
            ReportTemplateData data = previewService.buildPreview(bo);
            int version = Integer.parseInt(report.get("reportVersion").toString());
            String reportName = versionAllocator.name(report, resolveReportName(template, data, report));
            applyReportName(data, reportName);
            data.getReportInfo().setReportVersion(version);
            data.getReportInfo().setGenerationNo(attempt.generationNo());
            attempt = artifactStore.prepare(report, reportName);
            Path jsonPath = attempt.staged(ReportArtifactPaths.JSON, ".json");
            Path docxPath = attempt.staged(ReportArtifactPaths.DOCX, ".docx");
            writeJson(jsonPath, data);
            ArtifactPermissions.relax(jsonPath);
            Path templateFile = reportDocxRenderer.locateTemplate(template);
            reportDocxRenderer.render(templateFile, jsonPath, docxPath);
            ArtifactPermissions.relax(docxPath);
            pdfConverter.convert(docxPath, attempt.work().resolve(ReportArtifactPaths.PDF));
            artifactStore.publish(attempt);
            published = true;
            registerPublished(bo, data, attempt, reportName);
            artifactStore.finish(attempt);
            return buildResult(data, reportName, attempt.published(ReportArtifactPaths.JSON, ".json"),
                attempt.published(ReportArtifactPaths.DOCX, ".docx"), Map.of("reportVersion", version,
                    "generationNo", attempt.generationNo()));
        } catch (RuntimeException e) {
            if (published) {
                artifactStore.rollback(attempt);
            }
            // 生成失败不归档、不写失败目录：只丢弃临时工作目录，目标日期目录不留半成品
            if (attempt != null) {
                artifactStore.discard(attempt);
            }
            markGenerateFailed(bo.getReportId(), e.getMessage());
            throw e;
        }
    }

    private void registerPublished(InterpretationPreviewBo bo, ReportTemplateData data,
                                   ReportArtifactAttempt attempt, String reportName) {
        transactions.executeWithoutResult(status -> {
            updateArtifact(bo, data, attempt.published(ReportArtifactPaths.JSON, ".json"),
                attempt.published(ReportArtifactPaths.DOCX, ".docx"),
                attempt.published(ReportArtifactPaths.PDF, ".pdf"), reportName);
            markGenerated(bo.getReportId());
        });
    }

    /** 生成前校验报告归属与状态；签名失败允许重生成，已发送必须走重新解读。 */
    private void assertGeneratable(InterpretationPreviewBo bo, Map<String, Object> reportRow) {
        if (reportRow == null) {
            throw new ServiceException("报告不存在或已删除：reportId=" + bo.getReportId());
        }
        if (!String.valueOf(bo.getAnalysisId()).equals(String.valueOf(reportRow.get("analysisId")))) {
            throw new ServiceException("报告与分析批次不匹配");
        }
        int status = Integer.parseInt(String.valueOf(reportRow.get("status")));
        ReportRevisionPolicy.requireGeneratable(status);
    }

    /** 生成成功：状态转 32，并清空上一次的失败原因 */
    private void markGenerated(Long reportId) {
        interpretationMapper.updateReportGenerated(reportId, ReportStatus.GENERATED, null);
    }

    /** 生成失败：状态落 33 并记原因（下次生成成功会自动清掉） */
    private void markGenerateFailed(Long reportId, String reason) {
        interpretationMapper.updateReportGenerated(reportId, ReportStatus.GENERATE_FAILED, reason);
        log.warn("报告生成失败：reportId={} 原因={}", reportId, reason);
    }

    /**
     * 模板解析：报告产品 → product_template → report_template；
     * 请求里带了 templateId 就用人工选的那条（必须属于该产品），否则用产品默认模板。
     */
    private ReportTemplateVo resolveTemplate(InterpretationPreviewBo bo, Map<String, Object> reportRow) {
        Long templateId = bo.getTemplateId();
        if (templateId == null) {
            templateId = toLong(reportRow.get("templateId"));
        }
        return templateDataService.requireTemplateByProduct(
            toLong(reportRow == null ? null : reportRow.get("productId")),
            reportRow == null ? null : text(reportRow.get("product")),
            templateId);
    }

    /**
     * 报告名 = report_template.report_name 渲染结果；模板没配时用默认拼接规则
     * （{@link ReportNameResolver#DEFAULT_PATTERN}）。变量取值见 {@link ReportNameVariables}。
     */
    private String resolveReportName(ReportTemplateVo template, ReportTemplateData data,
                                     Map<String, Object> reportRow) {
        String pattern = template.getReportName();
        if (pattern == null || pattern.isBlank()) {
            log.info("模板未配报告命名，用默认拼接规则：{}", ReportNameResolver.DEFAULT_PATTERN);
        }
        return reportNameVariables.render(pattern, reportNameVariables.build(template, data, reportRow));
    }

    /** 报告名回填进 JSON：DOCX 封面/页眉可直接引用，不用各自再算一遍 */
    private void applyReportName(ReportTemplateData data, String reportName) {
        if (data.getReportInfo() != null) {
            data.getReportInfo().setReportName(reportName);
        }
    }

    /** 原子写 JSON：先写同目录临时文件，再 ATOMIC_MOVE 到目标文件名 */
    private void writeJson(Path target, ReportTemplateData data) {
        try {
            Files.createDirectories(target.getParent());
            Path temp = target.resolveSibling(target.getFileName() + ".tmp");
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            Files.write(temp, json.getBytes(StandardCharsets.UTF_8));
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new ServiceException("报告 JSON 写入失败：" + e.getMessage());
        }
    }

    /** 登记到 analysis_report（路径/模板/报告名 + 回填解读癌种；不改报告状态） */
    private void updateArtifact(InterpretationPreviewBo bo, ReportTemplateData data,
                                Path jsonPath, Path docxPath, Path pdfPath, String reportName) {
        String actor = LoginHelper.getUserId() == null ? null : String.valueOf(LoginHelper.getUserId());
        int affected = interpretationMapper.updateReportArtifact(bo.getReportId(), data.getTemplateId(),
            data.getTemplateCode(), jsonPath.toString(), docxPath.toString(),
            pdfPath == null ? null : pdfPath.toString(), reportName, actor);
        if (affected == 0) {
            throw new ServiceException("报告不存在或已删除，无法登记制品：reportId=" + bo.getReportId());
        }
        // 回填解读癌种：预览页可覆盖癌种，落库的是本次生效的那个（与报告 JSON 的 sampleInfo.disease 同源）
        interpretationMapper.updateReportDisease(bo.getReportId(), effectiveDisease(data), effectiveDiseaseId(data));
        log.info("报告已生成：reportId={} templateCode={} reportName={} disease={} json={} docx={} pdf={}",
            bo.getReportId(), data.getTemplateCode(), reportName, effectiveDisease(data), jsonPath, docxPath, pdfPath);
    }

    /** 本次生效的解读癌种（报告 JSON 的 sampleInfo.disease） */
    private String effectiveDisease(ReportTemplateData data) {
        return data.getSampleInfo() == null ? null : data.getSampleInfo().getDisease();
    }

    /** 本次生效的解读癌种ID（报告 JSON 位点分节的 diseaseId，NKB disease_id；无分节时为空） */
    private Long effectiveDiseaseId(ReportTemplateData data) {
        return data.getSomaticVariants() == null ? null : data.getSomaticVariants().getDiseaseId();
    }

    /** 返回给前端：保留既有键（fileName/jsonPath 指 JSON），新增 DOCX 与渲染信息 */
    private Map<String, Object> buildResult(ReportTemplateData data, String reportName, Path jsonPath,
                                            Path docxPath, Map<String, Object> rendered) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reportId", data.getReportId());
        result.put("templateCode", data.getTemplateCode());
        result.put("reportName", reportName);
        result.put("fileName", jsonPath.getFileName().toString());
        result.put("jsonPath", jsonPath.toString());
        result.put("docxFileName", docxPath.getFileName().toString());
        result.put("docxPath", docxPath.toString());
        result.putAll(rendered);
        return result;
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }
}
