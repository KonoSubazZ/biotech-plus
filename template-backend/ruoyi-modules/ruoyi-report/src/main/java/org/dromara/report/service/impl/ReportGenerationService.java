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
import org.dromara.report.service.ReportDocxRenderer;
import org.dromara.report.service.ReportNameResolver;
import org.dromara.report.service.ReportNameVariables;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 报告正式生成（设计书 §9.1）：重新组装 JSON → 解析报告名 → 写 JSON → 调 Python 渲染 DOCX → 登记制品。
 * <p>
 * 几个刻意的口径：
 * <ul>
 *   <li>不信前端回传的预览结果，每次都用 {@code buildPreview} 重新查库组装；</li>
 *   <li>每次生成写新文件（文件名带时间 + 随机串），旧制品不原地覆盖，库里只指向最新一次；</li>
 *   <li>失败时删掉本次刚写的 JSON/DOCX，不留半成品；</li>
 *   <li>报告名（report_template.report_name 渲染结果）同时进 JSON（reportInfo.reportName）与库
 *       （analysis_report.report_name），DOCX 文件名与 JSON 同前缀，三处同源。</li>
 * </ul>
 *
 * @author <你的名字>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    /** 文件名里的时间戳格式（与制品命名约定一致） */
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT);

    private final IInterpretationPreviewService previewService;

    private final InterpretationMapper interpretationMapper;

    private final ReportTemplateDataService templateDataService;

    private final ReportNameVariables reportNameVariables;

    private final ReportDocxRenderer reportDocxRenderer;

    private final ObjectMapper objectMapper;

    /** JSON/DOCX 制品根目录（可在 application.yml 里用 report.artifact.root 覆盖） */
    @Value("${report.artifact.root:/tmp/report-artifacts}")
    private String artifactRoot;

    /**
     * 生成报告：JSON 与 DOCX 各一份，并登记到 analysis_report。
     *
     * @param bo 入参（analysisId / reportId / templateCode）
     * @return 含报告名、JSON/DOCX 路径、渲染结果的结果集
     */
    public Map<String, Object> generate(InterpretationPreviewBo bo) {
        ReportTemplateData data = previewService.buildPreview(bo);
        Map<String, Object> reportRow = interpretationMapper.selectReportRow(bo.getReportId());
        ReportTemplateVo template = resolveTemplate(data, reportRow);
        String reportName = resolveReportName(template, data, reportRow);
        applyReportName(data, reportName);

        Path jsonPath = jsonTarget(bo.getReportId(), data.getTemplateCode(), reportName);
        Path docxPath = jsonPath.resolveSibling(fileBase(jsonPath) + ".docx");
        try {
            writeJson(jsonPath, data);
            Map<String, Object> rendered = reportDocxRenderer.render(template.getTemplatePath(), jsonPath, docxPath);
            updateArtifact(bo, data, jsonPath, docxPath, reportName);
            return buildResult(data, reportName, jsonPath, docxPath, rendered);
        } catch (RuntimeException e) {
            deleteQuietly(jsonPath);
            deleteQuietly(docxPath);
            throw e;
        }
    }

    /** 模板解析：入参编码 → 报告已绑定模板 → 产品默认；都没有 = 配置错误，禁止生成 */
    private ReportTemplateVo resolveTemplate(ReportTemplateData data, Map<String, Object> reportRow) {
        ReportTemplateVo template = templateDataService.resolveTemplate(
            data.getTemplateCode(),
            data.getTemplateId(),
            toLong(reportRow == null ? null : reportRow.get("productId")),
            reportRow == null ? null : text(reportRow.get("product")));
        if (template == null) {
            throw new ServiceException("报告模板未配置，无法生成报告：reportId=" + data.getReportId());
        }
        return template;
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

    /** 登记到 analysis_report（只更新路径/模板/报告名，不改报告状态） */
    private void updateArtifact(InterpretationPreviewBo bo, ReportTemplateData data,
                                Path jsonPath, Path docxPath, String reportName) {
        String actor = LoginHelper.getUserId() == null ? null : String.valueOf(LoginHelper.getUserId());
        int affected = interpretationMapper.updateReportArtifact(bo.getReportId(), data.getTemplateId(),
            data.getTemplateCode(), jsonPath.toString(), docxPath.toString(), reportName, actor);
        if (affected == 0) {
            throw new ServiceException("报告不存在或已删除，无法登记制品：reportId=" + bo.getReportId());
        }
        log.info("报告已生成：reportId={} templateCode={} reportName={} json={} docx={}",
            bo.getReportId(), data.getTemplateCode(), reportName, jsonPath, docxPath);
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

    /** 制品路径：&lt;root&gt;/&lt;reportId&gt;/&lt;报告名或默认前缀&gt;-&lt;时间&gt;-&lt;随机串&gt;.json */
    private Path jsonTarget(Long reportId, String templateCode, String reportName) {
        String reportPart = reportId == null ? "unknown" : String.valueOf(reportId);
        return Paths.get(artifactRoot, reportPart, fileBase(reportName, reportPart, templateCode) + ".json");
    }

    /** 文件名前缀：配了报告名就用安全化后的报告名，否则用默认的 report-&lt;id&gt;-&lt;模板编码&gt; */
    private String fileBase(String reportName, String reportPart, String templateCode) {
        if (reportName != null && !reportName.isBlank()) {
            return ReportNameResolver.toFileName(reportName) + "-" + uniqueSuffix();
        }
        return "report-" + reportPart + "-" + safeFilePart(templateCode) + "-" + uniqueSuffix();
    }

    /** 去掉扩展名取文件前缀（DOCX 与 JSON 同前缀，设计书 §9.1） */
    private String fileBase(Path jsonPath) {
        String fileName = jsonPath.getFileName().toString();
        return fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - ".json".length()) : fileName;
    }

    /** 唯一后缀：时间 + 随机串，保证「每次生成写新文件、旧制品不原地覆盖」 */
    private String uniqueSuffix() {
        return LocalDateTime.now().format(FILE_TIME) + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** 文件名里只保留安全字符（模板编码可能为空） */
    private String safeFilePart(String value) {
        if (value == null || value.isBlank()) {
            return "no-template";
        }
        return value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /** 失败清理：删不掉只记日志，不掩盖原始异常 */
    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("生成失败后清理制品未成功：{}，{}", path, e.getMessage());
        }
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
