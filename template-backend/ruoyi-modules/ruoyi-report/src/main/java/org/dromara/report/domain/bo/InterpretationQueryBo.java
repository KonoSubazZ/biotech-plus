package org.dromara.report.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 报告解读列表查询条件
 * <p>
 * 日期范围走 BaseEntity.params 的 beginTime / endTime（前端 NDatePicker type="daterange"
 * 写进 params，由 axios 的 qs 序列化传过来）——analysis_date 是 varchar(8) 的 YYYYMMDD，
 * 前端已把 yyyy-MM-dd 转成 yyyyMMdd 再传，避免字符串比较口径不一致。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InterpretationQueryBo extends BaseEntity {

    /** 样本编号（模糊） */
    private String subbarcode;

    /** 产品名称（模糊） */
    private String product;

    /** 报告状态（精确；空表示不限） */
    private String reportStatus;
}
