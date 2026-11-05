package org.dromara.report.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一个字段的变更项（同时是 cms_sample.sync_log 里 JSON 数组的一个元素）。
 * <p>
 * sync_log 存的就是本对象的数组：[{field,label,oldValue,newValue,by,at}, ...]。
 *
 * @author <你的名字>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CmsFieldChangeVo {

    /** 实体属性名，如 sampleType；新增样本时固定为 __insert__ */
    private String field;

    /** 中文名，如 样本类型 */
    private String label;

    /** 旧值（新增时为 null） */
    private String oldValue;

    /** 新值 */
    private String newValue;

    /** 本次变更的操作人：SYSTEM = 定时任务，否则为登录名 */
    private String by;

    /** 本次变更时间 yyyy-MM-dd HH:mm:ss */
    private String at;
}
