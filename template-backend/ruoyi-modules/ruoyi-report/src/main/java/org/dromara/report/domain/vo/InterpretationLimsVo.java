package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 解读页 Tab①：LIMS 信息（样本信息）
 * <p>
 * 数据源不另建 LIMS 表：本仓已把生产库录单样本表（myapp_webcrmsample，122 列）全量落成
 * {@code sample_file}，按 {@code sample_file.barcode = analysis_data.subbarcode} 取一条。
 * 字段映射见 docs/context/report/report-module-design.md 第 5.2 节。
 *
 * @author <你的名字>
 */
@Data
public class InterpretationLimsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 是否在 sample_file 里找到了该样本编号的记录；false 表示没有 LIMS 数据（阻止生成） */
    private Boolean found;

    /** 命中的样本编号 */
    private String barcode;

    // ---------------- 患者信息 ----------------
    /** 患者编号（sample_file.pcode） */
    private String patientId;
    /** 患者姓名 */
    private String patientName;
    /** 性别 */
    private String gender;
    /** 出生日期 */
    private String birthday;
    /** 年龄 */
    private String age;

    // ---------------- 疾病信息 ----------------
    /** 录单癌种 */
    private String cancerType;
    /** 病理类型 */
    private String pathologicalType;
    /** 临床分期 */
    private String clinicalStage;
    /** 临床备注 */
    private String clinicalRemark;

    // ---------------- 送检信息 ----------------
    /** 医院/送检单位（customer_name 为空时回退 custom_desc / corp_desc） */
    private String hospitalName;
    /** 送检医生 */
    private String doctorName;

    // ---------------- 样本信息 ----------------
    /** 样本类型 */
    private String specimenType;
    /** 样本量（specimen_num + unit） */
    private String specimenQuantity;
    /** 样本来源 */
    private String sampleSource;
    /** 取材部位 */
    private String fromOrgan;

    // ---------------- 日期信息 ----------------
    /** 采样日期（sample_time，为空回退 collect_date） */
    private String sampleCollectedAt;
    /** 收样日期 */
    private String sampleReceivedAt;
    /** 委托日期 */
    private String commissionedAt;

    // ---------------- 产品与其他 ----------------
    /** 录单产品（与报告产品做一致性检查） */
    private String testingProgram;
    /** 样本备注 */
    private String sampleRemark;
    /** 实验室 */
    private String laboratoryName;

    // ---------------- 邮件候选（报告发送页用） ----------------
    /** 报告接收人 */
    private String reportReceiver;
    /** 送检邮箱 */
    private String emailAddress;
    /** 患者信息邮箱 */
    private String patientInfoEmail;
    /** 接诊医生邮箱 */
    private String doctorEmail;
}
