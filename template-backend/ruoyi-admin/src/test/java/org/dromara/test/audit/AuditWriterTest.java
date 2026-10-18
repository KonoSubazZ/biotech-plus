package org.dromara.test.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.audit.domain.DataAudit;
import org.dromara.audit.domain.AuditChange;
import org.dromara.audit.domain.dto.AuditCapture;
import org.dromara.audit.domain.dto.AuditSource;
import org.dromara.audit.domain.dto.AuditTarget;
import org.dromara.audit.mapper.AuditChangeMapper;
import org.dromara.audit.mapper.AuditSourceMapper;
import org.dromara.audit.mapper.DataAuditMapper;
import org.dromara.audit.service.impl.AuditDiff;
import org.dromara.audit.service.impl.AuditWriter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("local")
class AuditWriterTest {
    private final AuditSourceMapper source = mock(AuditSourceMapper.class);
    private final DataAuditMapper events = mock(DataAuditMapper.class);
    private final AuditChangeMapper changes = mock(AuditChangeMapper.class);
    private final AuditWriter writer = new AuditWriter(source, events, changes, new AuditDiff(), new ObjectMapper());

    private AuditCapture capture() {
        AuditCapture capture = new AuditCapture();
        capture.setAction("业务修改");
        capture.setOperationId("operation");
        capture.setActorId("SYSTEM");
        capture.setActorName("系统");
        capture.setSource("SERVICE");
        capture.setOccurredAt(new Date());
        capture.setUnrestricted(true);
        return capture;
    }

    private void after(Map<AuditTarget, List<Map<String, Object>>> values) {
        when(source.snapshot(any(), nullable(String.class), anyBoolean()))
            .thenAnswer(invocation -> values.getOrDefault(invocation.getArgument(0), List.of()));
    }

    @Test
    void userAndRoleMembershipChangesShareOneEvent() {
        List<AuditTarget> targets = AuditTarget.users(List.of(7L));
        AuditCapture capture = capture();
        capture.getBefore().put(targets.get(0), List.of(Map.of("user_id", 7L, "tenant_id", "a", "nick_name", "old")));
        capture.getBefore().put(targets.get(1), List.of(Map.of("user_id", 7L, "role_id", 3L, "tenant_id", "a")));
        after(Map.of(targets.get(0), List.of(Map.of("user_id", 7L, "tenant_id", "a", "nick_name", "new")),
            targets.get(1), List.of(Map.of("user_id", 7L, "role_id", 4L, "tenant_id", "a"))));
        writer.write(capture);
        ArgumentCaptor<DataAudit> event = ArgumentCaptor.forClass(DataAudit.class);
        verify(events).insert(event.capture());
        assertEquals("a", event.getValue().getTenantId());
        assertEquals(3, event.getValue().getChangeCount());
        assertNull(event.getValue().getSubbarcode());
    }

    @Test
    void commonSampleEventHasNoInventedAnalysisIdentity() {
        AuditTarget target = AuditTarget.rows(AuditSource.SAMPLE, "id", 1);
        AuditCapture capture = capture();
        capture.getBefore().put(target, List.of());
        after(Map.of(target, List.of(Map.of("id", 1L, "tenant_id", "a", "subbarcode", "S001"))));
        writer.write(capture);
        ArgumentCaptor<DataAudit> event = ArgumentCaptor.forClass(DataAudit.class);
        verify(events).insert(event.capture());
        assertEquals("S001", event.getValue().getSubbarcode());
        assertEquals("SAMPLE", event.getValue().getAuditScope());
        assertNull(event.getValue().getAnalysisId());
    }

    @Test
    void oldReportsDoNotCreateExtraNoChangeEvents() {
        List<AuditTarget> targets = AuditTarget.analysis(9L);
        AuditCapture capture = capture();
        Map<String, Object> analysis = Map.of("analysis_id", 9L, "tenant_id", "a", "subbarcode", "S001");
        Map<String, Object> old = Map.of("report_id", 10L, "analysis_id", 9L, "tenant_id", "a", "subbarcode", "S001");
        capture.getBefore().put(targets.get(0), List.of(analysis));
        capture.getBefore().put(targets.get(1), List.of(old));
        Map<String, Object> next = Map.of("report_id", 11L, "analysis_id", 9L, "tenant_id", "a", "subbarcode", "S001");
        after(Map.of(targets.get(0), List.of(analysis), targets.get(1), List.of(old, next)));
        writer.write(capture);
        ArgumentCaptor<DataAudit> event = ArgumentCaptor.forClass(DataAudit.class);
        verify(events, times(1)).insert(event.capture());
        assertEquals(11L, event.getValue().getReportId());
        assertEquals(1, event.getValue().getChangeCount());
    }

