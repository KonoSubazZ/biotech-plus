package org.dromara.report.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 报告发送 请求对象（「报告解读 › 报告发送」Tab）
 * <p>
 * 收件 / 抄送 / 标题 / 内容都由页面带着走（页面从 {@code /mail-preview} 拿到默认值再允许修改），
 * 后端不再按客户查表拼默认值；标题正文也不在这里做 {@code {{变量}}} 校验 ——
 * 那是「邮件配置」保存模板时的事（见 {@code ReportMailConfigServiceImpl}）。
 *
 * @author <你的名字>
 */
@Data
public class InterpretationSendBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分析数据ID */
    @NotNull(message = "分析数据ID不能为空")
    private Long analysisId;

    /** 报告ID */
    @NotNull(message = "报告ID不能为空")
    private Long reportId;

    /** 收件人（逗号 / 分号分隔，可多个） */
    @NotBlank(message = "收件人不能为空")
    @Size(max = 2000, message = "收件人过长")
    private String to;

    /** 抄送（可空，同样逗号 / 分号分隔） */
    @Size(max = 2000, message = "抄送过长")
    private String cc;

    /** 标题 */
    @NotBlank(message = "标题不能为空")
    @Size(max = 500, message = "标题最长 500 个字符")
    private String subject;

    /** 正文（HTML；换行会在发送前转 &lt;br&gt;） */
    @NotBlank(message = "内容不能为空")
    private String content;

    /** 是否附带报告文件（默认 true） */
    private Boolean attachReport;

    /** 用哪个发件邮箱（report_mail_config.id）；不传 = 用「默认邮箱」 */
    private Long configId;
}
