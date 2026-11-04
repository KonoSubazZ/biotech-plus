package org.dromara.qc.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通知发送结果
 *
 * @author <你的名字>
 */
@Data
public class SampleNotifySendVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 通知记录 id */
    private Long notifyId;

    /** 实际收件人 */
    private String to;

    /** 实际抄送 */
    private String cc;

    /** 发件邮箱 */
    private String fromEmail;

    /** 服务商返回的 message-id */
    private String messageId;

    public SampleNotifySendVo() {
    }

    public SampleNotifySendVo(Long notifyId, String to, String cc, String fromEmail, String messageId) {
        this.notifyId = notifyId;
        this.to = to;
        this.cc = cc;
        this.fromEmail = fromEmail;
        this.messageId = messageId;
    }
}
