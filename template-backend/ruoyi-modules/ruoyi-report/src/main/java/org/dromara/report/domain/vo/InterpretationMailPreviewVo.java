package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 报告发送页参数（GET /report/interpretation/mail-preview）
 * <p>
 * 「报告发送」Tab 一次拉全：收件 / 抄送候选、渲染好的标题正文默认值、附件名、能不能发。
 * 收件候选取该报告 LIMS 里的 4 个邮箱字段（去重），抄送候选本期为空（留字段方便以后加规则）。
 * 标题正文是**默认值**：页面可改，改完以页面传的为准。
 *
 * @author <你的名字>
 */
@Data
public class InterpretationMailPreviewVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 收件人候选（LIMS 邮箱，去重） */
    private List<String> toCandidates = new ArrayList<>();

    /** 抄送候选（本期为空） */
    private List<String> ccCandidates = new ArrayList<>();

    /** 渲染后的默认标题 */
    private String subject;

    /** 渲染后的默认正文（HTML） */
    private String content;

    /** 附件文件名（报告已生成时才有） */
    private String attachmentName;

    /** 邮件配置是否启用 */
    private Boolean configEnabled;

    /** 是否允许发送（配置启用 + 报告已审核 + 报告文件存在） */
    private Boolean canSend;

    /** 不允许发送的原因（一句话；可发时为空） */
    private String blockedReason;

    /** 可选发件邮箱（邮件配置列表；enabled=false 的页面上不可选） */
    private List<ReportMailSenderOptionVo> senderOptions = new ArrayList<>();

    /** 默认选中的发件邮箱（= 邮件配置里「是否默认」那条；没有配置时为 null） */
    private Long defaultConfigId;
}
