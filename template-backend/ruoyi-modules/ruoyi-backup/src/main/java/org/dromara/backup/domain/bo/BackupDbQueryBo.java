package org.dromara.backup.domain.bo;

import lombok.Data;

/**
 * 备份记录查询条件
 * <p>
 * 日期用 yyyy-MM-dd 字符串传入，Service 里转成区间比较（避免时区把边界日算错）。
 *
 * @author liushangzhi
 */
@Data
public class BackupDbQueryBo {

    /** 覆盖的库（包含该值即命中，用于按单库筛备份记录） */
    private String dbScope;

    /** 实例标识（精确） */
    private String instance;

    /** 起始日期（含） */
    private String beginDate;

    /** 结束日期（含） */
    private String endDate;
}
