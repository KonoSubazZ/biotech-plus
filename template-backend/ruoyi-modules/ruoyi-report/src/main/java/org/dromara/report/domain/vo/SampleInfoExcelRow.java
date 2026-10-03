package org.dromara.report.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * Excel 导入「样本信息」时的行对象，同时也是「导入模板」的表头定义。
 * <p>
 * 18 列与 sample_file 的业务字段一一对应，列顺序即模板顺序。
 * 读的时候按 {@code index}（列序号）匹配，不依赖表头文案 —— 用户手上的表格表头写法五花八门，
 * 按序号读更不容易读空（同一口径见 project 模块的基因导入）；{@code value} 只用于生成模板表头。
 * <p>
 * 日期类列（出生日期/接收日期/委托日期）在上游是字符串，模板里按 {@code yyyy-MM-dd} 填写。
 *
 * @author <你的名字>
 */
@Data
public class SampleInfoExcelRow {

    /** 样本编号（必需，导入时的去重键） */
    @ExcelProperty(value = "样本编号", index = 0)
    private String subbarcode;

    /** 条码 */
    @ExcelProperty(value = "条码", index = 1)
    private String barcode;

    /** 患者编号 */
    @ExcelProperty(value = "患者编号", index = 2)
    private String patientId;

    /** 患者姓名 */
    @ExcelProperty(value = "患者姓名", index = 3)
    private String personName;

    /** 性别 */
    @ExcelProperty(value = "性别", index = 4)
    private String gender;

    /** 出生日期 */
    @ExcelProperty(value = "出生日期", index = 5)
    private String birthday;

    /** 年龄 */
    @ExcelProperty(value = "年龄", index = 6)
    private String age;

    /** 患者电话 */
    @ExcelProperty(value = "患者电话", index = 7)
    private String patientPhone;

    /** 医院 */
    @ExcelProperty(value = "医院", index = 8)
    private String hospital;

    /** 接收日期 */
    @ExcelProperty(value = "接收日期", index = 9)
    private String receivedDate;

    /** 样本类型 */
    @ExcelProperty(value = "样本类型", index = 10)
    private String specimenType;

    /** 样本数量 */
    @ExcelProperty(value = "样本数量", index = 11)
    private String specimenQuantity;

    /** 检测方案 */
    @ExcelProperty(value = "检测方案", index = 12)
    private String testingProgram;

    /** 疾病类型（录单癌种） */
    @ExcelProperty(value = "疾病类型", index = 13)
    private String diseaseType;

    /** 客户 */
    @ExcelProperty(value = "客户", index = 14)
    private String client;

    /** 委托日期 */
    @ExcelProperty(value = "委托日期", index = 15)
    private String commissionDate;

    /** 产品名称（录单产品） */
    @ExcelProperty(value = "产品名称", index = 16)
    private String productName;

    /** 备注 */
    @ExcelProperty(value = "备注", index = 17)
    private String remark;
}
