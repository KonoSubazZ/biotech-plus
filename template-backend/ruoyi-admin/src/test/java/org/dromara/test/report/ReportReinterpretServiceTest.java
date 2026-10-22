package org.dromara.test.report;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.drive.domain.vo.DriveSampleVo;
import org.dromara.drive.service.IDriveService;
import org.dromara.report.domain.AnalysisReport;
import org.dromara.report.domain.bo.InterpretationReinterpretBo;
import org.dromara.report.domain.vo.AnalysisReportVo;
import org.dromara.report.domain.vo.AnalysisSnapshotVo;
import org.dromara.report.mapper.AnalysisReportMapper;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.mapper.ReportRevisionMapper;
import org.dromara.report.mapper.ReportTemplateMapper;
import org.dromara.report.mapper.SampleInfoMapper;
import org.dromara.report.service.IInterpretationService;
import org.dromara.report.service.impl.InterpretationServiceImpl;
import org.dromara.report.service.impl.ReportReinterpretService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

/**
 * 重新解读编排：驱动在 ruoyi-drive（这里 mock），只验证「先驱动出新区批次 → 再建下一轮报告」的顺序与拦截条件。
 */
@Tag("local")
class ReportReinterpretServiceTest {
    private final InterpretationMapper mapper = Mockito.mock(InterpretationMapper.class);
    private final AnalysisReportMapper reports = Mockito.mock(AnalysisReportMapper.class);
    private final ReportRevisionMapper revisions = Mockito.mock(ReportRevisionMapper.class);
    private final IInterpretationService interpretations = Mockito.mock(IInterpretationService.class);
    private final IDriveService driveService = Mockito.mock(IDriveService.class);
    private final ReportReinterpretService service = new ReportReinterpretService(mapper, reports, revisions,
        interpretations, driveService, ReportTestLocks.direct());

    private InterpretationReinterpretBo bo() {
        InterpretationReinterpretBo bo = new InterpretationReinterpretBo();
        bo.setAnalysisId(1L);
        bo.setReportId(7L);
        Mockito.when(revisions.selectChild(7L)).thenReturn(null);
        Mockito.when(mapper.selectReportRow(7L)).thenReturn(Map.of("analysisId", 1L, "status", 62,
            "tenantId", "a", "analysisDate", "20261018", "product", "product", "subbarcode", "sample",
            "analysisBatchDate", "20261018000000", "productBio", "product"));
        return bo;
    }

    private DriveSampleVo drive(String status, int fileCount, Long analysisId, Integer driveStatus) {
        DriveSampleVo vo = new DriveSampleVo();
        vo.setStatus(status);
        vo.setFileCount(fileCount);
        vo.setAnalysisId(analysisId);
        vo.setDriveStatus(driveStatus);
        vo.setMessage("mock");
        return vo;
    }

    @Test
    void failedDriveNeverCreatesReport() {
        InterpretationReinterpretBo bo = bo();
        Mockito.when(driveService.driveSample(Mockito.any())).thenReturn(drive("failed", 0, 2L, 10));
        Assertions.assertThrows(ServiceException.class, () -> service.reinterpret(bo));
        Mockito.verifyNoInteractions(interpretations);
    }

    @Test
    void retryReturnsAlreadyCreatedRecordWithoutRescan() {
        InterpretationReinterpretBo bo = bo();
        Mockito.when(revisions.selectChild(7L)).thenReturn(8L);
        AnalysisReportVo child = new AnalysisReportVo();
        child.setReportId(8L);
        Mockito.when(reports.selectVoById(8L)).thenReturn(child);
        Assertions.assertEquals(8L, service.reinterpret(bo).getReportId());
        Mockito.verifyNoInteractions(driveService);
    }

    @Test
    void zeroFilesNeverCreatesReport() {
        InterpretationReinterpretBo bo = bo();
        Mockito.when(driveService.driveSample(Mockito.any())).thenReturn(drive("success", 0, 2L, 11));
        Assertions.assertThrows(ServiceException.class, () -> service.reinterpret(bo));
        Mockito.verifyNoInteractions(interpretations);
    }

    @Test
    void successfulRescanCreatesTheLinkedRecordUnderTheNewBatch() {
        InterpretationReinterpretBo bo = bo();
        Mockito.when(driveService.driveSample(Mockito.any())).thenReturn(drive("success", 2, 2L, 11));
        Mockito.when(revisions.countFiles(2L)).thenReturn(2);
        AnalysisReportVo child = new AnalysisReportVo();
        child.setReportId(8L);
        child.setSourceReportId(7L);
        child.setReportStatus(20);
        Mockito.when(interpretations.enterAfterRescan(2L, 7L)).thenReturn(child);
        Assertions.assertSame(child, service.reinterpret(bo));
        // 新批次 analysisId=2（不是来源批次 1）
        Mockito.verify(interpretations).enterAfterRescan(2L, 7L);
    }

    @Test
    void newRecordInheritsTheSourceNonDefaultTemplate() {
        AnalysisReport source = new AnalysisReport();
        source.setReportId(7L);
        source.setReportStatus(62);
        source.setTemplateId(3L);
        source.setTemplate("source-non-default");
        Mockito.when(reports.selectOne(Mockito.any())).thenReturn(source);
        AnalysisSnapshotVo snapshot = new AnalysisSnapshotVo();
        snapshot.setAnalysisId(1L);
        snapshot.setProductId(4L);
        snapshot.setSubbarcode("sample");
        Mockito.when(mapper.selectAnalysisSnapshot(1L)).thenReturn(snapshot);
        Mockito.when(reports.insert(Mockito.any(AnalysisReport.class))).thenAnswer(invocation -> {
            AnalysisReport created = invocation.getArgument(0);
            created.setReportId(8L);
            Assertions.assertEquals(3L, created.getTemplateId());
            Assertions.assertEquals("source-non-default", created.getTemplate());
            Assertions.assertEquals(7L, created.getSourceReportId());
            Assertions.assertEquals(20, created.getReportStatus());
            return 1;
        });
        Mockito.when(reports.selectVoById(8L)).thenReturn(new AnalysisReportVo());
        InterpretationServiceImpl entry = new InterpretationServiceImpl(mapper, reports,
            Mockito.mock(SampleInfoMapper.class), Mockito.mock(ReportTemplateMapper.class), driveService,
            ReportTestLocks.direct());
        Assertions.assertNotNull(entry.enterAfterRescan(1L, 7L));
        Mockito.verify(reports).insert(Mockito.any(AnalysisReport.class));
    }
}
