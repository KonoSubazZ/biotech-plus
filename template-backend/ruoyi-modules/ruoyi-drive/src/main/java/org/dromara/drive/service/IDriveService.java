package org.dromara.drive.service;

import org.dromara.drive.domain.bo.DriveSampleBo;
import org.dromara.drive.domain.vo.DriveFileVo;
import org.dromara.drive.domain.vo.DriveSampleVo;

/**
 * 驱动入库服务（同步执行：调用方要拿最终状态与文件数）。
 * <p>
 * 本期只保留两个入口（用户 2026-10-22 口径）：
 * <ol>
 *   <li>{@link #driveSample(DriveSampleBo)}：按「批次目录 + 产品编码 + 样本编号」驱动整个样本
 *       （UPLOAD 判重后新建 analysis_id / REINTERPRET 不判重新建 analysis_id）；</li>
 *   <li>{@link #driveFile(Long, Long)}：某个文件的重新驱动（同批次，老行 del_flag='1' + 新行新 file_id）。</li>
 * </ol>
 *
 * @author <你的名字>
 */
public interface IDriveService {

    /**
     * 驱动单个样本。
     *
     * @param bo 入参（analysisBatchDate / productBio / subbarcode / type）
     * @return 样本驱动结果
     */
    DriveSampleVo driveSample(DriveSampleBo bo);

    /**
     * 重新驱动单个文件（同一批次，老行 del_flag='1' + 新行新 file_id）。
     *
     * @param analysisId 分析批次 ID
     * @param fileId     文件 ID
     * @return 文件驱动结果（含样本级最新状态）
     */
    DriveFileVo driveFile(Long analysisId, Long fileId);
}
