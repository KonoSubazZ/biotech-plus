package org.dromara.test.report;

import org.dromara.test.audit.AuditTestSupport;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.service.impl.ReportSignStubService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.Map;

@Tag("local")
class ReportSignGenerationCallbackTest {
    private final InterpretationMapper mapper = Mockito.mock(InterpretationMapper.class);
    private final ReportSignStubService service = new ReportSignStubService(AuditTestSupport.recorder(),
        mapper, ReportTestLocks.direct());

    @Test
    void obsoleteContractNeverUpdatesNewGeneration() {
        Mockito.when(mapper.selectReportBySignFlowNo("old-contract")).thenReturn(null);
        Assertions.assertThrows(ServiceException.class, () -> service.handleCallback("old-contract", null));
        Mockito.verify(mapper, Mockito.never()).updateReportSignResultIfCurrent(Mockito.anyLong(), Mockito.any(),
            Mockito.anyInt(), Mockito.any(), Mockito.any());
    }

    @Test
    void callbackMustCompareTheContractEvenAfterLookup() {
        Mockito.when(mapper.selectReportBySignFlowNo("old-contract")).thenReturn(Map.of("reportId", 7L));
        Mockito.when(mapper.updateReportSignResultIfCurrent(7L, "old-contract", 60, null, null)).thenReturn(0);
        Assertions.assertThrows(ServiceException.class, () -> service.handleCallback("old-contract", null));
        Mockito.verify(mapper).updateReportSignResultIfCurrent(7L, "old-contract", 60, null, null);
    }
}
