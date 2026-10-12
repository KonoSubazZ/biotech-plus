package org.dromara.report.domain.bo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 发送测试邮件 请求对象（「报告管理 › 邮件配置」）
 * <p>
 * 测试发送只验证「账号 / 密码 / SSL / 端口」是否可用，收件人由操作人手填，
 * 不读 LIMS、不带附件，也不写发送审计日志。
 *
 * @author <你的名字>
 */
@Data
public class ReportMailTestBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 测试收件人 */
    @NotBlank(message = "收件人不能为空")
    @Email(message = "收件人邮箱格式不正确")
    private String to;

    /** 用哪条配置发（report_mail_config.id）；不传 = 用「默认邮箱」 */
    private Long configId;
}
