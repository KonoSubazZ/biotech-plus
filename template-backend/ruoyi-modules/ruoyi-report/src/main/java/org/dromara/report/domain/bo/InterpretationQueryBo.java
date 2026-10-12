package org.dromara.report.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 报告解读列表查询条件
 * <p>
 * 分析日期是**单个日期**（前端 NDatePicker type="date"，value-format=yyyyMMdd，默认当天），
 * 精确匹配 analysis_data.analysis_date（varchar(8) 的 YYYYMMDD）。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InterpretationQueryBo extends BaseEntity {

    /** 分析日期（精确匹配，YYYYMMDD；空表示不限） */
    private String analysisDate;

    /** 样本编号（模糊） */
    private String subbarcode;

    /** 产品名称（模糊） */
    private String product;

    /** 报告状态（精确；空表示不限） */
    private String reportStatus;
}
