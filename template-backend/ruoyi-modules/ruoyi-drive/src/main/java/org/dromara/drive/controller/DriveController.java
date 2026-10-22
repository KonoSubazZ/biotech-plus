package org.dromara.drive.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.drive.domain.bo.DriveFileBo;
import org.dromara.drive.domain.bo.DriveSampleBo;
import org.dromara.drive.domain.vo.DriveFileVo;
import org.dromara.drive.domain.vo.DriveSampleVo;
import org.dromara.drive.service.IDriveService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据驱动入库（生信流程上传 → 系统驱动；页面按钮复用同一批能力）。
 * <p>
 * <b>同步返回</b>：调用方（生信流程）要拿最终状态、文件数与四类变异数，与旧脚本 {@code scanner.py}
 * 的返回口径一致，所以这里不做异步任务。
 * <p>
 * 本期只有两个入口（用户 2026-10-22 口径）：按样本上传 / 按文件重新驱动。
 * 认证：外部系统带固定 token（{@code X-Api-Token}，路径配在 application.yml 的 api.token.paths），
 * 平台内页面按钮走正常登录态；两种身份都按 {@code report:drive:*} 权限点校验。
 *
 * @author <你的名字>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/report/drive")
public class DriveController extends BaseController {

    private final IDriveService driveService;

    /**
     * 驱动单个样本：type=UPLOAD（默认，三件套已驱动过则拒绝）/ REINTERPRET（重新解读，新建 analysis_id）。
     *
     * @param bo 入参（analysisBatchDate / productBio / subbarcode / type）
     * @return 该样本的驱动结果
     */
    @SaCheckPermission("report:drive:sample")
    @Log(title = "数据驱动-按样本", businessType = BusinessType.INSERT)
    @PostMapping("/sample")
    public R<DriveSampleVo> driveSample(@Validated @RequestBody DriveSampleBo bo) {
        return R.ok(driveService.driveSample(bo));
    }

    /**
     * 重新驱动某个文件（同一批次：老行 del_flag='1'，新行新 file_id）。
     *
     * @param bo 入参（analysisId + fileId）
     * @return 文件驱动结果（含样本级最新状态）
     */
    @SaCheckPermission("report:drive:file")
    @Log(title = "数据驱动-重新驱动文件", businessType = BusinessType.UPDATE)
    @PostMapping("/file")
    public R<DriveFileVo> driveFile(@Validated @RequestBody DriveFileBo bo) {
        return R.ok(driveService.driveFile(bo.getAnalysisId(), bo.getFileId()));
    }
}
