package org.dromara.test.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.domain.dto.ReportArtifactAttempt;
import org.dromara.report.service.impl.revision.ReportArtifactStore;
import org.dromara.report.utils.ReportArtifactPaths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 报告制品落盘：临时目录生成 → 按 analysis_date 发布到日期目录。
 * 只跑本类：mvn -Plocal -DskipTests=false -Dtest=ReportArtifactStoreTest test
 */
@Tag("local")
class ReportArtifactStoreTest {

    private static final String DAY = "20261027";

    @TempDir
    Path root;

    private ReportArtifactStore store() {
        ReportArtifactStore store = new ReportArtifactStore(new ObjectMapper());
        ReflectionTestUtils.setField(store, "artifactRoot", root.toString());
        return store;
    }

    private Map<String, Object> report() {
        Map<String, Object> row = new HashMap<>();
        row.put("tenantId", "test");
        row.put("reportId", 1L);
        row.put("reportVersion", 1);
        row.put("reportGenerationNo", 2);
        row.put("analysisDate", DAY);
        row.put("subbarcode", "TKHS260146718-1A");
        row.put("reportName", "样本A_报告");
        return row;
    }

    /** 把四个阶段都写进临时目录 */
    private void stageAll(ReportArtifactAttempt attempt) throws Exception {
        Files.writeString(attempt.staged(ReportArtifactPaths.JSON, ".json"), "{\"a\":1}");
        Files.writeString(attempt.staged(ReportArtifactPaths.DOCX, ".docx"), "docx");
        Files.writeString(attempt.staged(ReportArtifactPaths.PDF, ".pdf"), "pdf");
        Files.writeString(attempt.staged(ReportArtifactPaths.SIGNED, ".pdf"), "signed");
    }

    @Test
    @DisplayName("发布：落到 report/<stage>/<analysisDate>/，覆盖同名旧文件，回滚可还原")
    void publishToAnalysisDateDirAndRollback() throws Exception {
        ReportArtifactStore store = store();
        ReportArtifactAttempt attempt = store.prepare(report(), "sample_V1");

        Path oldJson = attempt.published(ReportArtifactPaths.JSON, ".json");
        Files.createDirectories(oldJson.getParent());
        Files.writeString(oldJson, "old data");
        Path untouchedSigned = attempt.published(ReportArtifactPaths.SIGNED, ".pdf");
        Files.createDirectories(untouchedSigned.getParent());
        Files.writeString(untouchedSigned, "previous signature");

        Files.writeString(attempt.staged(ReportArtifactPaths.JSON, ".json"), "new data");
        Files.writeString(attempt.staged(ReportArtifactPaths.DOCX, ".docx"), "docx");
        store.publish(attempt);

        Assertions.assertEquals("new data", Files.readString(oldJson));
        Assertions.assertEquals("docx",
            Files.readString(attempt.published(ReportArtifactPaths.DOCX, ".docx")));
        Assertions.assertEquals("previous signature", Files.readString(untouchedSigned));

        store.rollback(attempt);
        Assertions.assertEquals("old data", Files.readString(oldJson));
        store.finish(attempt);
        Assertions.assertFalse(Files.exists(attempt.work()));
    }

    @Test
    @DisplayName("生成失败：丢弃临时目录，目标日期目录不留半成品")
    void failedAttemptLeavesNoTrace() throws Exception {
        ReportArtifactStore store = store();
        ReportArtifactAttempt attempt = store.prepare(report(), "sample_V1");
        Files.writeString(attempt.staged(ReportArtifactPaths.JSON, ".json"), "partial");
        store.discard(attempt);

        Assertions.assertFalse(Files.exists(attempt.work()));
        Assertions.assertFalse(Files.exists(attempt.published(ReportArtifactPaths.JSON, ".json")));
        Assertions.assertFalse(Files.exists(root.resolve("report").resolve(ReportArtifactPaths.JSON)));
    }

    @Test
    @DisplayName("驳回：四类制品移到 rejected/<analysisDate>/report-<id>_V<version>/ 并写 manifest")
    void rejectedMovesArtifactsAndWritesManifest() throws Exception {
        ReportArtifactStore store = store();
        ReportArtifactAttempt attempt = store.prepare(report(), "sample_V1");
        stageAll(attempt);
        store.publish(attempt);
        store.finish(attempt);

        Map<String, Object> rejected = report();
        rejected.put("reportJsonPath", attempt.published(ReportArtifactPaths.JSON, ".json").toString());
        rejected.put("reportFileRawPath", attempt.published(ReportArtifactPaths.DOCX, ".docx").toString());
        rejected.put("reportFilePath", attempt.published(ReportArtifactPaths.PDF, ".pdf").toString());
        rejected.put("reportSignedFilePath", attempt.published(ReportArtifactPaths.SIGNED, ".pdf").toString());
        rejected.put("rejectedBy", 7L);
        rejected.put("rejectedAt", "2026-10-27T10:00:00");
        store.rejected(rejected, "位点不完整");

        Path target = root.resolve("report/rejected").resolve(DAY).resolve("TKHS260146718-1A_V1");
        Assertions.assertTrue(Files.isRegularFile(target.resolve("json/sample_V1.json")));
        Assertions.assertTrue(Files.isRegularFile(target.resolve("docx/sample_V1.docx")));
        Assertions.assertTrue(Files.isRegularFile(target.resolve("pdf/sample_V1.pdf")));
        Assertions.assertTrue(Files.isRegularFile(target.resolve("signed/sample_V1.pdf")));
        Assertions.assertFalse(Files.exists(attempt.published(ReportArtifactPaths.JSON, ".json")));
        String manifest = Files.readString(target.resolve("manifest.json"));
        Assertions.assertTrue(manifest.contains("位点不完整"));
        Assertions.assertTrue(manifest.contains("\"reportId\":1"));
    }

    @Test
    @DisplayName("analysis_date 非法时拒绝生成（不猜日期）")
    void invalidAnalysisDateIsRejected() {
        ReportArtifactStore store = store();
        Map<String, Object> row = report();
        row.put("analysisDate", "2026-10-27");
        Assertions.assertThrows(ServiceException.class, () -> store.prepare(row, "x"));

        row.put("analysisDate", "");
        Assertions.assertThrows(ServiceException.class, () -> store.prepare(row, "x"));
    }

    @Test
    @DisplayName("读 JSON 校验：不属于当前报告/版本/轮次时拒绝")
    void generatedJsonMustBelongToCurrentReportAndAttempt() throws Exception {
        ReportArtifactStore store = store();
        Path json = root.resolve("mismatched.json");
        Files.writeString(json, "{\"reportId\":99,\"reportInfo\":{\"reportVersion\":1,\"generationNo\":2}}");
        Map<String, Object> row = new HashMap<>(report());
        row.put("reportJsonPath", json.toString());
        Assertions.assertThrows(ServiceException.class, () -> store.generated(row));
    }
}
