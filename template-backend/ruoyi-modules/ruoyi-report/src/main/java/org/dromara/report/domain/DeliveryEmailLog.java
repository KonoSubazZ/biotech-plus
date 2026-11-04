package org.dromara.report.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.TenantEntity;

import java.util.Date;

/**
 * 交付物邮件发送记录 delivery_email_log
 * <p>
 * 一行 = 一次发送动作（报告 / 通知共用一张表，target_type 区分）。
 * 报告侧的 analysis_report_email_log 自本表上线后停止写入，历史只读保留（已标记废弃）。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("delivery_email_log")
public class DeliveryEmailLog extends TenantEntity {

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 邮件发送追踪号（UUID 去横线，32 位） */
    private String traceId;

    /** 交付物类型：report / notify */
    private String targetType;

    /** 报告ID / 通知ID */
    private Long targetId;

    /** 报告编号 / 通知单号 */
    private String targetNo;

    /** 分析数据ID（报告专用） */
    private Long analysisId;

    /** 通知类型（delay / sample / wet_lab / bioinfo） */
    private String notifyType;

    /** 样本编号 */
    private String subbarcode;

    /** 收件人（分号分隔） */
    private String recipients;

    /** 收件人数 */
    private Long recipientCount;

    /** 抄送（分号分隔） */
    private String cc;

    /** 邮件主题 */
    private String subject;

    /** 附件文件名（不含路径） */
    private String attachmentName;

    /** 发件邮箱配置ID */
    private Long mailConfigId;

    /** 发件地址 */
    private String fromAddr;

    /** SENDING / SUCCESS / FAILED */
    private String status;

    /** 邮件服务商 message-id */
    private String providerMessageId;

    /** 失败原因摘要 */
    private String failureReason;

    /** 触发人 */
    private String triggeredBy;

    /** 发起时间 */
    private Date requestedAt;

    /** 完成时间 */
    private Date finishedAt;
}
