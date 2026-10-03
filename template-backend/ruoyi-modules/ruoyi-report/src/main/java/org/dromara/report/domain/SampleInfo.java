package org.dromara.report.domain;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 样本信息对象 sample_file
 * <p>
 * 字段按生产库的「录单样本表」<b>全量 122 列</b>搬过来（源表 myapp_webcrmsample，
 * 见 ~/project/biotech/sql/biotech-structure-20261002.sql），列名只做了大小写/下划线规范化：
 * 源表列名写在每个字段的注释里，便于与原表逐列对照。
 * 源表列没有 COMMENT，所以除少数能确定含义的列外，中文名留空、以源列名为准。
 * <p>
 * 注意：实体只用于持久化，不直接作为接口返回值（用 SampleInfoVo）。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sample_file")
public class SampleInfo extends BaseEntity {

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    // @fields:start
    /** 年龄（源表列 AGE） */
    private String age;

    /** 样本编号（源表列 BARCODE） */
    private String barcode;

    /** 床位（源表列 BED） */
    private String bed;

    /** 出生日期（源表列 BIRTHDAY） */
    private String birthDay;

    /** 源表列 BIRTHPLACE */
    private String birthplace;

    /** 录单癌种（源表列 CANCERTYPE） */
    private String cancerType;

    /** 临床备注（源表列 CLINICALREMARK） */
    private String clinicalRemark;

    /** 临床分期（源表列 CLINICALSTAGES） */
    private String clinicalStages;

    /** 收样日期（源表列 COLLECTDATE） */
    private String collectDate;

    /** 客户名称（源表列 CUSTOMEDESC） */
    private String customDesc;

    /** 客户（源表列 CUSTOMERNAME） */
    private String customerName;

    /** 科室编码（源表列 DEPARTMENTCODE） */
    private String departmentCode;

    /** 医生姓名（源表列 DOCTORNAME） */
    private String doctorName;

    /** 邮箱（源表列 EMAILADDRESS） */
    private String emailAddress;

    /** 委托日期（源表列 ENTERDATE） */
    private String enterDate;

    /** ERP销售（源表列 ERPSALERNAME） */
    private String erpSalerName;

    /** 录单产品（源表列 ERPTESTNAME） */
    private String erpTestName;

    /** 一级亲属患癌情况（源表列 FAMILYFIRST） */
    private String familyFirst;

    /** 一级亲属年龄（源表列 FAMILYFIRST_AGE） */
    private String familyFirstAge;

    /** 一级亲属癌种（源表列 FAMILYFIRST_CANCERTYPE） */
    private String familyFirstCancerType;

    /** 一级亲属确诊时间（源表列 FAMILYFIRST_CONFIRMTIME） */
    private String familyFirstConfirmTime;

    /** 二级亲属患癌情况（源表列 FAMILYSECOND） */
    private String familySecond;

    /** 二级亲属年龄（源表列 FAMILYSECOND_AGE） */
    private String familySecondAge;

    /** 二级亲属癌种（源表列 FAMILYSECOND_CANCERTYPE） */
    private String familySecondCancerType;

    /** 二级亲属确诊时间（源表列 FAMILYSECOND_CONFIRMTIME） */
    private String familySecondConfirmTime;

    /** 加急编号（源表列 FASTCODE） */
    private String fastCode;

    /** 第一次治疗史（源表列 FIRSTTREATMENT） */
    private String firstTreatment;

    /** 取材部位（源表列 FROMORGAN） */
    private String fromOrgan;

    /** 基因检测结果（源表列 GENERESULT） */
    private String geneResult;

    /** 基因型（源表列 GENETYPE） */
    private String geneType;

    /** 取材日期（源表列 GETSPECDATE） */
    private String getSpecDate;

    /** 文库编号（源表列 LIBRARYNAME） */
    private String libraryName;

    /** 病区（源表列 LOCATIONNAME） */
    private String locationName;

    /** 邮寄地址（源表列 MAILINGADDRESS） */
    private String mailingAddress;

    /** 经理邮箱（源表列 MANAGEREMAIL） */
    private String managerEmail;

    /** 订单编号（源表列 ORDERCODE） */
    private String orderCode;

