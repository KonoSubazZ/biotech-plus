package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 「重新驱动」单个文件的结果（集群对接页签的文件行按钮）。
 * <p>
 * 文件级状态与样本级驱动状态都由驱动脚本写库，这里只是把**最新值**读回来给页面刷新用。
 *
 * @author <你的名字>
 */
@Data
public class FileRedriveVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 文件ID */
    private Long fileId;

    /** 文件名 */
    private String fileName;

    /** 该文件驱动后的状态（data_file_status.status：Loaded / Pending / Error） */
    private String fileStatus;

    /** 该批次的样本级驱动状态（int 码：10 失败 / 11 成功 / 12 驱动中） */
    private Integer driveStatus;

    /** 驱动脚本给的说明（成功几行/失败原因原样带回） */
    private String message;
}
