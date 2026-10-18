package org.dromara.report.service.impl.revision;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.domain.dto.ReportArtifactAttempt;
import org.dromara.report.domain.vo.ReportTemplateData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/** 文件按整轮目录发布；失败归档与数据库登记失败回退均保留上一轮。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportArtifactStore {
    private final ObjectMapper objectMapper;

    @Value("${report.artifact.root:/app/reports}")
    private String artifactRoot;

    public ReportArtifactAttempt prepare(Map<String, Object> report, String fileName) {
        Path home = home(report);
        int round = Integer.parseInt(report.get("reportGenerationNo").toString());
        ReportArtifactAttempt attempt = new ReportArtifactAttempt(home, home.resolve("staging/" + round),
            home.resolve("backup/" + round), fileName, round);
        try {
            for (String stage : new String[]{"json", "docx", "pdf", "signed"}) {
                Files.createDirectories(attempt.staging().resolve(stage));
            }
            return attempt;
        } catch (IOException e) {
            throw new ServiceException("创建报告工作目录失败：" + e.getMessage());
        }
    }

    public void publish(ReportArtifactAttempt attempt) {
        try {
            if (Files.exists(attempt.current())) {
                Files.createDirectories(attempt.backup().getParent());
                Files.move(attempt.current(), attempt.backup());
            }
            Files.move(attempt.staging(), attempt.current());
        } catch (IOException e) {
            rollback(attempt);
            throw new ServiceException("发布报告文件失败：" + e.getMessage());
        }
    }

    public void rollback(ReportArtifactAttempt attempt) {
        try {
            if (Files.exists(attempt.current()) && !Files.exists(attempt.staging())) {
                Files.move(attempt.current(), attempt.staging());
            }
            if (Files.exists(attempt.backup())) {
                Files.move(attempt.backup(), attempt.current());
            }
        } catch (IOException e) {
            throw new ServiceException("报告文件恢复失败，保留工作目录待人工恢复：" + e.getMessage());
        }
    }

    public void finish(ReportArtifactAttempt attempt) {
        try (Stream<Path> paths = Files.walk(attempt.backup())) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (java.nio.file.NoSuchFileException e) {
            log.debug("首轮生成无需清理备份：{}", attempt.backup());
        } catch (IOException e) {
            log.warn("报告已登记，旧制品备份保留待清理：{}", attempt.backup(), e);
        }
    }

    public void failed(ReportArtifactAttempt attempt, String reason) {
        try {
            Path target = attempt.home().resolve("failed/" + attempt.generationNo());
            Files.createDirectories(target.getParent());
            if (Files.exists(attempt.staging())) {
                Files.move(attempt.staging(), target);
            }
            Files.createDirectories(target);
            objectMapper.writeValue(target.resolve("manifest.json").toFile(),
                Map.of("generationNo", attempt.generationNo(), "reason", String.valueOf(reason)));
        } catch (IOException e) {
            log.error("失败制品归档失败，工作目录保留：{}", attempt.staging(), e);
        }
    }

    public void rejected(Map<String, Object> report, String reason) {
        Path target = home(report).resolve("rejected/" + report.get("reportGenerationNo"));
        try {
            Files.createDirectories(target);
            copyArtifact(report, target, "reportJsonPath", "json");
            copyArtifact(report, target, "reportFileRawPath", "docx");
            copyArtifact(report, target, "reportFilePath", "pdf");
            copyArtifact(report, target, "reportSignedFilePath", "signed");
            objectMapper.writeValue(target.resolve("manifest.json").toFile(),
                Map.of("report", report, "reason", reason));
        } catch (IOException e) {
            throw new ServiceException("审核驳回归档失败，报告状态未变更：" + e.getMessage());
        }
    }

    public ReportTemplateData generated(Map<String, Object> report) {
        Object jsonPath = report.get("reportJsonPath");
        if (jsonPath == null) {
            throw new ServiceException("本轮生成的 JSON 不存在，请重新生成报告");
        }
        Path path = Path.of(jsonPath.toString()).toAbsolutePath().normalize();
        if (!path.startsWith(Path.of(artifactRoot).toAbsolutePath().normalize())) {
            throw new ServiceException("报告 JSON 不在配置根目录内");
        }
        try {
            ReportTemplateData data = objectMapper.readValue(path.toFile(), ReportTemplateData.class);
            if (report.get("reportVersion") != null
                && (!Objects.equals(Long.valueOf(report.get("reportId").toString()), data.getReportId())
                    || data.getReportInfo() == null
                    || !report.get("reportVersion").toString()
                        .equals(String.valueOf(data.getReportInfo().getReportVersion())))) {
                throw new ServiceException("报告 JSON 与当前报告及版本不一致，请重新生成");
            }
            if (report.get("reportVersion") != null && (data.getReportInfo() == null
                || !report.get("reportGenerationNo").toString()
                    .equals(String.valueOf(data.getReportInfo().getGenerationNo())))) {
                throw new ServiceException("报告 JSON 与当前生成轮次不一致，请重新生成");
            }
            return data;
        } catch (IOException e) {
            throw new ServiceException("报告 JSON 读取失败，请重新生成：" + e.getMessage());
        }
    }

    private void copyArtifact(Map<String, Object> report, Path target, String key, String stage) throws IOException {
        Object value = report.get(key);
        if (value == null || value.toString().isBlank()) {
            return;
        }
        Path source = Path.of(value.toString()).toAbsolutePath().normalize();
        if (!source.startsWith(Path.of(artifactRoot).toAbsolutePath().normalize())) {
            throw new ServiceException("报告制品路径不在配置根目录内");
        }
        Files.createDirectories(target.resolve(stage));
        Files.copy(source, target.resolve(stage).resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }

    private Path home(Map<String, Object> report) {
        String tenant = String.valueOf(report.get("tenantId"));
        if (!tenant.matches("[A-Za-z0-9_-]+")) {
            throw new ServiceException("报告租户编号不合法");
        }
        String version = report.get("reportVersion") == null ? "legacy" : report.get("reportVersion").toString();
        return Path.of(artifactRoot, "report", tenant, report.get("reportId").toString(), "V" + version);
    }
}
