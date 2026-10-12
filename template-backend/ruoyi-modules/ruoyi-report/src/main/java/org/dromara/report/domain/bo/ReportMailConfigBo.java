package org.dromara.report.domain.bo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import org.dromara.report.utils.ReportMailDefaults;

/**
 * 报告邮件配置 请求对象（「报告管理 › 邮件配置」保存用）
 * <p>
 * password 约定：**空串或 null = 不修改已保存的密码**（页面不回显明文，只能整段覆盖）。
 * 标题 / 正文 / 昵称 / 服务器等空值会在 Service 里用 {@code ReportMailDefaults} 兜底。
 *
 * @author <你的名字>
 */
@Data
public class ReportMailConfigBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID（一个租户只有一行，新增时为空） */
    private Long id;

    /** 是否启用邮件发送（0否 1是） */
    private String enabled;

    /** 是否默认发件邮箱（0否 1是；设为 1 时服务端会把同租户其它行清成 0） */
    private String isDefault;

    /** SMTP 服务器 */
    private String smtpHost;

    /** SMTP 端口 */
    private Integer smtpPort;

    /** 是否 SSL（0否 1是） */
    private String sslEnable;

    /** 发件邮箱（= SMTP 登录账号） */
    @NotBlank(message = "发件邮箱不能为空")
    @Email(message = "发件邮箱格式不正确")
    private String fromEmail;

    /** 发件人昵称 */
    private String fromName;

    /** 邮箱密码 / 客户端授权码（空串 = 不修改） */
    private String password;

    /** 默认标题模板（支持 {{变量}}） */
    @Size(max = 500, message = "标题最长 500 个字符")
    private String defaultSubject;

    /** 默认正文模板（HTML，支持 {{变量}}） */
    private String defaultContent;
}
