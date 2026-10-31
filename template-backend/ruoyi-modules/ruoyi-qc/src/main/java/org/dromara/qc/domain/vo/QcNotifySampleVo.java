package org.dromara.qc.domain.vo;

import lombok.Data;

/**
 * 不合格通知要回填的样本字段（来自 cms_sample，按 subbarcode 取）
 * <p>
 * 只取通知单表头用得上的几个列（示例口径）：研究方案编号 ← contracts_no、诺禾项目编号 ← order_code、
 * 试验参与者编号 ← pcode、研究中心名称 ← testing_institution、样本接收日期 ← collect_date、
 * 样本类型/数量 ← sample_type/specimen_num、检测产品 ← erp_test_name、客户 ← customer_name。
 *
 * @author <你的名字>
 */
@Data
public class QcNotifySampleVo {

    /** cms_sample.id */
    private Long id;

    /** 样本编号 */
    private String subbarcode;

    /** 研究方案编号（cms_sample.contracts_no） */
    private String studyNo;

    /** 诺禾项目编号（cms_sample.order_code） */
    private String projectNo;

    /** 试验参与者编号（cms_sample.pcode） */
    private String participantNo;

    /** 研究中心名称（cms_sample.testing_institution） */
    private String center;

    /** 样本接收日期（cms_sample.collect_date） */
    private String receiveDate;

    /** 样本类型 */
    private String sampleType;

    /** 样本数量（cms_sample.specimen_num） */
    private String sampleCount;

    /** 检测产品（cms_sample.erp_test_name） */
    private String product;

    /** 客户（= 药企名，用于解析归属药企） */
    private String customerName;
}
