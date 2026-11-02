package org.dromara.report.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.report.domain.CmsSample;

/**
 * 样本信息 Excel 导入行 / 导入模板表头。
 * <p>
 * 列序与 cms_sample 的 85 个有效列一致，按 index 读取（不依赖表头文案）；
 * deprecated_* 字段不在模板里。
 *
 * @author <你的名字>
 */
@Data
@AutoMapper(target = CmsSample.class, reverseConvertGenerate = false)
public class CmsSampleExcelRow {

    /** CMS订单批次主键 */
    @ExcelProperty(value = "CMS订单批次主键", index = 0)
    private String cmsBatchId;

    /** 正式订单编号 */
    @ExcelProperty(value = "正式订单编号", index = 1)
    private String orderNo;

    /** 诺禾编号 */
    @ExcelProperty(value = "诺禾编号", index = 2)
    private String subbarcode;

    /** 订单批次编号 */
    @ExcelProperty(value = "订单批次编号", index = 3)
    private String batchNo;

    /** 订单业务类型 */
    @ExcelProperty(value = "订单业务类型", index = 4)
    private String businessType;

    /** 订单收样确认状态 */
    @ExcelProperty(value = "订单收样确认状态", index = 5)
    private String receiveConfirmStatus;

    /** 合同甲方客户编码 */
    @ExcelProperty(value = "合同甲方客户编码", index = 6)
    private String customerCode;

    /** 正式订单客户名称 */
    @ExcelProperty(value = "正式订单客户名称", index = 7)
    private String customerName;

    /** 客户类型 */
    @ExcelProperty(value = "客户类型", index = 8)
    private String customerType;

    /** 关联合同编号 */
    @ExcelProperty(value = "关联合同编号", index = 9)
    private String contractNo;

    /** 关联合同名称 */
    @ExcelProperty(value = "关联合同名称", index = 10)
    private String contractName;

    /** 研究方案编号 */
    @ExcelProperty(value = "研究方案编号", index = 11)
    private String schemeNo;

    /** 药企项目编号 */
    @ExcelProperty(value = "药企项目编号", index = 12)
    private String projectNo;

    /** 研究中心名称 */
    @ExcelProperty(value = "研究中心名称", index = 13)
    private String researchCenter;

    /** 合同事业部编码 */
    @ExcelProperty(value = "合同事业部编码", index = 14)
    private String departmentCode;

    /** 合同签订日期 */
    @ExcelProperty(value = "合同签订日期", index = 15)
    private String contractSignDate;

    /** 合同账期 */
    @ExcelProperty(value = "合同账期", index = 16)
    private String paymentTerm;

    /** 合同结算逻辑 */
    @ExcelProperty(value = "合同结算逻辑", index = 17)
    private String checkoutLogic;

    /** 合同冻结状态 */
    @ExcelProperty(value = "合同冻结状态", index = 18)
    private String contractBlockStatus;

    /** 合同联系人邮箱 */
    @ExcelProperty(value = "合同联系人邮箱", index = 19)
    private String contractContactEmail;

    /** 合同运营经理邮箱 */
    @ExcelProperty(value = "合同运营经理邮箱", index = 20)
    private String operatorEmail;

    /** 合同甲方CSS主账号邮箱 */
    @ExcelProperty(value = "合同甲方CSS主账号邮箱", index = 21)
    private String customerAccountEmail;

    /** 报告接收人 */
    @ExcelProperty(value = "报告接收人", index = 22)
    private String reportReceiver;

    /** 报告接收邮箱 */
    @ExcelProperty(value = "报告接收邮箱", index = 23)
    private String reportEmail;

    /** 报告接收人电话 */
    @ExcelProperty(value = "报告接收人电话", index = 24)
    private String reportReceiverPhone;

    /** 报告邮寄地址 */
    @ExcelProperty(value = "报告邮寄地址", index = 25)
    private String reportMailingAddress;

    /** 运营经理名称 */
    @ExcelProperty(value = "运营经理名称", index = 26)
    private String operatorName;

    /** 运营经理编码 */
    @ExcelProperty(value = "运营经理编码", index = 27)
    private String operatorCode;

    /** 合同跟进销售名称 */
    @ExcelProperty(value = "合同跟进销售名称", index = 28)
    private String salesName;

    /** 合同跟进销售编码 */
    @ExcelProperty(value = "合同跟进销售编码", index = 29)
    private String salesCode;

    /** 合同关联销售邮箱 */
    @ExcelProperty(value = "合同关联销售邮箱", index = 30)
    private String salesEmail;

    /** 订单保存的销售名称 */
    @ExcelProperty(value = "订单保存的销售名称", index = 31)
    private String orderSalesName;

    /** 合同技术支持邮箱 */
    @ExcelProperty(value = "合同技术支持邮箱", index = 32)
    private String supportEmail;

    /** 合同经理邮箱 */
    @ExcelProperty(value = "合同经理邮箱", index = 33)
    private String managerEmail;

    /** 订单创建人编码 */
    @ExcelProperty(value = "订单创建人编码", index = 34)
    private String orderRecorderCode;

    /** 订单创建人名称 */
    @ExcelProperty(value = "订单创建人名称", index = 35)
    private String orderRecorderName;

    /** 样本记录人名称 */
    @ExcelProperty(value = "样本记录人名称", index = 36)
    private String sampleRecorderName;

    /** 订单销售渠道 */
    @ExcelProperty(value = "订单销售渠道", index = 37)
    private String salesChannel;

    /** 订单销售区域 */
    @ExcelProperty(value = "订单销售区域", index = 38)
    private String salesRegion;

