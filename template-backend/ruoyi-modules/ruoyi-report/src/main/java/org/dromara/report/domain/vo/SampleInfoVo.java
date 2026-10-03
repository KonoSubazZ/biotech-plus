package org.dromara.report.domain.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.report.domain.SampleInfo;

/**
 * 样本信息视图对象 sample_file
 *
 * @author <你的名字>
 */
@Data
@NoArgsConstructor
// mapstruct-plus：生成 Vo ↔ 实体的双向转换器（查询结果 Entity → Vo 靠它）
@AutoMapper(target = SampleInfo.class)
public class SampleInfoVo {

    /** 主键 */
    private Long id;

    // @fields:start
    /** 年龄 */
    private String age;

    /** 样本编号 */
    private String barcode;

    /** 床位 */
    private String bed;

    /** 出生日期 */
    private String birthDay;

    /** 源表列 BIRTHPLACE */
    private String birthplace;

    /** 录单癌种 */
    private String cancerType;

    /** 临床备注 */
    private String clinicalRemark;

    /** 临床分期 */
    private String clinicalStages;

    /** 收样日期 */
    private String collectDate;

    /** 客户名称 */
    private String customDesc;

    /** 客户 */
    private String customerName;

    /** 科室编码 */
    private String departmentCode;

    /** 医生姓名 */
    private String doctorName;

    /** 邮箱 */
    private String emailAddress;

    /** 委托日期 */
    private String enterDate;

    /** ERP销售 */
    private String erpSalerName;

    /** 录单产品 */
    private String erpTestName;

    /** 一级亲属患癌情况 */
    private String familyFirst;

    /** 一级亲属年龄 */
    private String familyFirstAge;

    /** 一级亲属癌种 */
    private String familyFirstCancerType;

    /** 一级亲属确诊时间 */
    private String familyFirstConfirmTime;

    /** 二级亲属患癌情况 */
    private String familySecond;

    /** 二级亲属年龄 */
    private String familySecondAge;

    /** 二级亲属癌种 */
    private String familySecondCancerType;

    /** 二级亲属确诊时间 */
    private String familySecondConfirmTime;

    /** 加急编号 */
    private String fastCode;

    /** 第一次治疗史 */
    private String firstTreatment;

    /** 取材部位 */
    private String fromOrgan;

    /** 基因检测结果 */
    private String geneResult;

    /** 基因型 */
    private String geneType;

    /** 取材日期 */
    private String getSpecDate;

    /** 文库编号 */
    private String libraryName;

    /** 病区 */
    private String locationName;

    /** 邮寄地址 */
    private String mailingAddress;

    /** 经理邮箱 */
    private String managerEmail;

    /** 订单编号 */
    private String orderCode;

    /** 门诊号 */
    private String outpatient;

    /** 病理类型 */
    private String pathologicalType;

    /** 病理号 */
    private String pathologyNum;

    /** 姓名 */
    private String patientName;

    /** 患者电话 */
    private String patientPhone;

    /** 收款金额 */
    private String payAmount;

    /** 患者编号 */
    private String pcode;

    /** 项目经理邮箱 */
    private String pmEmail;

    /** 报告接收人电话 */
    private String receiverTelephone;

    /** 录单人 */
    private String recorder;

    /** 录单人编码 */
    private String recorderCode;

    /** 报告接收人 */
    private String reportReceiver;

    /** 房间 */
    private String room;

    /** 销售邮箱(ERP) */
    private String salerEmail;

    /** 样本备注 */
    private String sampleRemark;

    /** 样本来源 */
    private String sampleSource;

    /** 采样日期 */
    private String sampleTime;

    /** 样本类型 */
    private String sampleType;

    /** 第二次治疗史 */
    private String secondTreatment;

    /** 寄出日期 */
    private String sendDate;

    /** 性别 */
    private String sex;

    /** 标本编号 */
    private String specimenNo;

    /** 样本数量 */
    private String specimenNum;

