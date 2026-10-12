package org.dromara.qc.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.qc.domain.QcFailedNotify;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 样本质控不合格通知（生成记录）视图对象
 *
 * @author <你的名字>
 */
@Data
@NoArgsConstructor
@AutoMapper(target = QcFailedNotify.class)
public class QcFailedNotifyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 样本编号 */
    private String subbarcode;

    /** 样本信息 id */
    private Long sampleFileId;

    /** 湿实验质控 id */
    private Long wetLabQcId;

    /** 生信质控 id（湿实验合格后才可能有） */
    private Long bioinfoQcId;

    /** 质控类型（wet_lab 湿实验 / bioinfo 生信）—— 这条通知是在哪个质控页点处理产生的 */
    private String qcCategory;

    /** 通知单号 */
    private String notifyNo;

    /** 通知状态 */
    private String notifyStatus;

    /** 日期 */
    private String notifyDate;

    /** 分析时间 */
    private String analysisTime;

    /** 检测产品 */
    private String product;

    /** 客户 */
    private String customerName;

    /** 样本类型 */
    private String sampleType;

    /** 不合格环节（多选，顿号分隔） */
    private String qcStage;

    /** 是否终止检测（多选，顿号分隔） */
    private String terminateMeasure;

    /** 建议处理办法（多选，顿号分隔） */
    private String remedy;

    /** 建议处理办法-其他（原文） */
    private String remedyOther;

    /** 不合格原因 */
    private String reason;

    /** 反馈人 */
    private String reporter;

    /** 反馈时间 */
    private String reportTime;

    /** 复核人 */
    private String reviewer;

    /** 复核时间 */
    private String reviewTime;

    /** 渲染用的 JSON 路径 */
    private String jsonPath;

    /** 生成的 DOCX 路径 */
    private String rawDocxPath;

    /** PDF 路径 */
    private String pdfPath;

    /** 创建时间 */
    private Date createTime;
}
