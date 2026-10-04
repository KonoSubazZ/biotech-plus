package org.dromara.report.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.report.domain.bo.InterpretationPreviewBo;
import org.dromara.report.domain.vo.ReportTemplateData;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.service.IInterpretationPreviewService;
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
 * 报告正式生成（设计书 §9.1；当前阶段<b>只出 final JSON</b>，不渲染 DOCX/PDF）。
 * <p>
 * 流程：重新组装一次 JSON（不信前端回传的预览）→ 写受控制品目录的新文件（原子落盘）→
 * 把 analysis_report 的 template_id / template / report_json_path / 产出人与时间更新到最新制品。
 * <p>
 * 每次生成都写新文件，旧文件不原地修改；因此库里只指向「最新一次」的路径。
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

    private final ObjectMapper objectMapper;

    /** JSON 制品根目录（可在 application.yml 里用 report.artifact.root 覆盖） */
    @Value("${report.artifact.root:/tmp/report-artifacts}")
    private String artifactRoot;

    /**
     * 生成 final JSON 并登记路径。
     *
     * @param bo 入参（analysisId / reportId / templateCode）
     * @return 含 reportId / jsonPath / fileName / templateCode 的结果
     */
    public Map<String, Object> generate(InterpretationPreviewBo bo) {
        ReportTemplateData data = previewService.buildPreview(bo);
        Path jsonPath = writeJson(bo, data);
        updateArtifact(bo, data, jsonPath);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reportId", bo.getReportId());
        result.put("templateCode", data.getTemplateCode());
        result.put("fileName", jsonPath.getFileName().toString());
        result.put("jsonPath", jsonPath.toString());
        return result;
    }

    /** 原子写 JSON：先写同目录临时文件，再 ATOMIC_MOVE 到目标文件名 */
    private Path writeJson(InterpretationPreviewBo bo, ReportTemplateData data) {
        Path target = resolveTarget(bo.getReportId(), data.getTemplateCode());
        try {
            Files.createDirectories(target.getParent());
            Path temp = target.resolveSibling(target.getFileName() + ".tmp");
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            Files.write(temp, json.getBytes(StandardCharsets.UTF_8));
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return target;
        } catch (IOException e) {
            throw new ServiceException("报告 JSON 写入失败：" + e.getMessage());
        }
    }

    /** 制品路径：&lt;root&gt;/&lt;reportId&gt;/report-&lt;reportId&gt;-&lt;templateCode&gt;-&lt;时间&gt;-&lt;随机串&gt;.json */
    private Path resolveTarget(Long reportId, String templateCode) {
        String reportPart = reportId == null ? "unknown" : String.valueOf(reportId);
        String templatePart = safeFilePart(templateCode);
        String fileName = "report-" + reportPart + "-" + templatePart + "-" + LocalDateTime.now().format(FILE_TIME)
            + "-" + UUID.randomUUID().toString().substring(0, 8) + ".json";
        return Paths.get(artifactRoot, reportPart, fileName);
    }

    /** 登记到 analysis_report（只更新路径与模板，不改报告状态） */
    private void updateArtifact(InterpretationPreviewBo bo, ReportTemplateData data, Path jsonPath) {
        String actor = LoginHelper.getUserId() == null ? null : String.valueOf(LoginHelper.getUserId());
        int affected = interpretationMapper.updateReportJsonArtifact(bo.getReportId(), data.getTemplateId(),
            data.getTemplateCode(), jsonPath.toString(), actor);
        if (affected == 0) {
            throw new ServiceException("报告不存在或已删除，无法登记 JSON 制品：reportId=" + bo.getReportId());
        }
        log.info("报告 JSON 已生成：reportId={} templateCode={} path={}", bo.getReportId(), data.getTemplateCode(), jsonPath);
    }

    /** 文件名里只保留安全字符（模板编码可能为空） */
    private String safeFilePart(String value) {
        if (value == null || value.isBlank()) {
            return "no-template";
        }
        return value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
