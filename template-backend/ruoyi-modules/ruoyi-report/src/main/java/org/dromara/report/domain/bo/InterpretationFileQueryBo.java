package org.dromara.report.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 解读页 Tab② 文件列表查询条件
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InterpretationFileQueryBo extends BaseEntity {

    /** 文件名（模糊） */
    private String fileName;

    /** 文件类型（模糊） */
    private String fileType;

    /** 状态（精确：Pending / Loaded / Error） */
    private String status;
}
