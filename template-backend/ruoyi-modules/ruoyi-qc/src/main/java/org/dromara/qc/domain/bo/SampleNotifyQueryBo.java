package org.dromara.qc.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 样本通知台账 查询条件（异常处理页）
 *
 * @author <你的名字>
 */
@Data
public class SampleNotifyQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 通知类型（delay / sample / wet_lab / bioinfo；空=全部） */
    private String notifyType;

    /** 通知状态（generated / pending / sent / failed；空=全部） */
    private String notifyStatus;

    /** 通知日期起（yyyyMMdd，闭区间） */
    private String notifyDateStart;

    /** 通知日期止（yyyyMMdd，闭区间） */
    private String notifyDateEnd;

    /** 样本编号（模糊） */
    private String subbarcode;

    /** 项目编号（模糊） */
    private String projectNo;

    /** 关键字（样本编号 / 样本名称 / 参与者编号 模糊） */
    private String keyword;
}