    /** 门诊号（源表列 OUTPATIENT） */
    private String outpatient;

    /** 病理类型（源表列 PATHOLOGICALTYPE） */
    private String pathologicalType;

    /** 病理号（源表列 PATHOLOGYNUM） */
    private String pathologyNum;

    /** 姓名（源表列 PATIENTNAME） */
    private String patientName;

    /** 患者电话（源表列 PATIENTPHONE） */
    private String patientPhone;

    /** 收款金额（源表列 PAYAMOUNT） */
    private String payAmount;

    /** 患者编号（源表列 PCODE） */
    private String pcode;

    /** 项目经理邮箱（源表列 PMEMAIL） */
    private String pmEmail;

    /** 报告接收人电话（源表列 RECEIVERTELEPHONE） */
    private String receiverTelephone;

    /** 录单人（源表列 RECORDER） */
    private String recorder;

    /** 录单人编码（源表列 RECORDERCODE） */
    private String recorderCode;

    /** 报告接收人（源表列 REPORTRECEIVER） */
    private String reportReceiver;

    /** 房间（源表列 ROOM） */
    private String room;

    /** 销售邮箱(ERP)（源表列 SALEREMAIL） */
    private String salerEmail;

    /** 样本备注（源表列 SAMPLEREMARK） */
    private String sampleRemark;

    /** 样本来源（源表列 SAMPLESOURCE） */
    private String sampleSource;

    /** 采样日期（源表列 SAMPLETIME） */
    private String sampleTime;

    /** 样本类型（源表列 SAMPLETYPE） */
    private String sampleType;

    /** 第二次治疗史（源表列 SECONDTREATMENT） */
    private String secondTreatment;

    /** 寄出日期（源表列 SENDDATE） */
    private String sendDate;

    /** 性别（源表列 SEX） */
    private String sex;

    /** 标本编号（源表列 SPECIMENNO） */
    private String specimenNo;

    /** 样本数量（源表列 SPECIMENNUM） */
    private String specimenNum;

    /** 支持邮箱（源表列 SUPPORTEMAIL） */
    private String supportEmail;

    /** 第三次治疗史（源表列 THIRDTREATMENT） */
    private String thirdTreatment;

    /** 单位（源表列 UNIT） */
    private String unit;

    /** 接诊医生邮箱（源表列 admissiondoctoremail） */
    private String admissionDoctorEmail;

    /** 接诊医生电话（源表列 admissiondoctorphone） */
    private String admissionDoctorPhone;

    /** 就诊医院（源表列 admissionhospital） */
    private String admissionHospital;

    /** 癌种1（源表列 cancertype1） */
    // MyBatis-Plus 的驼峰转下划线在大写字母前插下划线、数字前不插，
    // cancerType1 会被推导成 cancer_type1，这里的列名是 cancer_type_1，所以显式指定列名
    @TableField("cancer_type_1")
    private String cancerType1;

    /** 癌种编码（源表列 cancertypecode） */
    private String cancerTypeCode;

    /** 合同名称（源表列 contractname） */
    private String contractName;

    /** 合同编号（源表列 contractsno） */
    private String contractsNo;

    /** 单位名称（源表列 corpdesc） */
    private String corpDesc;

    /** 单位编号（源表列 corpno） */
    private String corpNo;

    /** 客户类型（源表列 customertype） */
    private String customerType;

    /** 检测方法（源表列 detectionmethod） */
    private String detectionMethod;

    /** 检测时间（源表列 detectiontime） */
    private String detectionTime;

    /** 快递公司（源表列 expressname） */
    private String expressName;

    /** 快递单号（源表列 expressno） */
    private String expressNo;

    /** 二级亲属患癌情况（源表列 familykinshiptwocancer） */
    private String familyKinshipTwoCancer;

    /** 第一次治疗用药方案（源表列 firsttreatmentdrugregimen） */
    private String firstTreatmentDrugRegimen;

    /** 第一次治疗时长（源表列 firsttreatmentduration） */
    private String firstTreatmentDuration;

    /** 第一次治疗疗效（源表列 firsttreatmenteffect） */
    private String firstTreatmentEffect;

