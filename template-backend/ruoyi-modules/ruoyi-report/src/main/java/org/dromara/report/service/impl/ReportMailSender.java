package org.dromara.report.service.impl;

import cn.hutool.extra.mail.JakartaMail;
import cn.hutool.extra.mail.MailAccount;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.report.domain.ReportMailConfig;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 报告邮件发信器：拿一条「邮件配置」（发件邮箱）把 HTML 邮件发出去（可带附件）。
 * <p>
 * 纯发送器：不查库、不管默认邮箱，配置由调用方通过
 * {@code IReportMailConfigService.loadForSend(configId)} 取好传进来。
 * <p>
 * 为什么不用 {@code org.dromara.common.mail.utils.MailUtils}：它的账号是类常量
 * {@code SpringUtils.getBean(MailAccount.class)}，而本仓 {@code application.yml} 里
 * {@code mail.enabled=false} → MailConfig 的 MailAccount Bean 被
 * {@code @ConditionalOnProperty} 关掉 → 一碰该类就抛 {@code NoSuchBeanDefinitionException}。
 * 这里直接用 hutool 的 {@link JakartaMail} + 自己 new 的 {@link MailAccount}，与全局邮件 Bean 解耦。
 * <p>
 * 依赖说明：{@code cn.hutool.extra.mail} 随 {@code ruoyi-common-core} 的 {@code hutool-extra} 传递进来，
 * 本模块**不需要**新增 pom 依赖。
 *
 * @author <你的名字>
 */
@Slf4j
@Component
public class ReportMailSender {

    /**
     * 用指定发件邮箱发送 HTML 邮件（可带附件）。
     *
     * @param config  发件邮箱配置（含密码）
     * @param to      收件人（逗号 / 分号分隔，可多个）
     * @param cc      抄送（可空）
     * @param subject 标题
     * @param content 正文（HTML）
     * @param files   附件
     * @return 服务商返回的 message-id
     */
    public String send(ReportMailConfig config, String to, String cc, String subject, String content, File... files) {
        if (!"1".equals(config.getEnabled())) {
            throw new ServiceException("发件邮箱「" + config.getFromEmail()
                + "」未启用，请先到「报告管理 › 邮件配置」打开它的开关", 400);
        }
        List<String> tos = splitAddresses(to);
        if (tos.isEmpty()) {
            throw new ServiceException("收件人不能为空", 400);
        }
        List<String> ccs = splitAddresses(cc);
        try {
            JakartaMail mail = JakartaMail.create(toAccount(config));
            mail.setTos(tos.toArray(new String[0]));
            if (!ccs.isEmpty()) {
                mail.setCcs(ccs.toArray(new String[0]));
            }
            mail.setTitle(subject);
            mail.setContent(content);
            mail.setHtml(true);
            if (files != null && files.length > 0) {
                mail.setFiles(files);
            }
            return mail.send();
        } catch (Exception e) {
            // 只打收件人与标题：日志里绝不出现密码
            log.error("报告邮件发送失败：from={} to={} subject={}", config.getFromEmail(), to, subject, e);
            throw new ServiceException("邮件发送失败：" + e.getMessage());
        }
    }

    /**
     * 发送测试邮件（不带附件），用于验证该条配置的账号 / 密码 / SSL / 端口是否可用。
     *
     * @param config 发件邮箱配置（含密码）
     * @param to     收件人（手填）
     */
    public void sendTest(ReportMailConfig config, String to) {
        send(config, to, null, "【测试邮件】诺禾致源报告邮件配置",
            "这是一封来自「报告管理 › 邮件配置」的测试邮件，收到即表示该发件邮箱的账号与密码可用。");
    }

    /** 配置行 → hutool 邮件账号（发件人显示名走 from 的 `昵称 &lt;addr&gt;` 形式） */
    private MailAccount toAccount(ReportMailConfig config) {
        MailAccount account = new MailAccount();
        account.setHost(config.getSmtpHost());
        account.setPort(config.getSmtpPort());
        account.setAuth(true);
        account.setSslEnable("1".equals(config.getSslEnable()));
        account.setSocketFactoryPort(config.getSmtpPort());
        account.setUser(config.getFromEmail());
        account.setPass(config.getPassword());
        String fromName = StringUtils.isBlank(config.getFromName()) ? config.getFromEmail() : config.getFromName();
        account.setFrom(fromName + " <" + config.getFromEmail() + ">");
        return account;
    }

    /** 逗号 / 分号分隔 → 去空去重（保持输入顺序） */
    private List<String> splitAddresses(String addresses) {
        if (StringUtils.isBlank(addresses)) {
            return List.of();
        }
        return Arrays.stream(addresses.split("[,;]"))
            .map(String::trim)
            .filter(address -> !address.isEmpty())
            .distinct()
            .toList();
    }
}
