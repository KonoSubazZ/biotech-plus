package org.dromara.report.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.mapper.InterpretationMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.dromara.report.service.impl.PreviewSupport.asString;

/**
 * 报告文件读取：把 {@code analysis_report} 上登记的成品文件，交给页面下载。
 * <p>
 * 安全口径：
 * <ul>
 *   <li>只认报告行里登记的路径（不接前端传路径）；</li>
 *   <li>路径必须落在报告输出固定目录（{@code report.artifact.root}，即生成时的输出目录）内，
 *       防止库里路径被改成任意文件而被读走；</li>
 *   <li>文件不存在/越界都抛业务异常（不返回 404 裸体，前端好提示）。</li>
 * </ul>
 * 取文件优先级：PDF（{@code report_file_path}）→ DOCX（{@code report_file_raw_path}）；
 * 当前生成只出 DOCX（PDF 列为空），所以现在拿到的是 DOCX。
 *
 * @author <你的名字>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportFileService {

    private final InterpretationMapper interpretationMapper;

    /** 报告输出固定目录（与 ReportGenerationService 同一配置项） */
    @Value("${report.artifact.root:/tmp/report-artifacts}")
    private String artifactRoot;

    /**
     * 取报告成品文件。
     *
     * @param reportId   报告ID
     * @param analysisId 分析数据ID（校验报告属于该批次，避免串号）
     * @return 成品文件（文件名即登记的真实文件名）
     */
    public File loadReportFile(Long reportId, Long analysisId) {
        Map<String, Object> report = interpretationMapper.selectReportRow(reportId);
        if (report == null) {
            throw new ServiceException("报告不存在或已删除：reportId=" + reportId);
        }
        if (!String.valueOf(analysisId).equals(asString(report.get("analysisId")))) {
            throw new ServiceException("报告与分析批次不匹配：reportId=" + reportId + "，analysisId=" + analysisId);
        }
        String rawPath = firstNotBlank(asString(report.get("reportFilePath")),
            asString(report.get("reportFileRawPath")));
        if (rawPath == null) {
            throw new ServiceException("报告文件尚未生成，请先「生成报告」：reportId=" + reportId);
        }
        Path root = Paths.get(artifactRoot).toAbsolutePath().normalize();
        Path file = Paths.get(rawPath).toAbsolutePath().normalize();
        if (!file.startsWith(root)) {
            log.warn("报告文件路径不在输出目录内，拒绝读取：{}", rawPath);
            throw new ServiceException("报告文件不可用：reportId=" + reportId);
        }
        if (!Files.isRegularFile(file)) {
            throw new ServiceException("报告文件不存在：reportId=" + reportId);
        }
        return file.toFile();
    }

    /** 取第一个非空白值 */
    private String firstNotBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null || second.isBlank() ? null : second;
    }
}
