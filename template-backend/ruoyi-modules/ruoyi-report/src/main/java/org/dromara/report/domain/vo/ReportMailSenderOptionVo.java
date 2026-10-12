package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 发件邮箱候选（「报告发送」页的「发件邮箱」下拉项）
 * <p>
 * 只给页面需要的字段：**不暴露 SMTP/密码**。{@link #enabled} 由页面用来置灰不可用的选项
 * （先把服务端口径带下来，避免前端自己判断业务规则）。
 *
 * @author <你的名字>
 */
@Data
public class ReportMailSenderOptionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 邮件配置ID（report_mail_config.id） */
    private Long configId;

    /** 发件邮箱 */
    private String fromEmail;

    /** 发件人昵称 */
    private String fromName;

    /** 是否默认发件邮箱（0否 1是） */
    private String isDefault;

    /** 是否启用（0否 1是）；为 0 时页面把它置灰，不能选来发信 */
    private String enabled;

    /**
     * 该邮箱自己的默认标题（已按本报告变量渲染）。
     * <p>
     * 带上它，页面在「发件邮箱」之间切换时可以直接把标题/正文换成对应那套，
     * 不用再发一次请求（每条邮箱可以有各自的措辞）。
     */
    private String subject;

    /** 该邮箱自己的默认正文（已按本报告变量渲染，HTML） */
    private String content;
}
