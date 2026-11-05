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
 * 业务键：诺禾编号（subbarcode）+ 订单编号（order_no，表里 NOT NULL）。
 * 其余字段按列长度做长度校验。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = CmsSample.class, reverseConvertGenerate = false)
public class CmsSampleBo extends BaseEntity {

    /** 主键；新增时为空，更新时必填 */
    private Long id;

    // ---------------------------------------------------------------- 订单 / 客户 / 合同
    /** 订单批次编号 */
    @Size(max = 128, message = "订单批次编号长度不能超过 128")
    private String batchNo;

    /** 客户名称 */
    @Size(max = 100, message = "客户名称长度不能超过 100")
    private String customerName;

    /** 合同名称 */
    @Size(max = 100, message = "合同名称长度不能超过 100")
    private String contractName;

    /** 合同编号 */
    @Size(max = 128, message = "合同编号长度不能超过 128")
    private String contractNo;

    /** 研究方案编号 */
    @Size(max = 128, message = "研究方案编号长度不能超过 128")
    private String schemeNo;

    /** 项目编号 */
    @Size(max = 128, message = "项目编号长度不能超过 128")
    private String projectNo;

    /** 联系人邮箱 */
    @Size(max = 100, message = "联系人邮箱长度不能超过 100")
    private String contractContactEmail;

    /** 运营经理 */
    @Size(max = 100, message = "运营经理长度不能超过 100")
    private String operatorName;

    /** 研究中心名称 */
    @Size(max = 100, message = "研究中心名称长度不能超过 100")
    private String researchCenter;

    /** 报告接收人 */
    @Size(max = 100, message = "报告接收人长度不能超过 100")
    private String reportReceiver;

    /** 报告接收邮箱 */
    @Size(max = 100, message = "报告接收邮箱长度不能超过 100")
    private String reportEmail;

    /** 送检人 */
    @Size(max = 100, message = "送检人长度不能超过 100")
    private String senderName;

    /** 联系电话 */
    @Size(max = 100, message = "联系电话长度不能超过 100")
    private String senderPhone;

    /** 送检日期 */
    @Size(max = 10, message = "送检日期长度不能超过 10")
    private String sendDate;

    /** 快递公司 */
    @Size(max = 100, message = "快递公司长度不能超过 100")
    private String expressCompany;

    /** 快递单号 */
    @Size(max = 100, message = "快递单号长度不能超过 100")
    private String expressNo;

    /** 快递条件 */
    @Size(max = 100, message = "快递条件长度不能超过 100")
    private String transportCondition;

    /** 是否带温控计 */
    @Size(max = 16, message = "是否带温控计长度不能超过 16")
    private String hasTemperatureLogger;

    /** 收样确认时间 */
    @Size(max = 19, message = "收样确认时间长度不能超过 19")
    private String receiveConfirmTime;

    /** 提交时间 */
    @Size(max = 19, message = "提交时间长度不能超过 19")
    private String orderSubmitTime;

    // ---------------------------------------------------------------- 样本 / 受试者
    /** 订单编号 */
    @Size(max = 128, message = "订单编号长度不能超过 128")
    private String orderNo;

    /** 诺禾编号 */
    @NotBlank(message = "诺禾编号不能为空")
    @Size(max = 128, message = "诺禾编号长度不能超过 128")
    private String subbarcode;

    /** 客户采样识别码 */
    @Size(max = 100, message = "客户采样识别码长度不能超过 100")
    private String customerSamplingCode;

    /** 样本名称 */
    @Size(max = 100, message = "样本名称长度不能超过 100")
    private String sampleName;

    /** 试验参与者编号 */
    @Size(max = 100, message = "试验参与者编号长度不能超过 100")
    private String subjectNo;

    /** 性别 */
    @Size(max = 16, message = "性别长度不能超过 16")
    private String sex;

    /** 出生年份 */
    @Size(max = 4, message = "出生年份长度不能超过 4")
    private String birthYear;

    /** 临床诊断 */
    @Size(max = 100, message = "临床诊断长度不能超过 100")
    private String diseaseType;

    /** 访视周期 */
    @Size(max = 100, message = "访视周期长度不能超过 100")
    private String visitCycle;

    /** 产品编码 */
    @Size(max = 100, message = "产品编码长度不能超过 100")
    private String productCode;

    /** 产品名称 */
    @Size(max = 100, message = "产品名称长度不能超过 100")
    private String productName;

    /** 样本类型 */
    @Size(max = 100, message = "样本类型长度不能超过 100")
    private String sampleType;

    /** 样本数量 */
    @Size(max = 32, message = "样本数量长度不能超过 32")
    private String sampleQuantity;

    /** 数量单位 */
    @Size(max = 32, message = "数量单位长度不能超过 32")
    private String quantityUnit;

    /** 样本属性 */
    @Size(max = 100, message = "样本属性长度不能超过 100")
    private String sampleAttribute;

    /** 取材部位 */
    @Size(max = 100, message = "取材部位长度不能超过 100")
    private String collectionSite;

    /** 组织类型 */
    @Size(max = 100, message = "组织类型长度不能超过 100")
    private String tissueType;

    /** 肿瘤类型 */
    @Size(max = 100, message = "肿瘤类型长度不能超过 100")
    private String tumorType;

    /** 原发部位 */
    @Size(max = 100, message = "原发部位长度不能超过 100")
    private String primaryTumorSite;

    /** 采集日期 */
    @Size(max = 10, message = "采集日期长度不能超过 10")
    private String collectionDate;

    /** 检测方式 */
    @Size(max = 100, message = "检测方式长度不能超过 100")
    private String assayMethod;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;

    /** 异常备注 */
    @Size(max = 5000, message = "异常备注长度不能超过 5000")
    private String abnormalRemark;

    /** 管控类型 */
    @Size(max = 100, message = "管控类型长度不能超过 100")
    private String controlType;

    /** 到达时间 */
    @Size(max = 19, message = "到达时间长度不能超过 19")
    private String arrivalTime;
}
