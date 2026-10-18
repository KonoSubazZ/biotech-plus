package org.dromara.test.report;

import org.dromara.test.audit.AuditTestSupport;

import cn.dev33.satoken.stp.StpUtil;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.domain.bo.SampleFlowHandleBo;
import org.dromara.report.mapper.SampleFlowMapper;
import org.dromara.report.service.IInterpretationService;
import org.dromara.report.service.IReportSignService;
import org.dromara.report.service.impl.SampleFlowServiceImpl;
import org.dromara.report.service.impl.revision.ReportOperationLock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.util.Map;

@Tag("local")
class SampleFlowReviewClaimTest {
    @Test
    void claimRereadsStateInsideTheSharedReportLock() {
        SampleFlowMapper mapper = Mockito.mock(SampleFlowMapper.class);
        ReportOperationLock lock = ReportTestLocks.direct();
        SampleFlowServiceImpl service = new SampleFlowServiceImpl(AuditTestSupport.recorder(), mapper, lock,
            Mockito.mock(IInterpretationService.class), Mockito.mock(IReportSignService.class));
        Mockito.when(mapper.selectLatestReport(1L)).thenReturn(
            Map.of("reportId", 7L, "reportStatus", 40),
            Map.of("reportId", 7L, "reportStatus", 41, "reportReviewClaimedBy", 2L));
        SampleFlowHandleBo bo = new SampleFlowHandleBo();
        bo.setAnalysisId(1L);
        bo.setTab("pending-review");
        try (MockedStatic<StpUtil> permissions = Mockito.mockStatic(StpUtil.class)) {
            Assertions.assertThrows(ServiceException.class, () -> service.handle(bo));
        }
        Mockito.verify(lock).call(Mockito.eq(7L), Mockito.any());
        Mockito.verify(mapper, Mockito.never()).claimReview(Mockito.anyLong(), Mockito.anyLong());
    }
}
