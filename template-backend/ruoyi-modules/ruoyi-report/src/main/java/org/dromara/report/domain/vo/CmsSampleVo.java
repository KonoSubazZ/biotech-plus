package org.dromara.report.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.report.domain.CmsSample;

/**
 * 样本信息视图对象 cms_sample（45 个设计字段）
 *
 * @author <你的名字>
 */
@Data
@NoArgsConstructor
@AutoMapper(target = CmsSample.class)
public class CmsSampleVo {

    /** 主键 */
    private Long id;

    // ---------------------------------------------------------------- 订单 / 客户 / 合同
    /** 订单批次编号 */
    private String batchNo;

    /** 客户名称 */
    private String customerName;

    /** 合同名称 */
    private String contractName;

    /** 合同编号 */
    private String contractNo;

    /** 研究方案编号 */
    private String schemeNo;

    /** 项目编号 */
    private String projectNo;

    /** 联系人邮箱 */
    private String contractContactEmail;

    /** 运营经理 */
    private String operatorName;

    /** 研究中心名称 */
    private String researchCenter;

    /** 报告接收人 */
    private String reportReceiver;

    /** 报告接收邮箱 */
    private String reportEmail;

    /** 送检人 */
    private String senderName;

    /** 联系电话 */
    private String senderPhone;

    /** 送检日期 */
    private String sendDate;

    /** 快递公司 */
    private String expressCompany;

    /** 快递单号 */
    private String expressNo;

    /** 快递条件 */
    private String transportCondition;

    /** 是否带温控计 */
    private String hasTemperatureLogger;

    /** 收样确认时间 */
    private String receiveConfirmTime;

    /** 提交时间 */
    private String orderSubmitTime;

    // ---------------------------------------------------------------- 样本 / 受试者
    /** 订单编号 */
    private String orderNo;

    /** 诺禾编号 */
    private String subbarcode;

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

    /** 临床诊断 */
    private String diseaseType;

    /** 访视周期 */
    private String visitCycle;

    /** 产品编码 */
    private String productCode;

    /** 产品名称 */
    private String productName;

    /** 样本类型 */
    private String sampleType;

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

    /** 备注 */
    private String remark;

    /** 异常备注 */
    private String abnormalRemark;

    /** 管控类型 */
    private String controlType;

    /** 到达时间 */
    private String arrivalTime;

    /** 最近一次同步/刷新的变更明细（JSON 数组） */
    private String syncLog;
}
