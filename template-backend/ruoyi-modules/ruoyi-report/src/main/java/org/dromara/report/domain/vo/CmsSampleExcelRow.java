package org.dromara.report.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.report.domain.CmsSample;

/**
 * 样本信息 Excel 导入行 / 导入模板表头。
 * <p>
 * 列序与 cms_sample 的 45 个设计字段一致，按 index 读取（不依赖表头文案）。
 *
 * @author <你的名字>
 */
@Data
@AutoMapper(target = CmsSample.class, reverseConvertGenerate = false)
public class CmsSampleExcelRow {

    @ExcelProperty(value = "订单批次编号", index = 0)
    private String batchNo;

    @ExcelProperty(value = "客户名称", index = 1)
    private String customerName;

    @ExcelProperty(value = "合同名称", index = 2)
    private String contractName;

    @ExcelProperty(value = "合同编号", index = 3)
    private String contractNo;

    @ExcelProperty(value = "研究方案编号", index = 4)
    private String schemeNo;

    @ExcelProperty(value = "项目编号", index = 5)
    private String projectNo;

    @ExcelProperty(value = "联系人邮箱", index = 6)
    private String contractContactEmail;

    @ExcelProperty(value = "运营经理", index = 7)
    private String operatorName;

    @ExcelProperty(value = "研究中心名称", index = 8)
    private String researchCenter;

    @ExcelProperty(value = "报告接收人", index = 9)
    private String reportReceiver;

    @ExcelProperty(value = "报告接收邮箱", index = 10)
    private String reportEmail;

    @ExcelProperty(value = "送检人", index = 11)
    private String senderName;

    @ExcelProperty(value = "联系电话", index = 12)
    private String senderPhone;

    @ExcelProperty(value = "送检日期", index = 13)
    private String sendDate;

    @ExcelProperty(value = "快递公司", index = 14)
    private String expressCompany;

    @ExcelProperty(value = "快递单号", index = 15)
    private String expressNo;

    @ExcelProperty(value = "快递条件", index = 16)
    private String transportCondition;

    @ExcelProperty(value = "是否带温控计", index = 17)
    private String hasTemperatureLogger;

    @ExcelProperty(value = "收样确认时间", index = 18)
    private String receiveConfirmTime;

    @ExcelProperty(value = "提交时间", index = 19)
    private String orderSubmitTime;

    @ExcelProperty(value = "订单编号", index = 20)
    private String orderNo;

    @ExcelProperty(value = "诺禾编号", index = 21)
    private String subbarcode;

    @ExcelProperty(value = "客户采样识别码", index = 22)
    private String customerSamplingCode;

    @ExcelProperty(value = "样本名称", index = 23)
    private String sampleName;

    @ExcelProperty(value = "试验参与者编号", index = 24)
    private String subjectNo;

    @ExcelProperty(value = "性别", index = 25)
    private String sex;

    @ExcelProperty(value = "出生年份", index = 26)
    private String birthYear;

    @ExcelProperty(value = "临床诊断", index = 27)
    private String diseaseType;

    @ExcelProperty(value = "访视周期", index = 28)
    private String visitCycle;

    @ExcelProperty(value = "产品编码", index = 29)
    private String productCode;

    @ExcelProperty(value = "产品名称", index = 30)
    private String productName;

    @ExcelProperty(value = "样本类型", index = 31)
    private String sampleType;

    @ExcelProperty(value = "样本数量", index = 32)
    private String sampleQuantity;

    @ExcelProperty(value = "数量单位", index = 33)
    private String quantityUnit;

    @ExcelProperty(value = "样本属性", index = 34)
    private String sampleAttribute;

    @ExcelProperty(value = "取材部位", index = 35)
    private String collectionSite;

    @ExcelProperty(value = "组织类型", index = 36)
    private String tissueType;

    @ExcelProperty(value = "肿瘤类型", index = 37)
    private String tumorType;

    @ExcelProperty(value = "原发部位", index = 38)
    private String primaryTumorSite;

    @ExcelProperty(value = "采集日期", index = 39)
    private String collectionDate;

    @ExcelProperty(value = "检测方式", index = 40)
    private String assayMethod;

    @ExcelProperty(value = "备注", index = 41)
    private String remark;

    @ExcelProperty(value = "异常备注", index = 42)
    private String abnormalRemark;

    @ExcelProperty(value = "管控类型", index = 43)
    private String controlType;

    @ExcelProperty(value = "到达时间", index = 44)
    private String arrivalTime;
}