    @Test
    void clearedSqlNullIsPresentInSnapshotsAndFieldDifference() throws Exception {
        AuditTarget target = AuditTarget.rows(AuditSource.SAMPLE, "id", 1L);
        AuditCapture capture = capture();
        capture.getBefore().put(target, List.of(Map.of("id", 1L, "tenant_id", "a",
            "subbarcode", "S001", "patient_name", "old")));
        // MyBatis omits map entries for SQL NULL by default.
        after(Map.of(target, List.of(Map.of("id", 1L, "tenant_id", "a", "subbarcode", "S001"))));
        writer.write(capture);
        ArgumentCaptor<AuditChange> change = ArgumentCaptor.forClass(AuditChange.class);
        verify(changes).insert(change.capture());
        ObjectMapper json = new ObjectMapper();
        var saved = json.readTree(change.getValue().getAfterJson());
        assertTrue(saved.has("patient_name"));
        assertTrue(saved.get("patient_name").isNull());
        var difference = json.readTree(change.getValue().getChangedFieldsJson()).get(0);
        assertEquals("old", difference.get("before").asText());
        assertTrue(difference.has("after") && difference.get("after").isNull());
    }

    @Test
    void reportAndSampleVersionSequenceShareOneEvent() {
        AuditTarget report = AuditTarget.rows(AuditSource.REPORT, "report_id", 10L);
        AuditTarget sequence = AuditTarget.rows(AuditSource.SEQUENCE, "report_id", 10L);
        AuditCapture capture = capture();
        capture.getBefore().put(report, List.of(Map.of("report_id", 10L, "analysis_id", 9L,
            "subbarcode", "S001", "tenant_id", "a", "report_version", 1)));
        capture.getBefore().put(sequence, List.of(Map.of("id", 2L, "subbarcode", "S001",
            "tenant_id", "a", "last_version", 1)));
        after(Map.of(report, List.of(Map.of("report_id", 10L, "analysis_id", 9L,
            "subbarcode", "S001", "tenant_id", "a", "report_version", 2)),
            sequence, List.of(Map.of("id", 2L, "subbarcode", "S001", "tenant_id", "a", "last_version", 2))));
        writer.write(capture);
        ArgumentCaptor<DataAudit> event = ArgumentCaptor.forClass(DataAudit.class);
        verify(events).insert(event.capture());
        assertEquals(10L, event.getValue().getReportId());
        assertEquals(9L, event.getValue().getAnalysisId());
        assertEquals(2, event.getValue().getChangeCount());
    }

    @Test
    void externalProcessOutcomeDoesNotInventRowChanges() {
        AuditTarget target = AuditTarget.rows(AuditSource.ANALYSIS, "analysis_id", 9L);
        AuditCapture capture = capture();
        capture.setOutcome("FAILED");
        Map<String, Object> row = Map.of("analysis_id", 9L, "subbarcode", "S001", "tenant_id", "a");
        capture.getBefore().put(target, List.of(row));
        after(Map.of(target, List.of(row)));
        writer.write(capture);
        ArgumentCaptor<DataAudit> event = ArgumentCaptor.forClass(DataAudit.class);
        verify(events).insert(event.capture());
        assertEquals("FAILED", event.getValue().getOutcome());
        assertEquals(0, event.getValue().getChangeCount());
    }

    @Test
    void identicalQcBatchRetainsEverySampleImportFact() {
        AuditTarget first = AuditTarget.rows(AuditSource.WET_QC, "subbarcode", "S001");
        AuditTarget second = AuditTarget.rows(AuditSource.WET_QC, "subbarcode", "S002");
        Map<String, Object> row = Map.of("id", 1L, "tenant_id", "a", "subbarcode", "S001");
        Map<String, Object> other = Map.of("id", 2L, "tenant_id", "a", "subbarcode", "S002");
        AuditCapture capture = capture();
        capture.getBefore().put(first, List.of(row));
        capture.getBefore().put(second, List.of(other));
        after(Map.of(first, List.of(row), second, List.of(other)));
        writer.write(capture);
        ArgumentCaptor<DataAudit> event = ArgumentCaptor.forClass(DataAudit.class);
        verify(events, times(2)).insert(event.capture());
        assertEquals(List.of("S001", "S002"), event.getAllValues().stream().map(DataAudit::getSubbarcode).toList());
        assertTrue(event.getAllValues().stream().allMatch(value -> "NO_CHANGE".equals(value.getOutcome())));
    }

    @Test
    void tenantStatusChangeBelongsToTargetTenantWithoutSampleIdentity() throws Exception {
        AuditTarget target = AuditTarget.rows(AuditSource.TENANT, "id", 8L);
        AuditCapture capture = capture();
        capture.getBefore().put(target, List.of(Map.of("id", 8L, "tenant_id", "company_a",
            "tenant_name", "药企A", "status", "0")));
        after(Map.of(target, List.of(Map.of("id", 8L, "tenant_id", "company_a",
            "tenant_name", "药企A", "status", "1"))));
        writer.write(capture);
        ArgumentCaptor<DataAudit> event = ArgumentCaptor.forClass(DataAudit.class);
        verify(events).insert(event.capture());
        assertEquals("company_a", event.getValue().getTenantId());
        assertEquals("NON_SAMPLE", event.getValue().getAuditScope());
        assertNull(event.getValue().getSubbarcode());
        ArgumentCaptor<AuditChange> change = ArgumentCaptor.forClass(AuditChange.class);
        verify(changes).insert(change.capture());
        var field = new ObjectMapper().readTree(change.getValue().getChangedFieldsJson()).get(0);
        assertEquals("sys_tenant", change.getValue().getTableName());
        assertEquals("status", field.get("field").asText());
        assertEquals("0", field.get("before").asText());
        assertEquals("1", field.get("after").asText());
    }
}
