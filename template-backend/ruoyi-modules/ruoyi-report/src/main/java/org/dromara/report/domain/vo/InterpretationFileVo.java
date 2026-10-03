package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 解读页 Tab② 集群对接：文件列表行（data_file_status）
 * <p>
 * 只带列表需要的列，不带 {@code file_text}（LONGTEXT，最大几十 KB）——
 * 内容由「查看文件」单独接口按 file_id 取，避免列表接口把大字段全查出来。
 *
 * @author <你的名字>
 */
@Data
public class InterpretationFileVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 文件ID */
    private Long fileId;

    /** 文件类型（SNP / Indel / CNV / Fusion / CR_ALL / MSI / qc / Chem / Chemical_all） */
    private String fileType;

    /** 数据类别（variant / druginfo …） */
    private String dataType;

    /** 文件名 */
    private String fileName;

    /** 状态：Pending / Loaded / Error */
    private String status;

    /** 失败原因（status=Error 时） */
    private String message;

    /** 解析出的变异数 */
    private Integer mutNum;

    /** 分析日期（YYYYMMDD） */
    private String analysisDate;

    /** file_text 长度（字符），用于页面提示大文件 */
    private Long textLength;

    /** 更新时间 */
    private Date updateTime;
}
