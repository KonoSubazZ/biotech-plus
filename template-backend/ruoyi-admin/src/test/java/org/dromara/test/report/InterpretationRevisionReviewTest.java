package org.dromara.test.report;

import org.dromara.test.audit.AuditTestSupport;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.report.domain.bo.InterpretationPreviewBo;
import org.dromara.report.domain.vo.ReportTemplateData;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.mapper.ReportRevisionMapper;
import org.dromara.report.service.IInterpretationPreviewService;
import org.dromara.report.service.IReportSignService;
import org.dromara.report.service.impl.InterpretationReviewService;
import org.dromara.report.service.impl.ReportFileService;
import org.dromara.report.service.impl.preview.PreviewContextBuilder;
import org.dromara.report.service.impl.revision.ReportArtifactStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.util.HashMap;
import java.util.Map;

@Tag("local")
class InterpretationRevisionReviewTest {
    private final InterpretationMapper mapper = Mockito.mock(InterpretationMapper.class);
    private final PreviewContextBuilder context = Mockito.mock(PreviewContextBuilder.class);
    private final IInterpretationPreviewService preview = Mockito.mock(IInterpretationPreviewService.class);
    private final ReportRevisionMapper revisions = Mockito.mock(ReportRevisionMapper.class);
    private final ReportArtifactStore store = Mockito.mock(ReportArtifactStore.class);
    private final InterpretationReviewService service = new InterpretationReviewService(AuditTestSupport.recorder(),
        mapper, context, preview,
        Mockito.mock(IReportSignService.class), revisions, store, ReportTestLocks.direct(),
        Mockito.mock(ReportFileService.class));

    private InterpretationPreviewBo request(int status) {
        Map<String, Object> report = new HashMap<>();
        report.put("reportId", 7L);
        report.put("analysisId", 1L);
        report.put("status", status);
        report.put("tenantId", "tenant-a");
        report.put("reportGeneratedBy", 1L);
        report.put("reportReviewerId", 2L);
        report.put("reportFilePath", "generated.pdf");
        report.put("templateId", 3L);
        Mockito.when(context.loadReport(7L, 1L)).thenReturn(report);
        InterpretationPreviewBo bo = new InterpretationPreviewBo();
        bo.setAnalysisId(1L);
        bo.setReportId(7L);
        bo.setDisease("untrusted request parameter");
        return bo;
    }

    @Test
    void resubmitTargetsPreviousReviewerAndUsesGeneratedParameters() {
        InterpretationPreviewBo bo = request(32);
        Mockito.when(revisions.countReviewer(2L, "tenant-a")).thenReturn(1);
        Mockito.when(revisions.submitReview(7L, 2L)).thenReturn(1);
        ReportTemplateData data = new ReportTemplateData();
        ReportTemplateData.SampleInfo sample = new ReportTemplateData.SampleInfo();
        sample.setDisease("frozen disease");
        data.setSampleInfo(sample);
        Mockito.when(store.generated(Mockito.anyMap())).thenReturn(data);
        service.submit(bo);
        Mockito.verify(revisions).submitReview(7L, 2L);
        ArgumentCaptor<InterpretationPreviewBo> frozen = ArgumentCaptor.forClass(InterpretationPreviewBo.class);
        Mockito.verify(preview).freezeHistory(frozen.capture());
        Assertions.assertEquals("frozen disease", frozen.getValue().getDisease());
    }

    @Test
    void failedGenerationCannotSubmitPreviousArtifact() {
        InterpretationPreviewBo bo = request(33);
        Assertions.assertThrows(ServiceException.class, () -> service.submit(bo));
        Mockito.verifyNoInteractions(preview, revisions, store);
    }

    @Test
    void disabledReviewerBlocksSubmission() {
        InterpretationPreviewBo bo = request(32);
        Assertions.assertThrows(ServiceException.class, () -> service.submit(bo));
        Mockito.verify(revisions, Mockito.never()).submitReview(Mockito.anyLong(), Mockito.any());
    }

    @Test
    void anotherReviewerCannotApproveAssignedReport() {
        InterpretationPreviewBo bo = request(41);
        try (MockedStatic<LoginHelper> login = Mockito.mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(3L);
            Assertions.assertThrows(ServiceException.class, () -> service.approve(bo));
        }
        Mockito.verifyNoInteractions(mapper);
    }
}
