package org.dromara.drive.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 单文件重新驱动的入参（页面「重新驱动」按钮：某个文件有更新）。
 * <p>
 * 口径：同一批次内重跑 —— 该文件当前 del_flag='0' 的行置 '1'，再插一条新行（新 file_id）。
 *
 * @author <你的名字>
 */
@Data
public class DriveFileBo {

    /** 分析批次 ID */
    @NotNull(message = "analysisId 不能为空")
    private Long analysisId;

    /** 文件 ID（取 analysis_file_status.file_id） */
    @NotNull(message = "fileId 不能为空")
    private Long fileId;
}