    /** 第一次治疗方式（源表列 firsttreatmentmethod） */
    private String firstTreatmentMethod;

    /** 第一次治疗时间（源表列 firsttreatmenttime） */
    private String firstTreatmentTime;

    /** 实验室（源表列 laboratoryname） */
    private String laboratoryName;

    /** 运营负责人编码（源表列 operatemanagercode） */
    private String operateManagerCode;

    /** 运营负责人（源表列 operatemanagerdesc） */
    private String operateManagerDesc;

    /** 运营负责人邮箱（源表列 operatemanageremail） */
    private String operateManagerEmail;

    /** 订单金额（源表列 ordermoney） */
    private String orderMoney;

    /** 其它附件（源表列 otherattachments） */
    private String otherAttachments;

    /** 病理报告（源表列 pathologyreport） */
    private String pathologyReport;

    /** 患者信息邮箱（源表列 patientinfoemail） */
    private String patientInfoEmail;

    /** 患者是否肿瘤（源表列 patientinfoisacancer） */
    private String patientInfoIsCancer;

    /** 收款完成日期（源表列 payfinishdate） */
    private String payFinishDate;

    /** 录单单位（源表列 recorderdesc） */
    private String recorderDesc;

    /** 销售（源表列 salesman） */
    private String salesMan;

    /** 销售编码（源表列 salesmancode） */
    private String salesManCode;

    /** 销售邮箱（源表列 salesmanemail） */
    private String salesManEmail;

    /** 送样地址（源表列 sampleaddress） */
    private String sampleAddress;

    /** 送样联系人（源表列 samplecontactdesc） */
    private String sampleContactDesc;

    /** 送样联系电话（源表列 samplecontactphone） */
    private String sampleContactPhone;

    /** 数量单位（源表列 samplenumunit） */
    private String sampleNumUnit;

    /** 产品编码（源表列 sampleproductcode） */
    private String sampleProductCode;

    /** 第二次治疗用药方案（源表列 secondtreatmentdrugregimen） */
    private String secondTreatmentDrugRegimen;

    /** 第二次治疗时长（源表列 secondtreatmentduration） */
    private String secondTreatmentDuration;

    /** 第二次治疗疗效（源表列 secondtreatmenteffect） */
    private String secondTreatmentEffect;

    /** 第二次治疗方式（源表列 secondtreatmentmethod） */
    private String secondTreatmentMethod;

    /** 第二次治疗时间（源表列 secondtreatmenttime） */
    private String secondTreatmentTime;

    /** 流水号（源表列 serialnumber） */
    private String serialNumber;

    /** 具体癌种（源表列 specificcancer） */
    private String specificCancer;

    /** 送检机构（源表列 testinginstitution） */
    private String testingInstitution;

    /** 第三次治疗用药方案（源表列 thirdtreatmentdrugregimen） */
    private String thirdTreatmentDrugRegimen;

    /** 第三次治疗时长（源表列 thirdtreatmentduration） */
    private String thirdTreatmentDuration;

    /** 第三次治疗疗效（源表列 thirdtreatmenteffect） */
    private String thirdTreatmentEffect;

    /** 第三次治疗方式（源表列 thirdtreatmentmethod） */
    private String thirdTreatmentMethod;

    /** 第三次治疗时间（源表列 thirdtreatmenttime） */
    private String thirdTreatmentTime;

    /** 科室名称（源表列 DEPARTMENTDESC） */
    private String departmentDesc;

    /** 异常备注（源表列 ABNORMALREMARK） */
    private String abnormalRemark;

    /** 结算渠道（源表列 CHECKOUTLOGIC） */
    private String checkoutLogic;

    /** 到样日期（源表列 DYDATE） */
    private String dyDate;

    /** 账期(天)（源表列 PAYMENTDATE） */
    private String paymentDate;

    /** 样本属性（源表列 SAMPLEATTRIBUTE） */
    private String sampleAttribute;

    /** 签约时间（源表列 SIGNTIME） */
    private String signTime;

    /** 冻结状态（源表列 BLOCK） */
    private String block;
    // @fields:end

    /** 逻辑删除标志 */
    @TableLogic
    private String delFlag;
}
