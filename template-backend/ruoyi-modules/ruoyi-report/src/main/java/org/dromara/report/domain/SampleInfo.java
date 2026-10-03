package org.dromara.report.domain;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 样本信息对象 sample_file
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
    /** 样本编号 */
    private String subbarcode;

    /** 条码 */
    private String barcode;

    /** 患者编号 */
    private String patientId;

    /** 患者姓名 */
    private String personName;

    /** 性别 */
    private String gender;

    /** 出生日期 */
    private String birthday;

    /** 年龄 */
    private String age;

    /** 患者电话 */
    private String patientPhone;

    /** 医院 */
    private String hospital;

    /** 接收日期 */
    private String receivedDate;

    /** 样本类型 */
    private String specimenType;

    /** 样本数量 */
    private String specimenQuantity;

    /** 检测方案 */
    private String testingProgram;

    /** 疾病类型 */
    private String diseaseType;

    /** 客户 */
    private String client;

    /** 委托日期 */
    private String commissionDate;

    /** 产品名称 */
    private String productName;

    /** 备注 */
    private String remark;
    // @fields:end

    /** 逻辑删除标志 */
    @TableLogic
    private String delFlag;
}