    /** 支持邮箱 */
    private String supportEmail;

    /** 第三次治疗史 */
    private String thirdTreatment;

    /** 单位 */
    private String unit;

    /** 接诊医生邮箱 */
    private String admissionDoctorEmail;

    /** 接诊医生电话 */
    private String admissionDoctorPhone;

    /** 就诊医院 */
    private String admissionHospital;

    /** 癌种1 */
    private String cancerType1;

    /** 癌种编码 */
    private String cancerTypeCode;

    /** 合同名称 */
    private String contractName;

    /** 合同编号 */
    private String contractsNo;

    /** 单位名称 */
    private String corpDesc;

    /** 单位编号 */
    private String corpNo;

    /** 客户类型 */
    private String customerType;

    /** 检测方法 */
    private String detectionMethod;

    /** 检测时间 */
    private String detectionTime;

    /** 快递公司 */
    private String expressName;

    /** 快递单号 */
    private String expressNo;

    /** 二级亲属患癌情况 */
    private String familyKinshipTwoCancer;

    /** 第一次治疗用药方案 */
    private String firstTreatmentDrugRegimen;

    /** 第一次治疗时长 */
    private String firstTreatmentDuration;

    /** 第一次治疗疗效 */
    private String firstTreatmentEffect;

    /** 第一次治疗方式 */
    private String firstTreatmentMethod;

    /** 第一次治疗时间 */
    private String firstTreatmentTime;

    /** 实验室 */
    private String laboratoryName;

    /** 运营负责人编码 */
    private String operateManagerCode;

    /** 运营负责人 */
    private String operateManagerDesc;

    /** 运营负责人邮箱 */
    private String operateManagerEmail;

    /** 订单金额 */
    private String orderMoney;

    /** 其它附件 */
    private String otherAttachments;

    /** 病理报告 */
    private String pathologyReport;

    /** 患者信息邮箱 */
    private String patientInfoEmail;

    /** 患者是否肿瘤 */
    private String patientInfoIsCancer;

    /** 收款完成日期 */
    private String payFinishDate;

    /** 录单单位 */
    private String recorderDesc;

    /** 销售 */
    private String salesMan;

    /** 销售编码 */
    private String salesManCode;

    /** 销售邮箱 */
    private String salesManEmail;

    /** 送样地址 */
    private String sampleAddress;

    /** 送样联系人 */
    private String sampleContactDesc;

    /** 送样联系电话 */
    private String sampleContactPhone;

    /** 数量单位 */
    private String sampleNumUnit;

    /** 产品编码 */
    private String sampleProductCode;

    /** 第二次治疗用药方案 */
    private String secondTreatmentDrugRegimen;

    /** 第二次治疗时长 */
    private String secondTreatmentDuration;

    /** 第二次治疗疗效 */
    private String secondTreatmentEffect;

    /** 第二次治疗方式 */
    private String secondTreatmentMethod;

    /** 第二次治疗时间 */
    private String secondTreatmentTime;

    /** 流水号 */
    private String serialNumber;

    /** 具体癌种 */
    private String specificCancer;

    /** 送检机构 */
    private String testingInstitution;

    /** 第三次治疗用药方案 */
    private String thirdTreatmentDrugRegimen;

    /** 第三次治疗时长 */
    private String thirdTreatmentDuration;

    /** 第三次治疗疗效 */
    private String thirdTreatmentEffect;

    /** 第三次治疗方式 */
    private String thirdTreatmentMethod;

    /** 第三次治疗时间 */
    private String thirdTreatmentTime;

    /** 科室名称 */
    private String departmentDesc;

    /** 异常备注 */
    private String abnormalRemark;

    /** 结算渠道 */
    private String checkoutLogic;

    /** 到样日期 */
    private String dyDate;

    /** 账期(天) */
    private String paymentDate;

    /** 样本属性 */
    private String sampleAttribute;

    /** 签约时间 */
    private String signTime;

    /** 冻结状态 */
    private String block;

    // @fields:end

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
