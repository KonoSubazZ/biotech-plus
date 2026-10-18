package org.dromara.report.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.audit.service.impl.AuditRecorder;
import org.dromara.audit.domain.dto.AuditTarget;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.TenantContext;
import org.dromara.report.domain.bo.InterpretationReinterpretBo;
import org.dromara.report.domain.dto.DriverInvokeResult;
import org.dromara.report.domain.vo.AnalysisReportVo;
import org.dromara.report.mapper.AnalysisReportMapper;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.mapper.ReportRevisionMapper;
import org.dromara.report.service.IInterpretationService;
import org.dromara.report.service.impl.revision.ReportOperationLock;
import org.springframework.stereotype.Service;

import java.util.Map;

/** 已发送报告重新扫描后创建下一轮记录；外部扫描不置于数据库事务内。 */
@Service
@RequiredArgsConstructor
public class ReportReinterpretService {
    private final AuditRecorder auditRecorder;
    private final InterpretationMapper interpretationMapper;
    private final AnalysisReportMapper reportMapper;
    private final ReportRevisionMapper revisionMapper;
    private final IInterpretationService interpretationService;
    private final ClusterDriverInvoker driver;
    private final ReportOperationLock operationLock;

    public AnalysisReportVo reinterpret(InterpretationReinterpretBo bo) {
        return operationLock.call(-bo.getAnalysisId(), () -> operationLock.call(bo.getReportId(),
            () -> reinterpretLocked(bo)));
    }

    private AnalysisReportVo reinterpretLocked(InterpretationReinterpretBo bo) {
        Map<String, Object> source = interpretationMapper.selectReportRow(bo.getReportId());
        if (source == null || !bo.getAnalysisId().toString().equals(String.valueOf(source.get("analysisId")))) {
            throw new ServiceException("来源报告不存在或与分析批次不匹配");
        }
        if (!"62".equals(String.valueOf(source.get("status")))) {
            throw new ServiceException("只有已发送的报告可以重新解读");
        }
        Long childId = revisionMapper.selectChild(bo.getReportId());
        if (childId != null) {
            return reportMapper.selectVoById(childId);
        }
        driver.requireBatchEnabled();
        auditRecorder.process("重新解读扫描发起", "STARTED", AuditTarget.report(bo.getReportId()));
        DriverInvokeResult result;
        try {
            result = driver.invokeBatch(source.get("analysisDate").toString(),
                source.get("product").toString(), source.get("subbarcode").toString());
        } catch (RuntimeException exception) {
            auditRecorder.process("重新解读扫描失败", "FAILED", AuditTarget.report(bo.getReportId()));
            throw exception;
        }
        String outcome = result.isSuccess() ? "SUCCESS" : "FAILED";
        auditRecorder.process("重新解读扫描返回", outcome, AuditTarget.report(bo.getReportId()));
        if (!result.isSuccess() || result.getFileCount() <= 0
            || revisionMapper.countFiles(bo.getAnalysisId()) == 0
            || revisionMapper.countBadFiles(bo.getAnalysisId()) > 0
            || !Integer.valueOf(11).equals(interpretationMapper.selectAnalysisDriveStatus(bo.getAnalysisId()))) {
            throw new ServiceException("重新扫描未全部成功，未创建新报告：" + result.getMessage());
        }
        return TenantContext.withTenant(source.get("tenantId").toString(),
            () -> interpretationService.enterAfterRescan(bo.getAnalysisId(), bo.getReportId()));
    }
}
