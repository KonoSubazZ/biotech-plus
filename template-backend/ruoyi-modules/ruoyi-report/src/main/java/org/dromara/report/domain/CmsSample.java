package org.dromara.report.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 样本信息对象 cms_sample
 * <p>
 * 只映射 85 个 biotech_* 有效业务列；59 个 deprecated_* 兼容列不落实体（页面/接口均不出现）。
 * 业务唯一键 = (tenant_id, biotech_subbarcode)，见 Service 的新增查重与导入 upsert。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cms_sample")
public class CmsSample extends BaseEntity {

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** CMS订单批次主键 */
    @TableField("biotech_cms_batch_id")
    private String cmsBatchId;

    /** 正式订单编号 */
    @TableField("biotech_order_no")
    private String orderNo;

    /** 诺禾编号 */
    @TableField("biotech_subbarcode")
    private String subbarcode;

    /** 订单批次编号 */
    @TableField("biotech_batch_no")
    private String batchNo;

    /** 订单业务类型 */
    @TableField("biotech_business_type")
    private String businessType;

    /** 订单收样确认状态 */
    @TableField("biotech_receive_confirm_status")
    private String receiveConfirmStatus;

    /** 合同甲方客户编码 */
    @TableField("biotech_customer_code")
    private String customerCode;

    /** 正式订单客户名称 */
    @TableField("biotech_customer_name")
    private String customerName;

    /** 客户类型 */
    @TableField("biotech_customer_type")
    private String customerType;

    /** 关联合同编号 */
    @TableField("biotech_contract_no")
    private String contractNo;

    /** 关联合同名称 */
    @TableField("biotech_contract_name")
    private String contractName;

    /** 研究方案编号 */
    @TableField("biotech_scheme_no")
    private String schemeNo;

    /** 药企项目编号 */
    @TableField("biotech_project_no")
    private String projectNo;

    /** 研究中心名称 */
    @TableField("biotech_research_center")
    private String researchCenter;

    /** 合同事业部编码 */
    @TableField("biotech_department_code")
    private String departmentCode;

    /** 合同签订日期 */
    @TableField("biotech_contract_sign_date")
    private String contractSignDate;

    /** 合同账期 */
    @TableField("biotech_payment_term")
    private String paymentTerm;

    /** 合同结算逻辑 */
    @TableField("biotech_checkout_logic")
    private String checkoutLogic;

    /** 合同冻结状态 */
    @TableField("biotech_contract_block_status")
    private String contractBlockStatus;

    /** 合同联系人邮箱 */
    @TableField("biotech_contract_contact_email")
    private String contractContactEmail;

    /** 合同运营经理邮箱 */
    @TableField("biotech_operator_email")
    private String operatorEmail;

    /** 合同甲方CSS主账号邮箱 */
    @TableField("biotech_customer_account_email")
    private String customerAccountEmail;

    /** 报告接收人 */
    @TableField("biotech_report_receiver")
    private String reportReceiver;

    /** 报告接收邮箱 */
    @TableField("biotech_report_email")
    private String reportEmail;

    /** 报告接收人电话 */
    @TableField("biotech_report_receiver_phone")
    private String reportReceiverPhone;

    /** 报告邮寄地址 */
    @TableField("biotech_report_mailing_address")
    private String reportMailingAddress;

    /** 运营经理名称 */
    @TableField("biotech_operator_name")
    private String operatorName;

    /** 运营经理编码 */
    @TableField("biotech_operator_code")
    private String operatorCode;

    /** 合同跟进销售名称 */
    @TableField("biotech_sales_name")
    private String salesName;

    /** 合同跟进销售编码 */
    @TableField("biotech_sales_code")
    private String salesCode;

    /** 合同关联销售邮箱 */
    @TableField("biotech_sales_email")
    private String salesEmail;

    /** 订单保存的销售名称 */
    @TableField("biotech_order_sales_name")
    private String orderSalesName;

    /** 合同技术支持邮箱 */
    @TableField("biotech_support_email")
    private String supportEmail;

    /** 合同经理邮箱 */
    @TableField("biotech_manager_email")
    private String managerEmail;

    /** 订单创建人编码 */
    @TableField("biotech_order_recorder_code")
    private String orderRecorderCode;

    /** 订单创建人名称 */
    @TableField("biotech_order_recorder_name")
    private String orderRecorderName;

    /** 样本记录人名称 */
    @TableField("biotech_sample_recorder_name")
    private String sampleRecorderName;

    /** 订单销售渠道 */
    @TableField("biotech_sales_channel")
    private String salesChannel;

    /** 订单销售区域 */
    @TableField("biotech_sales_region")
    private String salesRegion;

