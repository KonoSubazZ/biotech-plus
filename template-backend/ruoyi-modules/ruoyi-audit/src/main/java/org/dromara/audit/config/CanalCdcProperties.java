package org.dromara.audit.config;

import lombok.Data;
import org.dromara.common.core.factory.YmlPropertySourceFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * CDC 变更日志配置。
 * <p>
 * 配置放模块自带的 audit-cdc.yml；用 @Configuration（@PropertySource 只在配置类上生效）。
 *
 * @author liushangzhi
 */
@Data
@Configuration(proxyBeanMethods = false)
@ConfigurationProperties(prefix = "audit.cdc")
@PropertySource(value = "classpath:audit-cdc.yml", factory = YmlPropertySourceFactory.class)
public class CanalCdcProperties {

    /** 总开关：false 时不连 canal、不起线程 */
    private boolean enabled = false;

    /** canal-server 地址（默认本机；换机时用环境变量 AUDIT_CDC_HOST 覆盖） */
    private String host = "localhost";

    /** canal-server 端口 */
    private int port = 11111;

    /** canal destination（镜像默认 example） */
    private String destination = "example";

    /** canal-server 账号（本机未开鉴权，留空） */
    private String username = "";

    /** canal-server 密码 */
    private String password = "";

    /** 采集表白名单（与 canal-server 的 instance filter 保持一致） */
    private String filter;

    /** 单批最大事件数 */
    private int batchSize = 500;

    /** 拉取超时（毫秒） */
    private long pollTimeoutMs = 1000;

    /** 单列值最大长度，超出截断 */
    private int maxValueLength = 4000;
}
