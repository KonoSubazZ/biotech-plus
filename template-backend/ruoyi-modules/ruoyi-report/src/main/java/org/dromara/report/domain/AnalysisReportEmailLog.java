package org.dromara.report.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * 报告邮件发送审计日志对象 analysis_report_email_log
 * <p>
 * 表**早已存在**（建表在 script/sql/business/report-interpretation.sql，与 16 张报告链路的表一起），
 * 本次只是把 Java 侧补上：发送前写 SENDING，发送后回写 SUCCESS / FAILED。
 * <p>
 * 语义：一行 = 一次发送动作（同一报告可以发多次，所以不是一报告一行）。
 * {@code traceId} 是本地追踪号（唯一键 uk_report_email_trace），{@code providerMessageId} 是
 * SMTP 服务商返回的 message-id；后续要接「投递状态回填」（腾讯企业邮 /cgi-bin/log/mail）时，
 * 按 {@code providerStatus} 记原始状态即可，本类先不落那一列。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("analysis_report_email_log")
public class AnalysisReportEmailLog extends BaseEntity {

    /** 日志ID */
    @TableId(value = "id")
    private Long id;

    /** 邮件发送追踪号（UUID 去横线，32 位） */
    private String traceId;

    /** 分析数据ID */
    private Long analysisId;

    /** 报告ID */
    private Long reportId;

    /** 收件人（分号分隔） */
    private String recipients;

    /** 收件人数 */
    private Long recipientCount;

    /** 邮件模板标识（报告发送固定写 REPORT_SEND） */
    private String templateCode;

    /** 状态：SENDING / SUCCESS / FAILED */
    private String status;

    /** 邮件服务商消息ID */
    private String providerMessageId;

    /** 失败原因摘要 */
    private String failureReason;

    /** 触发人 */
    private String triggeredBy;

    /** 发起时间 */
    private Date requestedAt;

    /** 完成时间 */
    private Date finishedAt;

    /** 逻辑删除标志 */
    @TableLogic
    private String delFlag;
}
