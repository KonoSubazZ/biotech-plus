package org.dromara.report.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.report.domain.SampleInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 样本信息业务对象 sample_file
 * <p>
 * 122 个业务字段与实体一一对应（列名见 SampleInfo 的字段注释）；text 列不校验长度，
 * varchar 列按源表长度加 @Size。样本编号（barcode，源表列 BARCODE）是业务键，见 Service 的查重 / upsert。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
// mapstruct-plus：生成 Bo → 实体的转换器。Service 里 MapstructUtils.convert(bo, SampleInfo.class) 靠它，
// 少了这个注解，运行期会抛 ConvertException: cannot find converter from SampleInfoBo to SampleInfo
@AutoMapper(target = SampleInfo.class, reverseConvertGenerate = false)
public class SampleInfoBo extends BaseEntity {

    /** 主键；新增时为空，更新时必填 */
    private Long id;

    // @fields:start
    @Size(max = 50, message = "年龄长度不能超过 50")
    private String age;

    @NotBlank(message = "样本编号不能为空")
    @Size(max = 100, message = "样本编号长度不能超过 100")
    private String barcode;

    @Size(max = 100, message = "床位长度不能超过 100")
    private String bed;

    @Size(max = 100, message = "出生日期长度不能超过 100")
    private String birthDay;

    @Size(max = 100, message = "BIRTHPLACE长度不能超过 100")
    private String birthplace;

    @Size(max = 256, message = "录单癌种长度不能超过 256")
    private String cancerType;

    @Size(max = 100, message = "临床备注长度不能超过 100")
    private String clinicalRemark;

    @Size(max = 100, message = "临床分期长度不能超过 100")
    private String clinicalStages;

    @Size(max = 100, message = "收样日期长度不能超过 100")
    private String collectDate;

    @Size(max = 256, message = "客户名称长度不能超过 256")
    private String customDesc;

    @Size(max = 100, message = "客户长度不能超过 100")
    private String customerName;

    @Size(max = 100, message = "科室编码长度不能超过 100")
    private String departmentCode;

    @Size(max = 100, message = "医生姓名长度不能超过 100")
    private String doctorName;

    @Size(max = 1024, message = "邮箱长度不能超过 1024")
    private String emailAddress;

    @Size(max = 100, message = "委托日期长度不能超过 100")
    private String enterDate;

    @Size(max = 100, message = "ERP销售长度不能超过 100")
    private String erpSalerName;

    @Size(max = 256, message = "录单产品长度不能超过 256")
    private String erpTestName;

    @Size(max = 100, message = "一级亲属患癌情况长度不能超过 100")
    private String familyFirst;

    @Size(max = 100, message = "一级亲属年龄长度不能超过 100")
    private String familyFirstAge;

    @Size(max = 100, message = "一级亲属癌种长度不能超过 100")
    private String familyFirstCancerType;

    @Size(max = 100, message = "一级亲属确诊时间长度不能超过 100")
    private String familyFirstConfirmTime;

    @Size(max = 100, message = "二级亲属患癌情况长度不能超过 100")
    private String familySecond;

    @Size(max = 100, message = "二级亲属年龄长度不能超过 100")
    private String familySecondAge;

    @Size(max = 100, message = "二级亲属癌种长度不能超过 100")
    private String familySecondCancerType;

    @Size(max = 100, message = "二级亲属确诊时间长度不能超过 100")
    private String familySecondConfirmTime;

    @Size(max = 100, message = "加急编号长度不能超过 100")
    private String fastCode;

    @Size(max = 100, message = "第一次治疗史长度不能超过 100")
    private String firstTreatment;

    @Size(max = 100, message = "取材部位长度不能超过 100")
    private String fromOrgan;

    private String geneResult;

    private String geneType;

    @Size(max = 100, message = "取材日期长度不能超过 100")
    private String getSpecDate;

    @Size(max = 100, message = "文库编号长度不能超过 100")
    private String libraryName;

    @Size(max = 100, message = "病区长度不能超过 100")
    private String locationName;

    private String mailingAddress;

    @Size(max = 100, message = "经理邮箱长度不能超过 100")
    private String managerEmail;

    @Size(max = 100, message = "订单编号长度不能超过 100")
    private String orderCode;

    @Size(max = 100, message = "门诊号长度不能超过 100")
    private String outpatient;

    @Size(max = 100, message = "病理类型长度不能超过 100")
    private String pathologicalType;

    @Size(max = 100, message = "病理号长度不能超过 100")
    private String pathologyNum;

    @Size(max = 100, message = "姓名长度不能超过 100")
    private String patientName;

    @Size(max = 100, message = "患者电话长度不能超过 100")
    private String patientPhone;

    @Size(max = 100, message = "收款金额长度不能超过 100")
    private String payAmount;

    @Size(max = 100, message = "患者编号长度不能超过 100")
    private String pcode;

    @Size(max = 100, message = "项目经理邮箱长度不能超过 100")
    private String pmEmail;

    @Size(max = 100, message = "报告接收人电话长度不能超过 100")
    private String receiverTelephone;

    @Size(max = 100, message = "录单人长度不能超过 100")
    private String recorder;

    @Size(max = 100, message = "录单人编码长度不能超过 100")
    private String recorderCode;

    @Size(max = 256, message = "报告接收人长度不能超过 256")
    private String reportReceiver;

    @Size(max = 100, message = "房间长度不能超过 100")
    private String room;

    @Size(max = 100, message = "销售邮箱(ERP)长度不能超过 100")
    private String salerEmail;

    private String sampleRemark;

    @Size(max = 100, message = "样本来源长度不能超过 100")
    private String sampleSource;

    @Size(max = 100, message = "采样日期长度不能超过 100")
    private String sampleTime;