    /** 送检人 */
    @TableField("biotech_sender_name")
    private String senderName;

    /** 送检人联系电话 */
    @TableField("biotech_sender_phone")
    private String senderPhone;

    /** 送检日期 */
    @TableField("biotech_send_date")
    private String sendDate;

    /** 样本快递公司 */
    @TableField("biotech_express_company")
    private String expressCompany;

    /** 样本快递单号 */
    @TableField("biotech_express_no")
    private String expressNo;

    /** 运输条件 */
    @TableField("biotech_transport_condition")
    private String transportCondition;

    /** 运输条件编码 */
    @TableField("biotech_transport_condition_code")
    private String transportConditionCode;

    /** 是否带温控计 */
    @TableField("biotech_has_temperature_logger")
    private String hasTemperatureLogger;

    /** 收样实验室 */
    @TableField("biotech_receiving_lab")
    private String receivingLab;

    /** 收样联系人 */
    @TableField("biotech_receiving_contact")
    private String receivingContact;

    /** 收样联系电话 */
    @TableField("biotech_receiving_phone")
    private String receivingPhone;

    /** 收样地址 */
    @TableField("biotech_receiving_address")
    private String receivingAddress;

    /** 客户采样识别码 */
    @TableField("biotech_customer_sampling_code")
    private String customerSamplingCode;

    /** 样本名称 */
    @TableField("biotech_sample_name")
    private String sampleName;

    /** 试验参与者编号 */
    @TableField("biotech_subject_no")
    private String subjectNo;

    /** 性别 */
    @TableField("biotech_sex")
    private String sex;

    /** 出生年份 */
    @TableField("biotech_birth_year")
    private String birthYear;

    /** 临床诊断或疾病类型 */
    @TableField("biotech_disease_type")
    private String diseaseType;

    /** 访视周期 */
    @TableField("biotech_visit_cycle")
    private String visitCycle;

    /** 产品编码 */
    @TableField("biotech_product_code")
    private String productCode;

    /** 产品名称 */
    @TableField("biotech_product_name")
    private String productName;

    /** 样本类型 */
    @TableField("biotech_sample_type")
    private String sampleType;

    /** 样本类型家族码 */
    @TableField("biotech_sample_type_code")
    private String sampleTypeCode;

    /** 样本数量 */
    @TableField("biotech_sample_quantity")
    private String sampleQuantity;

    /** 数量单位 */
    @TableField("biotech_quantity_unit")
    private String quantityUnit;

    /** 样本属性 */
    @TableField("biotech_sample_attribute")
    private String sampleAttribute;

    /** 取材部位 */
    @TableField("biotech_collection_site")
    private String collectionSite;

    /** 组织类型 */
    @TableField("biotech_tissue_type")
    private String tissueType;

    /** 肿瘤类型 */
    @TableField("biotech_tumor_type")
    private String tumorType;

    /** 原发部位 */
    @TableField("biotech_primary_tumor_site")
    private String primaryTumorSite;

    /** 采集日期 */
    @TableField("biotech_collection_date")
    private String collectionDate;

    /** 检测方式 */
    @TableField("biotech_assay_method")
    private String assayMethod;

    /** 样本备注 */
    @TableField("biotech_remark")
    private String remark;

    /** 异常备注 */
    @TableField("biotech_abnormal_remark")
    private String abnormalRemark;

    /** 管控类型 */
    @TableField("biotech_control_type")
    private String controlType;

    /** 病理编号 */
    @TableField("biotech_pathology_no")
    private String pathologyNo;

    /** 04类附件路径 */
    @TableField("biotech_pathology_report_paths")
    private String pathologyReportPaths;

    /** 03类附件路径 */
    @TableField("biotech_other_attachment_paths")
    private String otherAttachmentPaths;

    /** 订单收样确认时间 */
    @TableField("biotech_receive_confirm_time")
    private String receiveConfirmTime;

    /** 订单提交时间 */
    @TableField("biotech_order_submit_time")
    private String orderSubmitTime;

    /** 到达日期 */
    @TableField("biotech_arrival_date")
    private String arrivalDate;

    /** 到达时间 */
    @TableField("biotech_arrival_time")
    private String arrivalTime;

    /** 实际支付金额 */
    @TableField("biotech_paid_amount")
    private String paidAmount;

    /** 订单金额 */
    @TableField("biotech_order_amount")
    private String orderAmount;

    /** 订单流水标识 */
    @TableField("biotech_payment_serial_no")
    private String paymentSerialNo;

    /** 付款完成时间 */
    @TableField("biotech_payment_finish_time")
    private String paymentFinishTime;

    /** 逻辑删除标志 */
    @TableLogic
    private String delFlag;
}