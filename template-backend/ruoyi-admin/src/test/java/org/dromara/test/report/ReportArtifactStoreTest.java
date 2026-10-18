package org.dromara.test.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.report.domain.dto.ReportArtifactAttempt;
import org.dromara.report.service.impl.revision.ReportArtifactStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Tag("local")
class ReportArtifactStoreTest {
    @TempDir
    Path root;

    private ReportArtifactStore store() {
        ReportArtifactStore store = new ReportArtifactStore(new ObjectMapper());
        ReflectionTestUtils.setField(store, "artifactRoot", root.toString());
        return store;
    }

    private Map<String, Object> report() {
        return Map.of("tenantId", "test", "reportId", 1L, "reportVersion", 1, "reportGenerationNo", 2);
    }

    @Test
    void replacementRollsBackAllStagesAndPreservesSignedCopy() throws Exception {
        ReportArtifactStore store = store();
        ReportArtifactAttempt attempt = store.prepare(report(), "sample_V1");
        Files.createDirectories(attempt.current().resolve("signed"));
        Path oldSigned = attempt.published("signed", ".pdf");
        Files.writeString(oldSigned, "previous signature");
        Files.writeString(attempt.staged("json", ".json"), "new data");
        store.publish(attempt);
        Assertions.assertFalse(Files.exists(oldSigned));
        Assertions.assertEquals("new data", Files.readString(attempt.published("json", ".json")));
        store.rollback(attempt);
        Assertions.assertEquals("previous signature", Files.readString(oldSigned));
        store.failed(attempt, "database failed");
        Assertions.assertTrue(Files.isRegularFile(attempt.home().resolve("failed/2/manifest.json")));
    }

    @Test
    void failedAttemptDoesNotReplacePreviousFiles() throws Exception {
        ReportArtifactStore store = store();
        ReportArtifactAttempt attempt = store.prepare(report(), "sample_V1");
        Files.createDirectories(attempt.current());
        Files.writeString(attempt.current().resolve("evidence.txt"), "old");
        Files.writeString(attempt.staged("json", ".json"), "partial");
        store.failed(attempt, "renderer failed");
        Assertions.assertEquals("old", Files.readString(attempt.current().resolve("evidence.txt")));
        Assertions.assertEquals("partial",
            Files.readString(attempt.home().resolve("failed/2/json/sample_V1.json")));
    }

    @Test
    void generatedJsonMustBelongToCurrentReportAndAttempt() throws Exception {
        ReportArtifactStore store = store();
        Path json = root.resolve("mismatched.json");
        Files.writeString(json, "{\"reportId\":99,\"reportInfo\":{\"reportVersion\":1,\"generationNo\":2}}");
        Map<String, Object> row = new java.util.HashMap<>(report());
        row.put("reportJsonPath", json.toString());
        Assertions.assertThrows(org.dromara.common.core.exception.ServiceException.class, () -> store.generated(row));
    }
}
