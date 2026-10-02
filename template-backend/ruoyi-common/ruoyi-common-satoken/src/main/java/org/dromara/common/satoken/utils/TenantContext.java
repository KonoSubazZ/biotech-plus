package org.dromara.common.satoken.utils;

import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;

import java.util.function.Supplier;

/** Server-owned tenant scope. Internal authentication and asynchronous work restore their previous scope. */
public final class TenantContext {
    public static final String DEFAULT_TENANT_ID = "000000";
    public static final String PLATFORM_LOG_TENANT_ID = "__platform__";
    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IGNORED = new ThreadLocal<>();

    private TenantContext() { }

    public static String getTenantId() {
        String id = TENANT.get();
        if (id == null) {
            try {
                LoginUser user = LoginHelper.getLoginUser();
                id = user == null ? null : user.getTenantId();
            } catch (Exception ignored) {
                // No authenticated request; a tenant must be supplied by trusted background code.
            }
        }
        if (id == null || id.isBlank()) {
            throw new ServiceException("缺少租户身份，请重新登录");
        }
        return id;
    }

    public static boolean isIgnored() {
        return Boolean.TRUE.equals(IGNORED.get());
    }

    public static boolean isAdmin() {
        return TENANT.get() == null && LoginHelper.isSuperAdmin();
    }

    public static String cacheScope() {
        return isIgnored() || isAdmin() ? "platform" : getTenantId();
    }

    public static <T> T withoutTenant(Supplier<T> action) {
        Boolean previous = IGNORED.get();
        IGNORED.set(true);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                IGNORED.remove();
            } else {
                IGNORED.set(previous);
            }
        }
    }

    public static <T> T withTenant(String tenantId, Supplier<T> action) {
        if (tenantId == null || tenantId.isBlank()) throw new ServiceException("租户编号不能为空");
        String previous = TENANT.get();
        TENANT.set(tenantId);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                TENANT.remove();
            } else {
                TENANT.set(previous);
            }
        }
    }

    public static void withTenant(String tenantId, Runnable action) {
        withTenant(tenantId, () -> {
            action.run();
            return null;
        });
    }
}
