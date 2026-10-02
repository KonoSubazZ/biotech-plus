package org.dromara.system.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.service.TenantService;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.satoken.utils.TenantContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class TenantWebConfig implements WebMvcConfigurer {
    private final TenantService tenantService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (!LoginHelper.isLogin()) return true;
                if ("/auth/logout".equals(request.getServletPath())) return true;
                tenantService.checkTenant(TenantContext.getTenantId());
                if (LoginHelper.isSuperAdmin()) return true;
                String path = request.getServletPath();
                if (path.startsWith("/tool/gen") || path.startsWith("/monitor/cache") || path.startsWith("/monitor/online")) {
                    throw new ServiceException("仅超级管理员可以访问平台管理功能");
                }
                boolean shared = List.of("/system/role", "/system/menu", "/system/config", "/system/dict",
                    "/system/client", "/system/oss/config").stream().anyMatch(path::startsWith);
                if (shared && !"GET".equals(request.getMethod()) && !path.contains("/authUser/")) {
                    throw new ServiceException("仅超级管理员可以修改平台共享配置");
                }
                return true;
            }
        }).addPathPatterns("/**");
    }
}
