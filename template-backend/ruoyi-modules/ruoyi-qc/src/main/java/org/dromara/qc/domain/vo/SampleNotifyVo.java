package org.dromara.qc.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.qc.domain.SampleNotify;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 样本通知台账 视图对象（列表 / 详情）
 *
 * @author <你的名字>
 */
@Data
@NoArgsConstructor
@AutoMapper(target = SampleNotify.class)
public class SampleNotifyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 通知单号 */
    private String notifyNo;

    /** 通知类型（delay / sample / wet_lab / bioinfo） */
    private String notifyType;

    /** 通知状态（generated / pending / sent / failed） */
    private String notifyStatus;

    /** 签署状态（none / submitted / signed） */
    private String signStatus;

    /** 通知日期（yyyyMMdd） */
    private String notifyDate;

    /** 样本编号 */
    private String subbarcode;

    /** 样本名称 */
    private String sampleName;

    /** 研究方案编号 */
    private String schemeNo;

    /** 项目编号 */
    private String projectNo;

    /** 试验参与者编号 */
    private String subjectNo;

    /** 检测产品 */
    private String productName;

    /** 客户 */
    private String customerName;

    /** 样本类型 */
    private String sampleType;

    /** 不合格原因 */
    private String reason;

    /** 延迟原因（delay） */
    private String delayReason;

    /** 预计报告出具时间（delay） */
    private String expectedReportDate;

    /** 不合格环节（顿号分隔） */
    private String qcStage;

    /** 是否终止检测（顿号分隔） */
    private String terminateMeasure;

    /** 建议处理办法（顿号分隔） */
    private String remedy;

    /** 建议处理办法-其他 */
    private String remedyOther;

    /** 反馈人 */
    private String reporter;

    /** 反馈时间 */
    private String reportTime;

    /** 复核人 */
    private String reviewer;

    /** 复核时间 */
    private String reviewTime;

    /** JSON 路径 */
    private String jsonPath;

    /** DOCX 路径 */
    private String rawDocxPath;

    /** PDF 路径 */
    private String pdfPath;

    /** 签名后 PDF 路径 */
    private String signedPdfPath;

    /** 交付文件名（不含路径） */
    private String fileName;

    /** 上上签合同号 */
    private String signContractId;

    /** 处理人 id */
    private Long handlerBy;

    /** 处理人姓名（join sys_user） */
    private String handlerName;

    /** 处理时间 */
    private Date handlerTime;

    /** 发送人 id */
    private Long senderBy;

    /** 发送人姓名（join sys_user） */
    private String senderName;

    /** 发送时间 */
    private Date senderTime;

    /** 实际收件人 */
    private String sendTo;

    /** 实际抄送 */
    private String sendCc;

    /** 最近一次发送失败原因 */
    private String sendError;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
