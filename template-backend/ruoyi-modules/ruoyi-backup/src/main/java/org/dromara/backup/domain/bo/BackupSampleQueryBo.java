package org.dromara.backup.domain.bo;

import lombok.Data;

/**
 * 样本备份查询条件
 * <p>
 * 样本级列表按 backup_sample_file 聚合，条件都作用在明细行上；
 * 日期用 yyyy-MM-dd 字符串传，Service 里转成区间（含首尾日）。
 *
 * @author liushangzhi
 */
@Data
public class BackupSampleQueryBo {

    /** 样本编号（模糊） */
    private String subbarcode;

    /** 项目编号（模糊） */
    private String projectNo;

    /** 订单批次编号（模糊） */
    private String orderCode;

    /** 生信产品编码（精确） */
    private String productBio;

    /** 样本级状态：0 待备份 / 2 全部成功 / 3 部分成功 / 4 全部失败 */
    private Integer backupStatus;

    /** 起始日期（含，按明细创建时间） */
    private String beginDate;

    /** 结束日期（含） */
    private String endDate;
}
