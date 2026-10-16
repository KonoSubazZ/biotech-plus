package org.dromara.report.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 「重新驱动」入参（集群对接页签的文件行）。
 *
 * @author <你的名字>
 */
@Data
public class InterpretationFileRedriveBo {

    /** 分析数据ID（文件范围锁定在这个批次内） */
    @NotNull(message = "分析数据ID不能为空")
    private Long analysisId;

    /** 文件ID（data_file_status.file_id） */
    @NotNull(message = "文件ID不能为空")
    private Long fileId;
}
