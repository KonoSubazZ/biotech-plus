package org.dromara.qc.domain.vo;

import lombok.Data;

/**
 * 通知要回填的样本字段（来自 cms_sample，按 subbarcode 取）
 * <p>
 * 口径与样本信息页一致：研究方案编号 ← scheme_no、诺禾项目编号 ← project_no、
 * 试验参与者编号 ← subject_no、研究中心名称 ← research_center、样本接收日期 ← receive_confirm_time、
 * 样本类型/数量 ← sample_type/sample_quantity、检测产品 ← product_name、客户 ← customer_name、
 * 报告接收邮箱 ← report_email（通知邮件收件人）。
 *
 * @author <你的名字>
 */
@Data
public class QcNotifySampleVo {

    /** cms_sample.id */
    private Long id;

    /** 样本编号 */
    private String subbarcode;

    /** 样本名称（文件命名用） */
    private String sampleName;

    /** 订单编号（cms_sample.order_no；延期通知按它取同订单的兄弟样本） */
    private String orderNo;

    /** 研究方案编号（cms_sample.scheme_no） */
    private String schemeNo;

    /** 诺禾项目编号（cms_sample.project_no） */
    private String projectNo;

    /** 试验参与者编号（cms_sample.subject_no） */
    private String subjectNo;

    /** 研究中心名称（cms_sample.research_center） */
    private String center;

    /** 样本接收日期（cms_sample.receive_confirm_time） */
    private String receiveDate;

    /** 样本类型 */
    private String sampleType;

    /** 样本数量（cms_sample.sample_quantity） */
    private String sampleCount;

    /** 检测产品（cms_sample.product_name） */
    private String productName;

    /** 客户（= 药企名，用于解析归属药企） */
    private String customerName;

    /** 报告接收邮箱（cms_sample.report_email，通知邮件收件人） */
    private String reportEmail;
}
