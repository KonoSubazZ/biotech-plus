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
            id = loginTenantId();
        }
        if (id == null || id.isBlank()) {
            throw new ServiceException("缺少租户身份，请重新登录");
        }
        return id;
    }

    public static boolean isIgnored() {
        return Boolean.TRUE.equals(IGNORED.get());
    }

    /**
     * 平台身份：默认租户 000000 的账号与超级管理员同为平台身份，查询/更新不再拼租户条件，可跨药企查看与维护。
     * 显式切入某个租户（withTenant）时平台身份失效，避免平台账号误改指定租户以外的数据。
     */
    public static boolean isAdmin() {
        if (TENANT.get() != null) {
            return false;
        }
        return LoginHelper.isSuperAdmin() || DEFAULT_TENANT_ID.equals(loginTenantId());
    }

    /**
     * 当前登录用户所属租户；无登录上下文（后台任务、匿名请求）返回 null
     */
    private static String loginTenantId() {
        try {
            LoginUser user = LoginHelper.getLoginUser();
            return user == null ? null : user.getTenantId();
        } catch (Exception ignored) {
            // No authenticated request; a tenant must be supplied by trusted background code.
            return null;
        }
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

    /**
     * 无返回值版本（与 withTenant 的两个重载对称）；拦截器里建立身份这类「一整段动作」用它
     */
    public static void withoutTenant(Runnable action) {
        withoutTenant(() -> {
            action.run();
            return null;
        });
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
