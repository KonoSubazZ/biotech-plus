package org.dromara.report.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.report.domain.CmsSample;

/**
 * 样本信息视图对象 cms_sample（85 个有效字段，不含 deprecated_*）
 *
 * @author <你的名字>
 */
@Data
@NoArgsConstructor
@AutoMapper(target = CmsSample.class)
public class CmsSampleVo {

    /** 主键 */
    private Long id;

    /** CMS订单批次主键 */
    private String cmsBatchId;

    /** 正式订单编号 */
    private String orderNo;

    /** 诺禾编号 */
    private String subbarcode;

    /** 订单批次编号 */
    private String batchNo;

    /** 订单业务类型 */
    private String businessType;

    /** 订单收样确认状态 */
    private String receiveConfirmStatus;

    /** 合同甲方客户编码 */
    private String customerCode;

    /** 正式订单客户名称 */
    private String customerName;

    /** 客户类型 */
    private String customerType;

    /** 关联合同编号 */
    private String contractNo;

    /** 关联合同名称 */
    private String contractName;

    /** 研究方案编号 */
    private String schemeNo;

    /** 药企项目编号 */
    private String projectNo;

    /** 研究中心名称 */
    private String researchCenter;

    /** 合同事业部编码 */
    private String departmentCode;

    /** 合同签订日期 */
    private String contractSignDate;

    /** 合同账期 */
    private String paymentTerm;

    /** 合同结算逻辑 */
    private String checkoutLogic;

    /** 合同冻结状态 */
    private String contractBlockStatus;

    /** 合同联系人邮箱 */
    private String contractContactEmail;

    /** 合同运营经理邮箱 */
    private String operatorEmail;

    /** 合同甲方CSS主账号邮箱 */
    private String customerAccountEmail;

    /** 报告接收人 */
    private String reportReceiver;

    /** 报告接收邮箱 */
    private String reportEmail;

    /** 报告接收人电话 */
    private String reportReceiverPhone;

    /** 报告邮寄地址 */
    private String reportMailingAddress;

    /** 运营经理名称 */
    private String operatorName;

    /** 运营经理编码 */
    private String operatorCode;

    /** 合同跟进销售名称 */
    private String salesName;

    /** 合同跟进销售编码 */
    private String salesCode;

    /** 合同关联销售邮箱 */
    private String salesEmail;

    /** 订单保存的销售名称 */
    private String orderSalesName;

    /** 合同技术支持邮箱 */
    private String supportEmail;

    /** 合同经理邮箱 */
    private String managerEmail;

    /** 订单创建人编码 */
    private String orderRecorderCode;

    /** 订单创建人名称 */
    private String orderRecorderName;

    /** 样本记录人名称 */
    private String sampleRecorderName;

    /** 订单销售渠道 */
    private String salesChannel;

    /** 订单销售区域 */
    private String salesRegion;

    /** 送检人 */
    private String senderName;

    /** 送检人联系电话 */
    private String senderPhone;

    /** 送检日期 */
    private String sendDate;

    /** 样本快递公司 */
    private String expressCompany;

    /** 样本快递单号 */
    private String expressNo;

    /** 运输条件 */
    private String transportCondition;

    /** 运输条件编码 */
    private String transportConditionCode;

    /** 是否带温控计 */
    private String hasTemperatureLogger;

    /** 收样实验室 */
    private String receivingLab;

    /** 收样联系人 */
    private String receivingContact;

    /** 收样联系电话 */
    private String receivingPhone;

    /** 收样地址 */
    private String receivingAddress;

    /** 客户采样识别码 */
    private String customerSamplingCode;

    /** 样本名称 */
    private String sampleName;

    /** 试验参与者编号 */
    private String subjectNo;

    /** 性别 */
    private String sex;

    /** 出生年份 */
    private String birthYear;

    /** 临床诊断或疾病类型 */
    private String diseaseType;

    /** 访视周期 */
    private String visitCycle;

    /** 产品编码 */
    private String productCode;

    /** 产品名称 */
    private String productName;

    /** 样本类型 */
    private String sampleType;

    /** 样本类型家族码 */
    private String sampleTypeCode;

    /** 样本数量 */
    private String sampleQuantity;

    /** 数量单位 */
    private String quantityUnit;

    /** 样本属性 */
    private String sampleAttribute;

    /** 取材部位 */
    private String collectionSite;

    /** 组织类型 */
    private String tissueType;

    /** 肿瘤类型 */
    private String tumorType;

    /** 原发部位 */
    private String primaryTumorSite;

    /** 采集日期 */
    private String collectionDate;

    /** 检测方式 */
    private String assayMethod;

    /** 样本备注 */
    private String remark;

    /** 异常备注 */
    private String abnormalRemark;

    /** 管控类型 */
    private String controlType;

    /** 病理编号 */
    private String pathologyNo;

    /** 04类附件路径 */
    private String pathologyReportPaths;

    /** 03类附件路径 */
    private String otherAttachmentPaths;

    /** 订单收样确认时间 */
    private String receiveConfirmTime;

    /** 订单提交时间 */
    private String orderSubmitTime;

    /** 到达日期 */
    private String arrivalDate;

    /** 到达时间 */
    private String arrivalTime;

    /** 实际支付金额 */
    private String paidAmount;

    /** 订单金额 */
    private String orderAmount;

    /** 订单流水标识 */
    private String paymentSerialNo;

    /** 付款完成时间 */
    private String paymentFinishTime;

}