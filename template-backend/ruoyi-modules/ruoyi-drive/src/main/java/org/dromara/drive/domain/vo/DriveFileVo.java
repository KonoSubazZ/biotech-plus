package org.dromara.drive.domain.vo;

import lombok.Data;

/**
 * 单个文件的驱动结果（页面「重新驱动」返回体）。
 *
 * @author <你的名字>
 */
@Data
public class DriveFileVo {

    /** 文件 ID（重新驱动后是新插入的那条） */
    private Long fileId;

    /** 文件名 */
    private String fileName;

    /** 文件类型（SNP / Indel / CNV / Fusion / CR_ALL / MSI / qc / qc_g / Chem / Chemical_all） */
    private String fileType;

    /** 文件状态：Loaded 成功 / Error 失败 */
    private String status;

    /** 该文件写入明细的行数（= analysis_file_status.mut_num） */
    private Integer mutNum;

    /** 处理信息（失败原因是驱动器给出的原始错误） */
    private String message;

    /** 驱动后的样本级状态：10 失败 / 11 成功 / 12 驱动中 */
    private Integer driveStatus;
}
