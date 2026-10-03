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

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
