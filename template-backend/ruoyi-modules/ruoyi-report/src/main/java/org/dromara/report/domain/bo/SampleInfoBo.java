package org.dromara.report.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.report.domain.SampleInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 样本信息业务对象 sample_file
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
// mapstruct-plus：生成 Bo → 实体的转换器。Service 里 MapstructUtils.convert(bo, X.class) 靠它，
// 少了这个注解，运行期会抛 ConvertException: cannot find converter from XxxBo to Xxx
@AutoMapper(target = SampleInfo.class, reverseConvertGenerate = false)
public class SampleInfoBo extends BaseEntity {

    /** 主键；新增时为空，更新时必填 */
    private Long id;

    // @fields:start
    /** 样本编号 */
    @NotBlank(message = "样本编号不能为空")
    @Size(max = 80, message = "样本编号长度不能超过 80")
    private String subbarcode;

    /** 条码 */
    @Size(max = 80, message = "条码长度不能超过 80")
    private String barcode;

    /** 患者编号 */
    @Size(max = 80, message = "患者编号长度不能超过 80")
    private String patientId;

    /** 患者姓名 */
    @Size(max = 80, message = "患者姓名长度不能超过 80")
    private String personName;

    /** 性别 */
    @Size(max = 10, message = "性别长度不能超过 10")
    private String gender;

    /** 出生日期 */
    @Size(max = 20, message = "出生日期长度不能超过 20")
    private String birthday;

    /** 年龄 */
    @Size(max = 10, message = "年龄长度不能超过 10")
    private String age;

    /** 患者电话 */
    @Size(max = 40, message = "患者电话长度不能超过 40")
    private String patientPhone;

    /** 医院 */
    @Size(max = 120, message = "医院长度不能超过 120")
    private String hospital;

    /** 接收日期 */
    @Size(max = 20, message = "接收日期长度不能超过 20")
    private String receivedDate;

    /** 样本类型 */
    @Size(max = 60, message = "样本类型长度不能超过 60")
    private String specimenType;

    /** 样本数量 */
    @Size(max = 60, message = "样本数量长度不能超过 60")
    private String specimenQuantity;

    /** 检测方案 */
    @Size(max = 120, message = "检测方案长度不能超过 120")
    private String testingProgram;

    /** 疾病类型 */
    @Size(max = 120, message = "疾病类型长度不能超过 120")
    private String diseaseType;

    /** 客户 */
    @Size(max = 120, message = "客户长度不能超过 120")
    private String client;

    /** 委托日期 */
    @Size(max = 20, message = "委托日期长度不能超过 20")
    private String commissionDate;

    /** 产品名称 */
    @Size(max = 120, message = "产品名称长度不能超过 120")
    private String productName;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
    // @fields:end
}
