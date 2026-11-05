package org.dromara.qc.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通知发送表单（页面可改标题/正文/收件人/抄送/发件邮箱）
 *
 * @author <你的名字>
 */
@Data
public class SampleNotifySendBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 发件邮箱配置 id（空=默认邮箱） */
    private Long configId;

    /** 收件人（逗号/分号分隔） */
    @NotBlank(message = "收件人不能为空")
    private String to;

    /** 抄送（可空） */
    private String cc;

    /** 标题 */
    @NotBlank(message = "标题不能为空")
    private String subject;

    /** 正文（纯文本，发送前换行转 <br>） */
    @NotBlank(message = "正文不能为空")
    private String content;
}
