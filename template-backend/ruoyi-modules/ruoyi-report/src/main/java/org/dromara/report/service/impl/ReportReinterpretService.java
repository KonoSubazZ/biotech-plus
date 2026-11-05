package org.dromara.report.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.TenantContext;
import org.dromara.drive.domain.bo.DriveSampleBo;
import org.dromara.drive.domain.vo.DriveSampleVo;
import org.dromara.drive.service.IDriveService;
import org.dromara.drive.utils.DriveStatus;
import org.dromara.report.domain.bo.InterpretationReinterpretBo;
import org.dromara.report.domain.vo.AnalysisReportVo;
import org.dromara.report.mapper.AnalysisReportMapper;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.mapper.ReportRevisionMapper;
import org.dromara.report.service.IInterpretationService;
import org.dromara.report.service.impl.revision.ReportOperationLock;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 已发送报告重新解读：先按「上传」口径驱动出一个<b>新的分析批次</b>，再在新批次下创建下一轮报告。
 * <p>
 * 驱动本身（解析 + 落库 + 状态机）在 ruoyi-drive，本类只编排顺序与业务校验；
 * 外部驱动不会包在数据库事务里（同旧实现）。
 *
 * @author <你的名字>
 */
@Service
@RequiredArgsConstructor
public class ReportReinterpretService {
    private final InterpretationMapper interpretationMapper;
    private final AnalysisReportMapper reportMapper;
    private final ReportRevisionMapper revisionMapper;
    private final IInterpretationService interpretationService;
    private final IDriveService driveService;
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

        // 用户 2026-10-22 口径：重新解读 = 同一上传批次（三件套不变）+ 新建一个 analysis_id，
        // 所以 type=REINTERPRET（不判重）；批次信息以 analysis_data 为准（报告行是快照，可能是老值）
        Object batchDate = source.get("analysisBatchDate");
        Object productBio = source.get("productBio");
        if (batchDate == null || productBio == null) {
            throw new ServiceException("来源报告缺少批次信息（analysis_data 不存在）：analysisId=" + bo.getAnalysisId());
        }
        DriveSampleBo driveBo = new DriveSampleBo();
        driveBo.setAnalysisBatchDate(String.valueOf(batchDate));
        driveBo.setProductBio(String.valueOf(productBio));
        driveBo.setSubbarcode(String.valueOf(source.get("subbarcode")));
        driveBo.setType(DriveSampleBo.TYPE_REINTERPRET);
        DriveSampleVo drive = driveService.driveSample(driveBo);
        Long newAnalysisId = drive.getAnalysisId();
        if (!"success".equals(drive.getStatus()) || newAnalysisId == null
            || drive.getFileCount() == null || drive.getFileCount() <= 0
            || revisionMapper.countFiles(newAnalysisId) == 0
            || revisionMapper.countBadFiles(newAnalysisId) > 0
            || !Integer.valueOf(DriveStatus.SUCCESS).equals(drive.getDriveStatus())) {
            throw new ServiceException("重新扫描未全部成功，未创建新报告（新批次 analysisId=" + newAnalysisId + "）："
                + drive.getMessage());
        }
        return TenantContext.withTenant(source.get("tenantId").toString(),
            () -> interpretationService.enterAfterRescan(newAnalysisId, bo.getReportId()));
    }
}