    /** 送检人 */
    @ExcelProperty(value = "送检人", index = 39)
    private String senderName;

    /** 送检人联系电话 */
    @ExcelProperty(value = "送检人联系电话", index = 40)
    private String senderPhone;

    /** 送检日期 */
    @ExcelProperty(value = "送检日期", index = 41)
    private String sendDate;

    /** 样本快递公司 */
    @ExcelProperty(value = "样本快递公司", index = 42)
    private String expressCompany;

    /** 样本快递单号 */
    @ExcelProperty(value = "样本快递单号", index = 43)
    private String expressNo;

    /** 运输条件 */
    @ExcelProperty(value = "运输条件", index = 44)
    private String transportCondition;

    /** 运输条件编码 */
    @ExcelProperty(value = "运输条件编码", index = 45)
    private String transportConditionCode;

    /** 是否带温控计 */
    @ExcelProperty(value = "是否带温控计", index = 46)
    private String hasTemperatureLogger;

    /** 收样实验室 */
    @ExcelProperty(value = "收样实验室", index = 47)
    private String receivingLab;

    /** 收样联系人 */
    @ExcelProperty(value = "收样联系人", index = 48)
    private String receivingContact;

    /** 收样联系电话 */
    @ExcelProperty(value = "收样联系电话", index = 49)
    private String receivingPhone;

    /** 收样地址 */
    @ExcelProperty(value = "收样地址", index = 50)
    private String receivingAddress;

    /** 客户采样识别码 */
    @ExcelProperty(value = "客户采样识别码", index = 51)
    private String customerSamplingCode;

    /** 样本名称 */
    @ExcelProperty(value = "样本名称", index = 52)
    private String sampleName;

    /** 试验参与者编号 */
    @ExcelProperty(value = "试验参与者编号", index = 53)
    private String subjectNo;

    /** 性别 */
    @ExcelProperty(value = "性别", index = 54)
    private String sex;

    /** 出生年份 */
    @ExcelProperty(value = "出生年份", index = 55)
    private String birthYear;

    /** 临床诊断或疾病类型 */
    @ExcelProperty(value = "临床诊断或疾病类型", index = 56)
    private String diseaseType;

    /** 访视周期 */
    @ExcelProperty(value = "访视周期", index = 57)
    private String visitCycle;

    /** 产品编码 */
    @ExcelProperty(value = "产品编码", index = 58)
    private String productCode;

    /** 产品名称 */
    @ExcelProperty(value = "产品名称", index = 59)
    private String productName;

    /** 样本类型 */
    @ExcelProperty(value = "样本类型", index = 60)
    private String sampleType;

    /** 样本类型家族码 */
    @ExcelProperty(value = "样本类型家族码", index = 61)
    private String sampleTypeCode;

    /** 样本数量 */
    @ExcelProperty(value = "样本数量", index = 62)
    private String sampleQuantity;

    /** 数量单位 */
    @ExcelProperty(value = "数量单位", index = 63)
    private String quantityUnit;

    /** 样本属性 */
    @ExcelProperty(value = "样本属性", index = 64)
    private String sampleAttribute;

    /** 取材部位 */
    @ExcelProperty(value = "取材部位", index = 65)
    private String collectionSite;

    /** 组织类型 */
    @ExcelProperty(value = "组织类型", index = 66)
    private String tissueType;

    /** 肿瘤类型 */
    @ExcelProperty(value = "肿瘤类型", index = 67)
    private String tumorType;

    /** 原发部位 */
    @ExcelProperty(value = "原发部位", index = 68)
    private String primaryTumorSite;

    /** 采集日期 */
    @ExcelProperty(value = "采集日期", index = 69)
    private String collectionDate;

    /** 检测方式 */
    @ExcelProperty(value = "检测方式", index = 70)
    private String assayMethod;

    /** 样本备注 */
    @ExcelProperty(value = "样本备注", index = 71)
    private String remark;

    /** 异常备注 */
    @ExcelProperty(value = "异常备注", index = 72)
    private String abnormalRemark;

    /** 管控类型 */
    @ExcelProperty(value = "管控类型", index = 73)
    private String controlType;

    /** 病理编号 */
    @ExcelProperty(value = "病理编号", index = 74)
    private String pathologyNo;

    /** 04类附件路径 */
    @ExcelProperty(value = "04类附件路径", index = 75)
    private String pathologyReportPaths;

    /** 03类附件路径 */
    @ExcelProperty(value = "03类附件路径", index = 76)
    private String otherAttachmentPaths;

    /** 订单收样确认时间 */
    @ExcelProperty(value = "订单收样确认时间", index = 77)
    private String receiveConfirmTime;

    /** 订单提交时间 */
    @ExcelProperty(value = "订单提交时间", index = 78)
    private String orderSubmitTime;

    /** 到达日期 */
    @ExcelProperty(value = "到达日期", index = 79)
    private String arrivalDate;

    /** 到达时间 */
    @ExcelProperty(value = "到达时间", index = 80)
    private String arrivalTime;

    /** 实际支付金额 */
    @ExcelProperty(value = "实际支付金额", index = 81)
    private String paidAmount;

    /** 订单金额 */
    @ExcelProperty(value = "订单金额", index = 82)
    private String orderAmount;

    /** 订单流水标识 */
    @ExcelProperty(value = "订单流水标识", index = 83)
    private String paymentSerialNo;

    /** 付款完成时间 */
    @ExcelProperty(value = "付款完成时间", index = 84)
    private String paymentFinishTime;

}