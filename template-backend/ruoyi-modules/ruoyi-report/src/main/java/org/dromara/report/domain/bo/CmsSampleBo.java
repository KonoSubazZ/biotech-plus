package org.dromara.report.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.report.domain.CmsSample;

/**
 * 样本信息业务对象 cms_sample
 * <p>
 * 诺禾编号（subbarcode）是业务唯一键，新增/修改必填；其余字段按列长度做长度校验。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = CmsSample.class, reverseConvertGenerate = false)
public class CmsSampleBo extends BaseEntity {

    /** 主键；新增时为空，更新时必填 */
    private Long id;

    @Size(max = 128, message = "CMS订单批次主键长度不能超过 128")
    private String cmsBatchId;

    @Size(max = 128, message = "正式订单编号长度不能超过 128")
    private String orderNo;

    @NotBlank(message = "诺禾编号不能为空")
    @Size(max = 128, message = "诺禾编号长度不能超过 128")
    private String subbarcode;

    @Size(max = 128, message = "订单批次编号长度不能超过 128")
    private String batchNo;

    @Size(max = 100, message = "订单业务类型长度不能超过 100")
    private String businessType;

    @Size(max = 100, message = "订单收样确认状态长度不能超过 100")
    private String receiveConfirmStatus;

    @Size(max = 128, message = "合同甲方客户编码长度不能超过 128")
    private String customerCode;

    @Size(max = 100, message = "正式订单客户名称长度不能超过 100")
    private String customerName;

    @Size(max = 100, message = "客户类型长度不能超过 100")
    private String customerType;

    @Size(max = 128, message = "关联合同编号长度不能超过 128")
    private String contractNo;

    @Size(max = 100, message = "关联合同名称长度不能超过 100")
    private String contractName;

    @Size(max = 128, message = "研究方案编号长度不能超过 128")
    private String schemeNo;

    @Size(max = 128, message = "药企项目编号长度不能超过 128")
    private String projectNo;

    @Size(max = 100, message = "研究中心名称长度不能超过 100")
    private String researchCenter;

    @Size(max = 100, message = "合同事业部编码长度不能超过 100")
    private String departmentCode;

    @Size(max = 10, message = "合同签订日期长度不能超过 10")
    private String contractSignDate;

    @Size(max = 100, message = "合同账期长度不能超过 100")
    private String paymentTerm;

    private String checkoutLogic;

    @Size(max = 100, message = "合同冻结状态长度不能超过 100")
    private String contractBlockStatus;

    @Size(max = 100, message = "合同联系人邮箱长度不能超过 100")
    private String contractContactEmail;

    @Size(max = 100, message = "合同运营经理邮箱长度不能超过 100")
    private String operatorEmail;

    @Size(max = 100, message = "合同甲方CSS主账号邮箱长度不能超过 100")
    private String customerAccountEmail;

    @Size(max = 100, message = "报告接收人长度不能超过 100")
    private String reportReceiver;

    @Size(max = 100, message = "报告接收邮箱长度不能超过 100")
    private String reportEmail;

    @Size(max = 100, message = "报告接收人电话长度不能超过 100")
    private String reportReceiverPhone;

    @Size(max = 100, message = "报告邮寄地址长度不能超过 100")
    private String reportMailingAddress;

    @Size(max = 100, message = "运营经理名称长度不能超过 100")
    private String operatorName;

    @Size(max = 100, message = "运营经理编码长度不能超过 100")
    private String operatorCode;

    @Size(max = 100, message = "合同跟进销售名称长度不能超过 100")
    private String salesName;

    @Size(max = 100, message = "合同跟进销售编码长度不能超过 100")
    private String salesCode;

    @Size(max = 100, message = "合同关联销售邮箱长度不能超过 100")
    private String salesEmail;

    @Size(max = 100, message = "订单保存的销售名称长度不能超过 100")
    private String orderSalesName;

    @Size(max = 100, message = "合同技术支持邮箱长度不能超过 100")
    private String supportEmail;

    @Size(max = 100, message = "合同经理邮箱长度不能超过 100")
    private String managerEmail;

    @Size(max = 100, message = "订单创建人编码长度不能超过 100")
    private String orderRecorderCode;

    @Size(max = 100, message = "订单创建人名称长度不能超过 100")
    private String orderRecorderName;

    @Size(max = 100, message = "样本记录人名称长度不能超过 100")
    private String sampleRecorderName;

    @Size(max = 100, message = "订单销售渠道长度不能超过 100")
    private String salesChannel;

    @Size(max = 100, message = "订单销售区域长度不能超过 100")
    private String salesRegion;

    @Size(max = 100, message = "送检人长度不能超过 100")
    private String senderName;

    @Size(max = 100, message = "送检人联系电话长度不能超过 100")
    private String senderPhone;

    @Size(max = 100, message = "送检日期长度不能超过 100")
    private String sendDate;

    @Size(max = 100, message = "样本快递公司长度不能超过 100")
    private String expressCompany;

    @Size(max = 100, message = "样本快递单号长度不能超过 100")
    private String expressNo;

    @Size(max = 100, message = "运输条件长度不能超过 100")
    private String transportCondition;

    @Size(max = 100, message = "运输条件编码长度不能超过 100")
    private String transportConditionCode;

    @Size(max = 100, message = "是否带温控计长度不能超过 100")
    private String hasTemperatureLogger;

    @Size(max = 100, message = "收样实验室长度不能超过 100")
    private String receivingLab;

    @Size(max = 100, message = "收样联系人长度不能超过 100")
    private String receivingContact;

    @Size(max = 100, message = "收样联系电话长度不能超过 100")
    private String receivingPhone;

    @Size(max = 100, message = "收样地址长度不能超过 100")
    private String receivingAddress;

    @Size(max = 100, message = "客户采样识别码长度不能超过 100")
    private String customerSamplingCode;

    @Size(max = 100, message = "样本名称长度不能超过 100")
    private String sampleName;

    @Size(max = 100, message = "试验参与者编号长度不能超过 100")
    private String subjectNo;

    @Size(max = 100, message = "性别长度不能超过 100")
    private String sex;

    @Size(max = 4, message = "出生年份长度不能超过 4")
    private String birthYear;

    @Size(max = 100, message = "临床诊断或疾病类型长度不能超过 100")
    private String diseaseType;

    @Size(max = 100, message = "访视周期长度不能超过 100")
    private String visitCycle;

    @Size(max = 100, message = "产品编码长度不能超过 100")
    private String productCode;

    @Size(max = 100, message = "产品名称长度不能超过 100")
    private String productName;

    @Size(max = 100, message = "样本类型长度不能超过 100")
    private String sampleType;

    @Size(max = 100, message = "样本类型家族码长度不能超过 100")
    private String sampleTypeCode;

    @Size(max = 100, message = "样本数量长度不能超过 100")
    private String sampleQuantity;

    @Size(max = 100, message = "数量单位长度不能超过 100")
    private String quantityUnit;

    @Size(max = 100, message = "样本属性长度不能超过 100")
    private String sampleAttribute;

    @Size(max = 100, message = "取材部位长度不能超过 100")
    private String collectionSite;

    @Size(max = 100, message = "组织类型长度不能超过 100")
    private String tissueType;

    @Size(max = 100, message = "肿瘤类型长度不能超过 100")
    private String tumorType;

    @Size(max = 100, message = "原发部位长度不能超过 100")
    private String primaryTumorSite;

    @Size(max = 10, message = "采集日期长度不能超过 10")
    private String collectionDate;

    @Size(max = 100, message = "检测方式长度不能超过 100")
    private String assayMethod;

    @Size(max = 100, message = "样本备注长度不能超过 100")
    private String remark;

    private String abnormalRemark;

    @Size(max = 100, message = "管控类型长度不能超过 100")
    private String controlType;

    @Size(max = 100, message = "病理编号长度不能超过 100")
    private String pathologyNo;

    private String pathologyReportPaths;

    private String otherAttachmentPaths;

    @Size(max = 19, message = "订单收样确认时间长度不能超过 19")
    private String receiveConfirmTime;

    @Size(max = 19, message = "订单提交时间长度不能超过 19")
    private String orderSubmitTime;

    @Size(max = 10, message = "到达日期长度不能超过 10")
    private String arrivalDate;

    @Size(max = 19, message = "到达时间长度不能超过 19")
    private String arrivalTime;

    @Size(max = 100, message = "实际支付金额长度不能超过 100")
    private String paidAmount;

    @Size(max = 100, message = "订单金额长度不能超过 100")
    private String orderAmount;

    @Size(max = 100, message = "订单流水标识长度不能超过 100")
    private String paymentSerialNo;

    @Size(max = 19, message = "付款完成时间长度不能超过 19")
    private String paymentFinishTime;

}