    @Size(max = 100, message = "样本类型长度不能超过 100")
    private String sampleType;

    private String secondTreatment;

    @Size(max = 100, message = "寄出日期长度不能超过 100")
    private String sendDate;

    @Size(max = 100, message = "性别长度不能超过 100")
    private String sex;

    @Size(max = 100, message = "标本编号长度不能超过 100")
    private String specimenNo;

    @Size(max = 100, message = "样本数量长度不能超过 100")
    private String specimenNum;

    @Size(max = 100, message = "支持邮箱长度不能超过 100")
    private String supportEmail;

    private String thirdTreatment;

    @Size(max = 100, message = "单位长度不能超过 100")
    private String unit;

    @Size(max = 100, message = "接诊医生邮箱长度不能超过 100")
    private String admissionDoctorEmail;

    @Size(max = 100, message = "接诊医生电话长度不能超过 100")
    private String admissionDoctorPhone;

    private String admissionHospital;

    @Size(max = 100, message = "癌种1长度不能超过 100")
    private String cancerType1;

    @Size(max = 100, message = "癌种编码长度不能超过 100")
    private String cancerTypeCode;

    @Size(max = 100, message = "合同名称长度不能超过 100")
    private String contractName;

    @Size(max = 100, message = "合同编号长度不能超过 100")
    private String contractsNo;

    @Size(max = 100, message = "单位名称长度不能超过 100")
    private String corpDesc;

    @Size(max = 100, message = "单位编号长度不能超过 100")
    private String corpNo;

    @Size(max = 100, message = "客户类型长度不能超过 100")
    private String customerType;

    private String detectionMethod;

    @Size(max = 100, message = "检测时间长度不能超过 100")
    private String detectionTime;

    @Size(max = 100, message = "快递公司长度不能超过 100")
    private String expressName;

    @Size(max = 100, message = "快递单号长度不能超过 100")
    private String expressNo;

    @Size(max = 100, message = "二级亲属患癌情况长度不能超过 100")
    private String familyKinshipTwoCancer;

    private String firstTreatmentDrugRegimen;

    @Size(max = 100, message = "第一次治疗时长长度不能超过 100")
    private String firstTreatmentDuration;

    private String firstTreatmentEffect;

    private String firstTreatmentMethod;

    @Size(max = 100, message = "第一次治疗时间长度不能超过 100")
    private String firstTreatmentTime;

    @Size(max = 100, message = "实验室长度不能超过 100")
    private String laboratoryName;

    @Size(max = 100, message = "运营负责人编码长度不能超过 100")
    private String operateManagerCode;

    @Size(max = 100, message = "运营负责人长度不能超过 100")
    private String operateManagerDesc;

    @Size(max = 100, message = "运营负责人邮箱长度不能超过 100")
    private String operateManagerEmail;

    @Size(max = 100, message = "订单金额长度不能超过 100")
    private String orderMoney;

    private String otherAttachments;

    private String pathologyReport;

    @Size(max = 1024, message = "患者信息邮箱长度不能超过 1024")
    private String patientInfoEmail;

    @Size(max = 100, message = "患者是否肿瘤长度不能超过 100")
    private String patientInfoIsCancer;

    @Size(max = 100, message = "收款完成日期长度不能超过 100")
    private String payFinishDate;

    @Size(max = 100, message = "录单单位长度不能超过 100")
    private String recorderDesc;

    @Size(max = 100, message = "销售长度不能超过 100")
    private String salesMan;

    @Size(max = 100, message = "销售编码长度不能超过 100")
    private String salesManCode;

    @Size(max = 100, message = "销售邮箱长度不能超过 100")
    private String salesManEmail;

    private String sampleAddress;

    @Size(max = 100, message = "送样联系人长度不能超过 100")
    private String sampleContactDesc;

    @Size(max = 100, message = "送样联系电话长度不能超过 100")
    private String sampleContactPhone;

    @Size(max = 100, message = "数量单位长度不能超过 100")
    private String sampleNumUnit;

    @Size(max = 100, message = "产品编码长度不能超过 100")
    private String sampleProductCode;

    private String secondTreatmentDrugRegimen;

    @Size(max = 100, message = "第二次治疗时长长度不能超过 100")
    private String secondTreatmentDuration;

    private String secondTreatmentEffect;

    private String secondTreatmentMethod;

    @Size(max = 100, message = "第二次治疗时间长度不能超过 100")
    private String secondTreatmentTime;

    @Size(max = 100, message = "流水号长度不能超过 100")
    private String serialNumber;

    @Size(max = 100, message = "具体癌种长度不能超过 100")
    private String specificCancer;

    @Size(max = 100, message = "送检机构长度不能超过 100")
    private String testingInstitution;

    private String thirdTreatmentDrugRegimen;

    @Size(max = 100, message = "第三次治疗时长长度不能超过 100")
    private String thirdTreatmentDuration;

    private String thirdTreatmentEffect;

    private String thirdTreatmentMethod;

    @Size(max = 100, message = "第三次治疗时间长度不能超过 100")
    private String thirdTreatmentTime;

    @Size(max = 50, message = "科室名称长度不能超过 50")
    private String departmentDesc;

    private String abnormalRemark;

    @Size(max = 256, message = "结算渠道长度不能超过 256")
    private String checkoutLogic;

    @Size(max = 100, message = "到样日期长度不能超过 100")
    private String dyDate;

    @Size(max = 100, message = "账期(天)长度不能超过 100")
    private String paymentDate;

    @Size(max = 256, message = "样本属性长度不能超过 256")
    private String sampleAttribute;

    @Size(max = 100, message = "签约时间长度不能超过 100")
    private String signTime;

    @Size(max = 256, message = "冻结状态长度不能超过 256")
    private String block;
    // @fields:end
}
