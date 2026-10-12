package org.dromara.report.utils;

/**
 * 报告邮件发送的**代码默认值**（「报告管理 › 邮件配置」未配置时按这些值展示与使用）。
 * <p>
 * 口径来自生产系统 report-system（{@code src/mail.properties} + {@code utils/EmailUtil.java}）：
 * SMTP {@code smtp.exmail.qq.com:465} + SSL；发件昵称「诺禾致源报告」；
 * 标题形如「请查收诺禾致源的检测报告，姓名：{{patientName}}-{{subbarcode}}」。
 * <p>
 * 刻意**不写死账号密码**：账号密码必须由「邮件配置」页录入（多租户各自一份，落库）。
 *
 * @author <你的名字>
 */
public final class ReportMailDefaults {

    /** SMTP 服务器（腾讯企业邮箱 / 企业微信邮箱同源） */
    public static final String SMTP_HOST = "smtp.exmail.qq.com";

    /** SMTP 端口（SSL） */
    public static final int SMTP_PORT = 465;

    /** 是否 SSL（1是 0否） */
    public static final String SSL_ENABLE = "1";

    /** 发件人昵称 */
    public static final String FROM_NAME = "诺禾致源报告";

    /** 默认标题模板 */
    public static final String SUBJECT = "请查收诺禾致源的检测报告，姓名：{{patientName}}-{{subbarcode}}";

    /** 默认正文模板（HTML；换行在发送前统一转 &lt;br&gt;） */
    public static final String CONTENT = "尊敬的客户：<br>您好！<br>请您查收附件的检测报告<br>祝好~";

    private ReportMailDefaults() {
    }
}
