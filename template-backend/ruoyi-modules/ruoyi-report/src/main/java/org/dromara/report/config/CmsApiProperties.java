package org.dromara.report.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CMS 订单样本查询（iPaaS 网关）配置（{@code cms.api.*}）。
 * <p>
 * 与 {@link SsqSignProperties} 同款：环境差异项只写在配置文件里按 profile 区分
 * （{@code application-dev.yml} 写 UAT、{@code application-prod.yml} 留空），外层可被环境变量覆盖。
 * 留空 = 调用时快速失败（报「未配置 cms.api.base-url」），不静默连到测试环境。
 * 接口名在 {@code CmsApiClient} 里现算 {@code Base64("HJERK:CMS:" + 接口名)}，不抄魔法串。
 *
 * @author <你的名字>
 */
@Data
@Component
@ConfigurationProperties(prefix = "cms.api")
public class CmsApiProperties {

    /** iPaaS 网关地址；空 = 未配置 */
    private String baseUrl;

    /** OAuth client_id（与 ssq 共用同一个 iPaaS client） */
    private String clientId;

    /** OAuth client_secret */
    private String clientSecret;

    /** 单次 HTTP 超时（秒） */
    private long timeoutSeconds = 30;

    /** 是否注册每日定时拉取 */
    private boolean syncEnabled = true;

    /** 定时表达式：默认每天 01:00 */
    private String syncCron = "0 0 1 * * ?";

    /** 分页大小（接口 numPerPage 上限 500） */
    private int pageSize = 500;
}
