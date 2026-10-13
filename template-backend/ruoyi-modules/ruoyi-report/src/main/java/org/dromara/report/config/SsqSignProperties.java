package org.dromara.report.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 上上签（iPaaS 网关）配置（{@code ssq.*}）。
 * <p>
 * 环境差异项的值一律写在配置文件里，按 Spring profile 区分（一个 key 只在一个地方能看到值）：
 * <ul>
 *   <li>{@code application-dev.yml} —— 本机 / 测试环境（UAT 网关 + 测试账号）</li>
 *   <li>{@code application-prod.yml} —— 生产环境（生产网关 + 生产账号，待对接人确认）</li>
 * </ul>
 * 外层仍可被环境变量覆盖（写法 {@code ${SSQ_CLIENT_SECRET:...}}），与项目里 {@code onlyoffice.public-base} /
 * {@code api.token.value} 同款；运行时用的是哪套，见 {@code SsqSignServiceImpl} 里的 token 日志。
 * 签名人账号不在这里（本阶段按口径写死在 {@code SsqSignServiceImpl}）。
 *
 * @author <你的名字>
 */
@Data
@Component
@ConfigurationProperties(prefix = "ssq")
public class SsqSignProperties {

    /** iPaaS 网关地址；空 = 未配置，调用时快速失败（不猜地址、不兜底） */
    private String baseUrl;

    /** OAuth client_id（该 client 需已授权） */
    private String clientId;

    /** OAuth client_secret */
    private String clientSecret;

    /** 签署模板ID：三个场景共用同一模板 */
    private String templateId;

    /** 单次 HTTP 超时（秒）；不是环境差异项，给个够用的默认值 */
    private long timeoutSeconds = 30;
}
