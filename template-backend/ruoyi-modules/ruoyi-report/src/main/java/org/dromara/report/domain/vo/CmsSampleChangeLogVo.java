package org.dromara.report.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.report.domain.CmsSampleChangeLog;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 样本信息变更流水（抽屉「历史修改信息」展示用）
 *
 * @author <你的名字>
 */
@Data
@AutoMapper(target = CmsSampleChangeLog.class)
public class CmsSampleChangeLogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 诺禾样本编号 */
    private String subbarcode;

    /** 来源：refresh=行内从CMS更新，edit=页面编辑 */
    private String source;

    /** 字段属性名 */
    private String field;

    /** 字段中文名 */
    private String label;

    /** 旧值 */
    private String oldValue;

    /** 新值 */
    private String newValue;

    /** 操作人 */
    private String operator;

    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date operateTime;
}
