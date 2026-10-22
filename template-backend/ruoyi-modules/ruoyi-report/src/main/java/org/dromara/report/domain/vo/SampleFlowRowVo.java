package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 样本流转列表行（analysis_data ⟕ sample_file ⟕ analysis_report）。
 * <p>
 * 一行 = 一个分析批次；报告的可见状态、处理人在 analysis_report 上，样本侧展示字段来自
 * analysis_data 与 sample_file。字段名与前端 typings 的 Api.Report.SampleFlowRow 一一对应。
 *
 * @author <你的名字>
 */
@Data
public class SampleFlowRowVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分析数据ID */
    private Long analysisId;

    /** 分析日期（YYYYMMDD） */
    private String analysisDate;

    /** 样本编号 */
    private String subbarcode;

    /** 患者编号 */
    private String barcode;

    /** 产品名称 */
    private String product;

    /** 驱动状态（int 码：10 失败 / 11 成功 / 12 驱动中，常量见 ruoyi-drive 的 utils/DriveStatus） */
    private Integer driveStatus;

    /** 驱动执行时间 */
    private Date driveExecutedAt;

    /** 送检单位 / 客户（sample_file.customer_name） */
    private String customerName;

    /** 患者姓名（sample_file.patient_name） */
    private String patientName;

    /** 样本类型（sample_file.sample_type） */
    private String sampleType;

    /** 录单产品（sample_file.erp_test_name） */
    private String erpTestName;

    /** 报告ID（为空 = 该批次还没有报告记录） */
    private Long reportId;

    /** 报告名（最近一次生成） */
    private String reportName;

    /** 解读癌种 */
    private String analysisDisease;

    /** 报告状态（int 码，取值见 utils/ReportStatus） */
    private Integer reportStatus;

    /** 报告解读人（认领人，sys_user.user_id） */
    private Long interpreter;

    /** 报告解读人昵称（join sys_user） */
    private String interpreterName;

    /** 报告解读认领时间 */
    private Date reportInterpretedAt;

    /** 生信看点人（sys_user.user_id） */
    private Long bioinfoViewedBy;

    /** 生信看点人昵称（join sys_user） */
    private String bioinfoViewedByName;

    /** 生信看点完成时间 */
    private Date bioinfoViewedAt;

    /** 生信复核人（sys_user.user_id） */
    private Long bioinfoReviewedBy;

    /** 生信复核人昵称（join sys_user） */
    private String bioinfoReviewedByName;

    /** 生信复核时间 */
    private Date bioinfoReviewedAt;

    /** 报告文件（转化后的 PDF 文件名，取自 report_file_path 的文件名部分；未生成为空） */
    private String reportFilePdfName;

    /** 签名件（签名后 PDF 文件名，取自 report_signed_file_path；未签名为空） */
    private String reportSignedFileName;

    /** 签名开始时间（送签时间） */
    private Date reportSignStartedAt;

    /** 签名完成时间（上上签回调） */
    private Date reportSignedAt;

    /** 报告生成人（sys_user.user_id） */
    private Long reportGeneratedBy;

    /** 报告生成人昵称（join sys_user） */
    private String reportGeneratedByName;

    /** 报告生成时间 */
    private Date reportGeneratedAt;

    /** 报告复核人（sys_user.user_id） */
    private Long reportCheckedBy;

    /** 报告复核人昵称（join sys_user） */
    private String reportCheckedByName;

    /** 报告复核时间 */
    private Date reportCheckedAt;

    /** 报告发送人（sys_user.user_id） */
    private Long reportSentBy;

    /** 报告发送人昵称（join sys_user） */
    private String reportSentByName;

    /** 报告发送时间 */
    private Date reportSentAt;

    /** 回退来源状态（审核失败回退时非空，页面打「审核失败回退」标签） */
    private Integer revertFrom;

    /**
     * 本行最新更新时间：取 analysis_data / analysis_report / sample_file 三者 update_time 的最大值。
     * <p>
     * 前端轮询用它做「有没有变化」的对比（行集合 + 该时间戳），没变就不动表格，避免整表闪动、丢失滚动位置。
     */
    private Date latestUpdateTime;
}
