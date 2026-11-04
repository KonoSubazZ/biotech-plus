package org.dromara.qc.domain.vo;

import lombok.Data;
import org.dromara.report.domain.vo.ReportMailSenderOptionVo;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通知发送页参数（与报告发送页同构）
 * <p>
 * 收件人 = 样本报告接收邮箱 + 研究中心收件邮箱；抄送 = 项目配置的抄送邮箱。
 *
 * @author <你的名字>
 */
@Data
public class SampleNotifyMailPreviewVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 通知记录 id */
    private Long notifyId;

    /** 可选发件邮箱（含各自的标题/正文默认值） */
    private List<ReportMailSenderOptionVo> senderOptions = new ArrayList<>();

    /** 默认发件邮箱 id */
    private Long defaultConfigId;

    /** 收件人候选（样本 + 研究中心） */
    private List<String> toCandidates = new ArrayList<>();

    /** 抄送候选（项目配置） */
    private List<String> ccCandidates = new ArrayList<>();

    /** 默认标题 */
    private String subject;

    /** 默认正文 */
    private String content;

    /** 附件名（签名件文件名） */
    private String attachmentName;

    /** 是否可发送 */
    private Boolean canSend;

    /** 不可发送原因（可发送时为空） */
    private String blockedReason;
}
