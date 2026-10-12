package org.dromara.qc.domain.vo;

import lombok.Data;

/**
 * 不合格通知要回填的样本字段（来自 sample_file，按 subbarcode 取）
 * <p>
 * 只取通知单表头用得上的几个列（示例口径）：研究方案编号 ← contracts_no、诺禾项目编号 ← order_code、
 * 试验参与者编号 ← pcode、研究中心名称 ← testing_institution、样本接收日期 ← collect_date、
 * 样本类型/数量 ← sample_type/specimen_num、检测产品 ← erp_test_name、客户 ← customer_name。
 *
 * @author <你的名字>
 */
@Data
public class QcNotifySampleVo {

    /** sample_file.id */
    private Long id;

    /** 样本编号 */
    private String subbarcode;

    /** 研究方案编号（sample_file.contracts_no） */
    private String studyNo;

    /** 诺禾项目编号（sample_file.order_code） */
    private String projectNo;

    /** 试验参与者编号（sample_file.pcode） */
    private String participantNo;

    /** 研究中心名称（sample_file.testing_institution） */
    private String center;

    /** 样本接收日期（sample_file.collect_date） */
    private String receiveDate;

    /** 样本类型 */
    private String sampleType;

    /** 样本数量（sample_file.specimen_num） */
    private String sampleCount;

    /** 检测产品（sample_file.erp_test_name） */
    private String product;

    /** 客户（= 药企名，用于解析归属药企） */
    private String customerName;
}
