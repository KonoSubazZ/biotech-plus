package org.dromara.test.report;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.domain.vo.ReportTemplateVo;
import org.dromara.report.mapper.ReportRevisionMapper;
import org.dromara.report.service.impl.revision.ReportVersionAllocator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

@Tag("local")
class ReportVersionAllocatorTest {
    private final ReportRevisionMapper mapper = Mockito.mock(ReportRevisionMapper.class);
    private final ReportVersionAllocator allocator = new ReportVersionAllocator(mapper);

    private Map<String, Object> report() {
        return Map.of("reportId", 7L, "tenantId", "tenant-a", "subbarcode", "sample");
    }

    private ReportTemplateVo template() {
        ReportTemplateVo template = new ReportTemplateVo();
        template.setTemplateId(3L);
        template.setCustomerCode("client");
        return template;
    }

    @Test
    void generatesNextVersionAfterLastSuccessfulSend() {
        Mockito.when(mapper.lockSequence(Mockito.anyMap())).thenReturn(Map.of("id", 1L, "sentVersion", 1));
        Mockito.when(mapper.claimSequence(1L, 7L)).thenReturn(1);
        Mockito.when(mapper.startGeneration(Mockito.anyMap())).thenReturn(1);
        Map<String, Object> allocated = allocator.begin(report(), template());
        Assertions.assertEquals(2L, allocated.get("reportVersion"));
        Assertions.assertEquals("tenant-a", allocated.get("tenantId"));
        Mockito.verify(mapper, Mockito.never()).completeVersion(Mockito.anyLong());
    }

    @Test
    void sameOwnerRetriesWithoutConsumingVersion() {
        Mockito.when(mapper.lockSequence(Mockito.anyMap())).thenReturn(
            Map.of("id", 1L, "sentVersion", 0, "activeReportId", 7L));
        Mockito.when(mapper.claimSequence(1L, 7L)).thenReturn(1);
        Mockito.when(mapper.startGeneration(Mockito.anyMap())).thenReturn(1);
        Assertions.assertEquals(1L, allocator.begin(report(), template()).get("reportVersion"));
        Assertions.assertEquals(1L, allocator.begin(report(), template()).get("reportVersion"));
    }

    @Test
    void anotherInProgressReportBlocksAllocation() {
        Mockito.when(mapper.lockSequence(Mockito.anyMap())).thenReturn(
            Map.of("id", 1L, "sentVersion", 0, "activeReportId", 8L));
        Assertions.assertThrows(ServiceException.class, () -> allocator.begin(report(), template()));
        Mockito.verify(mapper, Mockito.never()).startGeneration(Mockito.anyMap());
    }

    @Test
    void databaseConflictAfterSmtpDoesNotMarkReportSent() {
        Mockito.when(mapper.completeVersion(7L)).thenReturn(0);
        Assertions.assertThrows(ServiceException.class, () -> allocator.sent(7L, 2L, true));
        Mockito.verify(mapper, Mockito.never()).markSent(Mockito.anyLong(), Mockito.anyLong());
    }

    @Test
    void templateRenameCannotChangeTheReservedBaseName() {
        Mockito.when(mapper.lockSequenceById(1L)).thenReturn(
            Map.of("activeReportId", 7L, "baseName", "client_sample_original"));
        Map<String, Object> report = Map.of("reportId", 7L, "reportSequenceId", 1L, "reportVersion", 2);
        Assertions.assertEquals("client_sample_original_V2", allocator.name(report, "client_sample_changed"));
        Mockito.verify(mapper, Mockito.never()).rememberBaseName(Mockito.anyLong(), Mockito.any());
    }

    @Test
    void longCustomNamesAreNormalizedBeforePersistence() {
        Mockito.when(mapper.lockSequenceById(1L)).thenReturn(Map.of("activeReportId", 7L));
        Mockito.when(mapper.rememberBaseName(Mockito.eq(1L), Mockito.any())).thenReturn(1);
        Map<String, Object> report = Map.of("reportId", 7L, "reportSequenceId", 1L, "reportVersion", 1);
        String name = allocator.name(report, "报告".repeat(600));
        Assertions.assertTrue(name.endsWith("_V1"));
        org.mockito.ArgumentCaptor<String> stored = org.mockito.ArgumentCaptor.forClass(String.class);
        Mockito.verify(mapper).rememberBaseName(Mockito.eq(1L), stored.capture());
        Assertions.assertTrue(stored.getValue().length() <= 80);
    }
}
