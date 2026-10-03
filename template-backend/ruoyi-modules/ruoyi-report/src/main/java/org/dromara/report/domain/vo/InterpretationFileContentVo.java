package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 解读页 Tab② 文件内容（data_file_status.file_text）
 *
 * @author <你的名字>
 */
@Data
public class InterpretationFileContentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 文件ID */
    private Long fileId;

    /** 文件名 */
    private String fileName;

    /** 文件类型 */
    private String fileType;

    /** 文件内容（过长时按 MAX_TEXT_LENGTH 截断） */
    private String fileText;

    /** 是否被截断 */
    private Boolean truncated;

    /** 原始长度 */
    private Long textLength;
}
