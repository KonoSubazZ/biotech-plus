package org.dromara.drive.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.TenantContext;
import org.dromara.drive.config.DriveProperties;
import org.dromara.drive.domain.bo.DriveSampleBo;
import org.dromara.drive.domain.vo.DriveFileVo;
import org.dromara.drive.domain.vo.DriveSampleVo;
import org.dromara.drive.service.IDriveService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 驱动入库服务实现：两个入口都是<b>同步</b>执行（调用方要拿最终状态与文件数）。
 * <p>
 * 数据访问统一包在 {@link TenantContext#withoutTenant} 里：驱动属于平台动作，
 * 不参与租户数据权限（用户 2026-10-22 口径：本期仍不考虑 tenant_id）。
 *
 * @author <你的名字>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriveServiceImpl implements IDriveService {

    private final DriveProperties properties;
    private final DriveSampleRunner runner;

    @Override
    public DriveSampleVo driveSample(DriveSampleBo bo) {
        requireReady();
        String type = StringUtils.hasText(bo.getType())
            ? bo.getType().trim().toUpperCase() : DriveSampleBo.TYPE_UPLOAD;
        if (!DriveSampleBo.TYPE_UPLOAD.equals(type) && !DriveSampleBo.TYPE_REINTERPRET.equals(type)) {
            throw new ServiceException("驱动类型不合法：只支持 UPLOAD / REINTERPRET，实际=" + bo.getType());
        }
        boolean rejectIfDriven = DriveSampleBo.TYPE_UPLOAD.equals(type);
        log.info("驱动样本：batch={}，productBio={}，subbarcode={}，type={}", bo.getAnalysisBatchDate(),
            bo.getProductBio(), bo.getSubbarcode(), type);
        return TenantContext.withoutTenant(() -> runner.run(bo.getAnalysisBatchDate(), bo.getProductBio(),
            bo.getSubbarcode(), rejectIfDriven));
    }

    @Override
    public DriveFileVo driveFile(Long analysisId, Long fileId) {
        requireReady();
        if (analysisId == null || fileId == null) {
            throw new ServiceException("analysisId 与 fileId 不能为空");
        }
        log.info("重新驱动文件：analysisId={}，fileId={}", analysisId, fileId);
        return TenantContext.withoutTenant(() -> {
            DriveFileVo vo = runner.driveSingleFile(analysisId, fileId);
            log.info("重新驱动文件完成：fileId={}，status={}，driveStatus={}", fileId, vo.getStatus(), vo.getDriveStatus());
            return vo;
        });
    }

    /** 上传根目录没配就快速失败（生产靠 DRIVE_ROOT_DIR，dev 配 /tmp/rp-upload） */
    private void requireReady() {
        if (!StringUtils.hasText(properties.getRootDir())) {
            throw new ServiceException("未配置上传根目录（drive.root-dir），无法驱动");
        }
    }
}
