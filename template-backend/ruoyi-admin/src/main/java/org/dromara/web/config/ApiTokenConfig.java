package org.dromara.web.config;

import lombok.RequiredArgsConstructor;
import org.dromara.web.config.properties.ApiTokenProperties;
import org.dromara.web.interceptor.ApiTokenInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 固定 token 接口调用的拦截器注册
 * <p>
 * 拦截器挂在所有路径上（要能看到请求），真正生效范围由 api.token.paths 决定，
 * 所以别的模块接入时只需要往配置里加一条路径，不用改代码。
 * <p>
 * order = 最高优先级：必须早于 Sa-Token 的拦截器（SaInterceptor 注册在 ruoyi-common-security），
 * 否则 @SaCheckPermission 检查时身份还没建立。
 *
 * @author <你的名字>
 */
@Configuration
@EnableConfigurationProperties(ApiTokenProperties.class)
@RequiredArgsConstructor
public class ApiTokenConfig implements WebMvcConfigurer {

    private final ApiTokenInterceptor apiTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiTokenInterceptor)
            .addPathPatterns("/**")
            .order(Ordered.HIGHEST_PRECEDENCE);
    }
}
