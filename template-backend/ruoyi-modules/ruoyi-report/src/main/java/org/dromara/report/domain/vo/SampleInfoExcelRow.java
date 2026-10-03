package org.dromara.report.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.report.domain.SampleInfo;

/**
 * Excel 导入「样本信息」时的行对象，同时也是「导入模板」的表头定义。
 * <p>
 * <b>列顺序与源表 122 列一致，表头直接写源表列名</b>（如 PATIENTNAME）——
 * 这样从录单系统（myapp_webcrmsample）导出的表格可以原样导入，不用先改名。
 * 读的时候按 {@code index}（列序号）匹配，不看表头文案，所以表头行改不改都能读。
 * <p>
 * mapstruct-plus：字段名与 SampleInfo 一一对应，导入时直接
 * {@code MapstructUtils.convert(row, SampleInfo.class)}，不用手写 122 个 setter。
 *
 * @author <你的名字>
 */
@Data
@AutoMapper(target = SampleInfo.class, reverseConvertGenerate = false)
public class SampleInfoExcelRow {

    /** 年龄 */
    @ExcelProperty(value = "AGE", index = 0)
    private String age;
    /** 样本编号 */
    @ExcelProperty(value = "BARCODE", index = 1)
    private String barcode;
    /** 床位 */
    @ExcelProperty(value = "BED", index = 2)
    private String bed;
    /** 出生日期 */
    @ExcelProperty(value = "BIRTHDAY", index = 3)
    private String birthDay;
    @ExcelProperty(value = "BIRTHPLACE", index = 4)
    private String birthplace;
    /** 录单癌种 */
    @ExcelProperty(value = "CANCERTYPE", index = 5)
    private String cancerType;
    /** 临床备注 */
    @ExcelProperty(value = "CLINICALREMARK", index = 6)
    private String clinicalRemark;
    /** 临床分期 */
    @ExcelProperty(value = "CLINICALSTAGES", index = 7)
    private String clinicalStages;
    /** 收样日期 */
    @ExcelProperty(value = "COLLECTDATE", index = 8)
    private String collectDate;
    /** 客户名称 */
    @ExcelProperty(value = "CUSTOMEDESC", index = 9)
    private String customDesc;
    /** 客户 */
    @ExcelProperty(value = "CUSTOMERNAME", index = 10)
    private String customerName;
    /** 科室编码 */
    @ExcelProperty(value = "DEPARTMENTCODE", index = 11)
    private String departmentCode;
    /** 医生姓名 */
    @ExcelProperty(value = "DOCTORNAME", index = 12)
    private String doctorName;
    /** 邮箱 */
    @ExcelProperty(value = "EMAILADDRESS", index = 13)
    private String emailAddress;
    /** 委托日期 */
    @ExcelProperty(value = "ENTERDATE", index = 14)
    private String enterDate;
    /** ERP销售 */
    @ExcelProperty(value = "ERPSALERNAME", index = 15)
    private String erpSalerName;
    /** 录单产品 */
    @ExcelProperty(value = "ERPTESTNAME", index = 16)
    private String erpTestName;
    /** 一级亲属患癌情况 */
    @ExcelProperty(value = "FAMILYFIRST", index = 17)
    private String familyFirst;
    /** 一级亲属年龄 */
    @ExcelProperty(value = "FAMILYFIRST_AGE", index = 18)
    private String familyFirstAge;
    /** 一级亲属癌种 */
    @ExcelProperty(value = "FAMILYFIRST_CANCERTYPE", index = 19)
    private String familyFirstCancerType;
    /** 一级亲属确诊时间 */
    @ExcelProperty(value = "FAMILYFIRST_CONFIRMTIME", index = 20)
    private String familyFirstConfirmTime;
    /** 二级亲属患癌情况 */
    @ExcelProperty(value = "FAMILYSECOND", index = 21)
    private String familySecond;
    /** 二级亲属年龄 */
    @ExcelProperty(value = "FAMILYSECOND_AGE", index = 22)
    private String familySecondAge;
    /** 二级亲属癌种 */
    @ExcelProperty(value = "FAMILYSECOND_CANCERTYPE", index = 23)
    private String familySecondCancerType;
    /** 二级亲属确诊时间 */
    @ExcelProperty(value = "FAMILYSECOND_CONFIRMTIME", index = 24)
    private String familySecondConfirmTime;
    /** 加急编号 */
    @ExcelProperty(value = "FASTCODE", index = 25)
    private String fastCode;
    /** 第一次治疗史 */
    @ExcelProperty(value = "FIRSTTREATMENT", index = 26)
    private String firstTreatment;
    /** 取材部位 */
    @ExcelProperty(value = "FROMORGAN", index = 27)
    private String fromOrgan;
    /** 基因检测结果 */
    @ExcelProperty(value = "GENERESULT", index = 28)
    private String geneResult;
    /** 基因型 */
    @ExcelProperty(value = "GENETYPE", index = 29)
    private String geneType;
    /** 取材日期 */
    @ExcelProperty(value = "GETSPECDATE", index = 30)
    private String getSpecDate;
    /** 文库编号 */
    @ExcelProperty(value = "LIBRARYNAME", index = 31)
    private String libraryName;
    /** 病区 */
    @ExcelProperty(value = "LOCATIONNAME", index = 32)
    private String locationName;
    /** 邮寄地址 */
    @ExcelProperty(value = "MAILINGADDRESS", index = 33)
    private String mailingAddress;
    /** 经理邮箱 */
    @ExcelProperty(value = "MANAGEREMAIL", index = 34)
    private String managerEmail;
    /** 订单编号 */
    @ExcelProperty(value = "ORDERCODE", index = 35)
    private String orderCode;
    /** 门诊号 */
    @ExcelProperty(value = "OUTPATIENT", index = 36)
    private String outpatient;
    /** 病理类型 */
    @ExcelProperty(value = "PATHOLOGICALTYPE", index = 37)
    private String pathologicalType;
    /** 病理号 */
    @ExcelProperty(value = "PATHOLOGYNUM", index = 38)
    private String pathologyNum;
    /** 姓名 */
    @ExcelProperty(value = "PATIENTNAME", index = 39)
    private String patientName;
    /** 患者电话 */
    @ExcelProperty(value = "PATIENTPHONE", index = 40)
    private String patientPhone;
    /** 收款金额 */
    @ExcelProperty(value = "PAYAMOUNT", index = 41)
    private String payAmount;
    /** 患者编号 */
    @ExcelProperty(value = "PCODE", index = 42)
    private String pcode;
    /** 项目经理邮箱 */
    @ExcelProperty(value = "PMEMAIL", index = 43)
    private String pmEmail;
    /** 报告接收人电话 */
    @ExcelProperty(value = "RECEIVERTELEPHONE", index = 44)
    private String receiverTelephone;
    /** 录单人 */
    @ExcelProperty(value = "RECORDER", index = 45)
    private String recorder;
    /** 录单人编码 */
    @ExcelProperty(value = "RECORDERCODE", index = 46)
    private String recorderCode;
    /** 报告接收人 */
    @ExcelProperty(value = "REPORTRECEIVER", index = 47)
    private String reportReceiver;
    /** 房间 */
    @ExcelProperty(value = "ROOM", index = 48)
    private String room;
    /** 销售邮箱(ERP) */
    @ExcelProperty(value = "SALEREMAIL", index = 49)
    private String salerEmail;
    /** 样本备注 */
    @ExcelProperty(value = "SAMPLEREMARK", index = 50)
    private String sampleRemark;
    /** 样本来源 */
    @ExcelProperty(value = "SAMPLESOURCE", index = 51)
    private String sampleSource;
    /** 采样日期 */
    @ExcelProperty(value = "SAMPLETIME", index = 52)
    private String sampleTime;
    /** 样本类型 */
    @ExcelProperty(value = "SAMPLETYPE", index = 53)
    private String sampleType;
    /** 第二次治疗史 */
    @ExcelProperty(value = "SECONDTREATMENT", index = 54)
    private String secondTreatment;
    /** 寄出日期 */
    @ExcelProperty(value = "SENDDATE", index = 55)
    private String sendDate;
    /** 性别 */
    @ExcelProperty(value = "SEX", index = 56)
    private String sex;
    /** 标本编号 */
    @ExcelProperty(value = "SPECIMENNO", index = 57)
    private String specimenNo;
    /** 样本数量 */
    @ExcelProperty(value = "SPECIMENNUM", index = 58)
    private String specimenNum;
    /** 支持邮箱 */
    @ExcelProperty(value = "SUPPORTEMAIL", index = 59)
    private String supportEmail;
    /** 第三次治疗史 */
    @ExcelProperty(value = "THIRDTREATMENT", index = 60)
    private String thirdTreatment;
    /** 单位 */
    @ExcelProperty(value = "UNIT", index = 61)
    private String unit;
    /** 接诊医生邮箱 */
    @ExcelProperty(value = "admissiondoctoremail", index = 62)
    private String admissionDoctorEmail;
    /** 接诊医生电话 */
    @ExcelProperty(value = "admissiondoctorphone", index = 63)
    private String admissionDoctorPhone;
    /** 就诊医院 */
    @ExcelProperty(value = "admissionhospital", index = 64)
    private String admissionHospital;
    /** 癌种1 */
    @ExcelProperty(value = "cancertype1", index = 65)
    private String cancerType1;
    /** 癌种编码 */
    @ExcelProperty(value = "cancertypecode", index = 66)
    private String cancerTypeCode;
    /** 合同名称 */
    @ExcelProperty(value = "contractname", index = 67)
    private String contractName;
    /** 合同编号 */
    @ExcelProperty(value = "contractsno", index = 68)
    private String contractsNo;
    /** 单位名称 */
    @ExcelProperty(value = "corpdesc", index = 69)
    private String corpDesc;
    /** 单位编号 */
    @ExcelProperty(value = "corpno", index = 70)
    private String corpNo;
    /** 客户类型 */
    @ExcelProperty(value = "customertype", index = 71)
    private String customerType;
    /** 检测方法 */
    @ExcelProperty(value = "detectionmethod", index = 72)
    private String detectionMethod;
    /** 检测时间 */
    @ExcelProperty(value = "detectiontime", index = 73)
    private String detectionTime;
    /** 快递公司 */
    @ExcelProperty(value = "expressname", index = 74)
    private String expressName;
    /** 快递单号 */
    @ExcelProperty(value = "expressno", index = 75)
    private String expressNo;
    /** 二级亲属患癌情况 */
    @ExcelProperty(value = "familykinshiptwocancer", index = 76)
    private String familyKinshipTwoCancer;
    /** 第一次治疗用药方案 */
    @ExcelProperty(value = "firsttreatmentdrugregimen", index = 77)
    private String firstTreatmentDrugRegimen;
    /** 第一次治疗时长 */
    @ExcelProperty(value = "firsttreatmentduration", index = 78)
    private String firstTreatmentDuration;
    /** 第一次治疗疗效 */
    @ExcelProperty(value = "firsttreatmenteffect", index = 79)
    private String firstTreatmentEffect;
    /** 第一次治疗方式 */
    @ExcelProperty(value = "firsttreatmentmethod", index = 80)
    private String firstTreatmentMethod;
    /** 第一次治疗时间 */
    @ExcelProperty(value = "firsttreatmenttime", index = 81)
    private String firstTreatmentTime;
    /** 实验室 */
    @ExcelProperty(value = "laboratoryname", index = 82)
    private String laboratoryName;
    /** 运营负责人编码 */
    @ExcelProperty(value = "operatemanagercode", index = 83)
    private String operateManagerCode;
    /** 运营负责人 */
    @ExcelProperty(value = "operatemanagerdesc", index = 84)
    private String operateManagerDesc;
    /** 运营负责人邮箱 */
    @ExcelProperty(value = "operatemanageremail", index = 85)
    private String operateManagerEmail;
    /** 订单金额 */
    @ExcelProperty(value = "ordermoney", index = 86)
    private String orderMoney;
    /** 其它附件 */
    @ExcelProperty(value = "otherattachments", index = 87)
    private String otherAttachments;
    /** 病理报告 */
    @ExcelProperty(value = "pathologyreport", index = 88)
    private String pathologyReport;
    /** 患者信息邮箱 */
    @ExcelProperty(value = "patientinfoemail", index = 89)
    private String patientInfoEmail;
    /** 患者是否肿瘤 */
    @ExcelProperty(value = "patientinfoisacancer", index = 90)
    private String patientInfoIsCancer;
    /** 收款完成日期 */
    @ExcelProperty(value = "payfinishdate", index = 91)
    private String payFinishDate;
    /** 录单单位 */
    @ExcelProperty(value = "recorderdesc", index = 92)
    private String recorderDesc;
    /** 销售 */
    @ExcelProperty(value = "salesman", index = 93)
    private String salesMan;
    /** 销售编码 */
    @ExcelProperty(value = "salesmancode", index = 94)
    private String salesManCode;
    /** 销售邮箱 */
    @ExcelProperty(value = "salesmanemail", index = 95)
    private String salesManEmail;
    /** 送样地址 */
    @ExcelProperty(value = "sampleaddress", index = 96)
    private String sampleAddress;
    /** 送样联系人 */
    @ExcelProperty(value = "samplecontactdesc", index = 97)
    private String sampleContactDesc;
    /** 送样联系电话 */
    @ExcelProperty(value = "samplecontactphone", index = 98)
    private String sampleContactPhone;
    /** 数量单位 */
    @ExcelProperty(value = "samplenumunit", index = 99)
    private String sampleNumUnit;
    /** 产品编码 */
    @ExcelProperty(value = "sampleproductcode", index = 100)
    private String sampleProductCode;
    /** 第二次治疗用药方案 */
    @ExcelProperty(value = "secondtreatmentdrugregimen", index = 101)
    private String secondTreatmentDrugRegimen;
    /** 第二次治疗时长 */
    @ExcelProperty(value = "secondtreatmentduration", index = 102)
    private String secondTreatmentDuration;
    /** 第二次治疗疗效 */
    @ExcelProperty(value = "secondtreatmenteffect", index = 103)
    private String secondTreatmentEffect;
    /** 第二次治疗方式 */
    @ExcelProperty(value = "secondtreatmentmethod", index = 104)
    private String secondTreatmentMethod;
    /** 第二次治疗时间 */
    @ExcelProperty(value = "secondtreatmenttime", index = 105)
    private String secondTreatmentTime;
    /** 流水号 */
    @ExcelProperty(value = "serialnumber", index = 106)
    private String serialNumber;
    /** 具体癌种 */
    @ExcelProperty(value = "specificcancer", index = 107)
    private String specificCancer;
    /** 送检机构 */
    @ExcelProperty(value = "testinginstitution", index = 108)
    private String testingInstitution;
    /** 第三次治疗用药方案 */
    @ExcelProperty(value = "thirdtreatmentdrugregimen", index = 109)
    private String thirdTreatmentDrugRegimen;
    /** 第三次治疗时长 */
    @ExcelProperty(value = "thirdtreatmentduration", index = 110)
    private String thirdTreatmentDuration;
    /** 第三次治疗疗效 */
    @ExcelProperty(value = "thirdtreatmenteffect", index = 111)
    private String thirdTreatmentEffect;
    /** 第三次治疗方式 */
    @ExcelProperty(value = "thirdtreatmentmethod", index = 112)
    private String thirdTreatmentMethod;
    /** 第三次治疗时间 */
    @ExcelProperty(value = "thirdtreatmenttime", index = 113)
    private String thirdTreatmentTime;
    /** 科室名称 */
    @ExcelProperty(value = "DEPARTMENTDESC", index = 114)
    private String departmentDesc;
    /** 异常备注 */
    @ExcelProperty(value = "ABNORMALREMARK", index = 115)
    private String abnormalRemark;
    /** 结算渠道 */
    @ExcelProperty(value = "CHECKOUTLOGIC", index = 116)
    private String checkoutLogic;
    /** 到样日期 */
    @ExcelProperty(value = "DYDATE", index = 117)
    private String dyDate;
    /** 账期(天) */
    @ExcelProperty(value = "PAYMENTDATE", index = 118)
    private String paymentDate;
    /** 样本属性 */
    @ExcelProperty(value = "SAMPLEATTRIBUTE", index = 119)
    private String sampleAttribute;
    /** 签约时间 */
    @ExcelProperty(value = "SIGNTIME", index = 120)
    private String signTime;
    /** 冻结状态 */
    @ExcelProperty(value = "BLOCK", index = 121)
    private String block;
}
