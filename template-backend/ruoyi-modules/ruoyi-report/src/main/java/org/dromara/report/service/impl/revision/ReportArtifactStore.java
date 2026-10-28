package org.dromara.report.service.impl.revision;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.domain.dto.ReportArtifactAttempt;
import org.dromara.report.domain.vo.ReportTemplateData;
import org.dromara.report.utils.ReportArtifactPaths;
import org.dromara.report.utils.ReportNameResolver;
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

/**
 * 报告制品落盘：临时目录生成 → 成功后按 analysis_date 发布到日期目录。
 * <p>
 * 口径（2026-10-26 定）：
 * <ul>
 *   <li>目标目录 = {@code <root>/report/{json,docx,pdf,signed}/<analysis_report.analysis_date>}，已存在直接复用；</li>
 *   <li>生成过程写在 {@code <root>/.work/...} 临时目录，发布是一次次 move，失败直接丢弃临时目录、目标目录不留半成品；</li>
 *   <li>同名文件先挪到临时目录的 backup/，数据库登记失败时还原；登记成功后清理；</li>
 *   <li>审核驳回：把已登记的四类件 move 到 {@code <root>/report/rejected/<日期>/<subbarcode>_V<n>/}，并写 manifest.json。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportArtifactStore {

    /** 临时工作目录名（与制品目录分开，发布前不出现在 report/ 下） */
    private static final String WORK_DIR = ".work";

    private final ObjectMapper objectMapper;

    @Value("${report.artifact.root:/app/reports}")
    private String artifactRoot;

    /**
     * 准备一轮生成的工作目录，并按 analysis_date 计算发布目标。
     *
     * @param report   报告行（需含 analysisDate / reportId / reportGenerationNo / tenantId）
     * @param fileName 本轮文件名前缀（不含扩展名）
     * @return 本轮工作区
     */
    public ReportArtifactAttempt prepare(Map<String, Object> report, String fileName) {
        String tenant = String.valueOf(report.get("tenantId"));
        if (!tenant.matches("[A-Za-z0-9_-]+")) {
            throw new ServiceException("报告租户编号不合法");
        }
        String analysisDate = ReportArtifactPaths.requireDay(text(report.get("analysisDate")));
        int round = Integer.parseInt(report.get("reportGenerationNo").toString());
        Path work = Path.of(artifactRoot, WORK_DIR,
            tenant + "_" + report.get("reportId") + "_" + round);
        deleteQuietly(work);
        try {
            for (String stage : ReportArtifactPaths.REPORT_STAGES) {
                Files.createDirectories(work.resolve(stage));
            }
        } catch (IOException e) {
            throw new ServiceException("创建报告工作目录失败：" + e.getMessage());
        }
        return new ReportArtifactAttempt(artifactRoot, analysisDate, fileName, round, work);
    }

    /**
     * 发布：把临时目录里的阶段文件 move 到日期目录；目标已存在同名文件时先备份，供回滚。
     */
    public void publish(ReportArtifactAttempt attempt) {
        try {
            for (String stage : ReportArtifactPaths.REPORT_STAGES) {
                Path from = attempt.work().resolve(stage);
                if (!Files.isDirectory(from)) {
                    continue;
                }
                Path to = ReportArtifactPaths.reportDir(attempt.root(), stage, attempt.analysisDate());
                Files.createDirectories(to);
                for (Path file : list(from)) {
                    publishFile(attempt, stage, file, to);
                }
            }
        } catch (IOException e) {
            rollback(attempt);
            throw new ServiceException("发布报告文件失败：" + e.getMessage());
        }
    }

    /**
     * 单个阶段文件入位：目标日期目录里已有同名文件时，先把旧文件挪到 backup/（供 rollback 还原）。
     */
    private void publishFile(ReportArtifactAttempt attempt, String stage, Path file, Path to) throws IOException {
        Path target = to.resolve(file.getFileName());
        if (!Files.exists(target)) {
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        Path backup = attempt.backupDir(stage).resolve(file.getFileName());
        Files.createDirectories(backup.getParent());
        Files.move(target, backup, StandardCopyOption.REPLACE_EXISTING);
        Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 回滚：把本轮覆盖掉的旧文件还原（新产生的文件留在原地，同版本下次生成会覆盖，且数据库未登记）。
     */
    public void rollback(ReportArtifactAttempt attempt) {
        for (String stage : ReportArtifactPaths.REPORT_STAGES) {
            Path backup = attempt.backupDir(stage);
            if (!Files.isDirectory(backup)) {
                continue;
            }
            Path to = ReportArtifactPaths.reportDir(attempt.root(), stage, attempt.analysisDate());
            try {
                Files.createDirectories(to);
                for (Path file : list(backup)) {
                    Files.move(file, to.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                log.error("报告文件回滚失败，备份保留待人工恢复：{}", backup, e);
                throw new ServiceException("报告文件恢复失败：" + e.getMessage());
            }
        }
    }

    /** 发布成功后清理工作目录（含备份） */
    public void finish(ReportArtifactAttempt attempt) {
        deleteQuietly(attempt.work());
    }

    /** 生成失败：直接丢弃工作目录，不做归档、不写失败目录（数据库也不登记） */
    public void discard(ReportArtifactAttempt attempt) {
        deleteQuietly(attempt.work());
    }

    /**
     * 审核驳回：把已登记的四类制品 move 到 rejected/<analysisDate>/<subbarcode>_V<version>/，并写 manifest.json。
     * <p>
     * 只搬文件，不写数据库；调用方负责清空 analysis_report 里已失效的路径字段。
     *
     * @param report 报告行（含四个路径字段 / analysisDate / reportId / reportVersion）
     * @param reason 驳回原因
     */
    public void rejected(Map<String, Object> report, String reason) {
        String analysisDate = ReportArtifactPaths.requireDay(text(report.get("analysisDate")));
        String version = report.get("reportVersion") == null ? "legacy" : report.get("reportVersion").toString();
        // 目录用样本编号而不是 reportId：业务上按样本找人，reportId 是内部主键
        String subbarcode = ReportNameResolver.toFileName(text(report.get("subbarcode")));
        if (subbarcode.isBlank()) {
            subbarcode = "report-" + report.get("reportId");
        }
        Path target = ReportArtifactPaths.rejectedDir(artifactRoot, analysisDate)
            .resolve(subbarcode + "_V" + version);
        try {
            Files.createDirectories(target);
            moveIfExists(report, "reportJsonPath", ReportArtifactPaths.JSON, target);
            moveIfExists(report, "reportFileRawPath", ReportArtifactPaths.DOCX, target);
            moveIfExists(report, "reportFilePath", ReportArtifactPaths.PDF, target);
            moveIfExists(report, "reportSignedFilePath", ReportArtifactPaths.SIGNED, target);
            Map<String, Object> manifest = new java.util.LinkedHashMap<>();
            manifest.put("reportId", report.get("reportId"));
            manifest.put("reportVersion", report.get("reportVersion"));
            manifest.put("reportGenerationNo", report.get("reportGenerationNo"));
            manifest.put("reportName", report.get("reportName"));
            manifest.put("analysisDate", analysisDate);
            manifest.put("rejectedBy", report.get("rejectedBy"));
            manifest.put("rejectedAt", report.get("rejectedAt"));
            manifest.put("reason", reason);
            objectMapper.writeValue(target.resolve("manifest.json").toFile(), manifest);
            log.info("报告制品已移入驳回目录：{}", target);
        } catch (IOException e) {
            throw new ServiceException("审核驳回归档失败，报告状态未变更：" + e.getMessage());
        }
    }

    /** 读本轮生成的 JSON，并校验报告与版本/轮次一致 */
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

    /** 移动单个制品到驳回目录；路径为空或文件不存在时跳过 */
    private void moveIfExists(Map<String, Object> report, String key, String stage, Path target) throws IOException {
        Object value = report.get(key);
        if (value == null || value.toString().isBlank()) {
            return;
        }
        Path source = Path.of(value.toString()).toAbsolutePath().normalize();
        if (!source.startsWith(Path.of(artifactRoot).toAbsolutePath().normalize())) {
            throw new ServiceException("报告制品路径不在配置根目录内");
        }
        if (!Files.isRegularFile(source)) {
            log.warn("驳回时制品已不存在，跳过：{}", source);
            return;
        }
        Path stageDir = target.resolve(stage);
        Files.createDirectories(stageDir);
        Files.move(source, stageDir.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }

    /** 目录下的一级文件（不存在返回空） */
    private java.util.List<Path> list(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(Files::isRegularFile).toList();
        }
    }

    /** 递归删除目录，失败只记日志 */
    private void deleteQuietly(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            log.warn("清理报告工作目录失败：{}，{}", dir, e.getMessage());
        }
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
