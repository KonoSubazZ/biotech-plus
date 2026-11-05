package org.dromara.report.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.report.domain.DeliveryEmailLog;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 交付物邮件发送记录（通知详情/报告详情展示用）
 *
 * @author <你的名字>
 */
@Data
@AutoMapper(target = DeliveryEmailLog.class)
public class DeliveryEmailLogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String traceId;

    /** report / notify */
    private String targetType;

    private Long targetId;

    private String targetNo;

    private String notifyType;

    private String subbarcode;

    private String recipients;

    private Long recipientCount;

    private String cc;

    private String subject;

    private String attachmentName;

    private String fromAddr;

    /** SENDING / SUCCESS / FAILED */
    private String status;

    private String providerMessageId;

    private String failureReason;

    private String triggeredBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date requestedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date finishedAt;
}
