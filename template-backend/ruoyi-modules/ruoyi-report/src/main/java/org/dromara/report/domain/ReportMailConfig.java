package org.dromara.report.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.report.utils.ReportMailDefaults;

/**
 * 报告邮件发送配置对象 report_mail_config
 * <p>
 * 表来源见 script/sql/update/update_5.6.6-report-mail-config.sql。
 * 语义：**一个租户一条**（唯一键 uk_report_mail_config_tenant），页面「报告管理 › 邮件配置」可改；
 * 代码默认值见 {@code ReportMailDefaults}，库里为空时按默认值兜底。
 * <p>
 * 密码列只用于发信，**接口永不回显**（Vo 里只给「是否已设置」）。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report_mail_config")
public class ReportMailConfig extends BaseEntity {

    /** 主键ID */
    @TableId(value = "id")
    private Long id;

    /** 是否启用邮件发送（0否 1是） */
    private String enabled;

    /** 是否默认发件邮箱（0否 1是；同一租户只允许一条为 1，报告发送页默认选中它） */
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

    /** 邮箱密码 / 客户端授权码 */
    private String password;

    /** 默认标题模板（支持 {{变量}}） */
    private String defaultSubject;

    /** 默认正文模板（HTML，支持 {{变量}}） */
    private String defaultContent;

    /** 逻辑删除标志 */
    @TableLogic
    private String delFlag;
}
