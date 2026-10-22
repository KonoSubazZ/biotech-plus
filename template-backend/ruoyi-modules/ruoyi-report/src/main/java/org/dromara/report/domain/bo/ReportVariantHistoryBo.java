package org.dromara.report.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 历史检出 查询对象（报告管理 › 历史检出）
 * <p>
 * 搜索栏 5 项：报告时间（范围，走 params.beginTime/endTime → report_generated_at）、
 * 样本编号、解读癌种、产品、客户（都是模糊匹配）。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReportVariantHistoryBo extends BaseEntity {

    /** 样本编号（模糊） */
    private String subbarcode;

    /** 解读癌种（模糊） */
    private String disease;

    /** 产品（模糊） */
    private String product;

    /** 客户（模糊） */
    private String client;

    /** 项目编号（模糊查询） */
    private String projectNo;
}
