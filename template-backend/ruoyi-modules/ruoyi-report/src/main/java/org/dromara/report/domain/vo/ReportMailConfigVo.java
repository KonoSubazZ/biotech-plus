package org.dromara.report.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.report.domain.ReportMailConfig;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 报告邮件配置 视图对象（「报告管理 › 邮件配置」读取用）
 * <p>
 * **不包含 password**：密码只用于发信，接口只回「是否已设置」（{@link #passwordSet}），
 * 避免把授权码暴露给前端 / 浏览器 / 日志。
 *
 * @author <你的名字>
 */
@Data
// mapstruct-plus：生成 Vo ↔ 实体的转换器（Entity → Vo 靠它）
@AutoMapper(target = ReportMailConfig.class)
public class ReportMailConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID（无记录时为空，表示还没保存过、当前用的是代码默认值） */
    private Long id;

    /** 是否启用邮件发送（0否 1是） */
    private String enabled;

    /** 是否默认发件邮箱（0否 1是） */
    private String isDefault;

    /** SMTP 服务器 */
    private String smtpHost;

    /** SMTP 端口 */
    private Integer smtpPort;

    /** 是否 SSL（0否 1是） */
    private String sslEnable;

    /** 发件邮箱（= SMTP 登录账号） */
    private String fromEmail;

    /** 发件人昵称 */
    private String fromName;

    /** 是否已设置密码（前端只显示「已设置 / 未设置」，永不回显明文） */
    private Boolean passwordSet;

    /** 默认标题模板（支持 {{变量}}） */
    private String defaultSubject;

    /** 默认正文模板（HTML，支持 {{变量}}） */
    private String defaultContent;

    /** 创建时间（实体基类的审计列带出来，列表展示用） */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
