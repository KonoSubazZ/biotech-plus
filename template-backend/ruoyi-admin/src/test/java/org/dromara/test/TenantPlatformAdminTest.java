package org.dromara.test;

import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.mybatis.handler.TenantLineHandlerImpl;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.satoken.utils.TenantContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

/**
 * 平台身份口径：默认租户 000000 内的账号与超级管理员同一数据范围（不拼 tenant_id），
 * 其它租户仍按自身 tenant_id 过滤；显式 withTenant 切入某租户时平台身份失效。
 */
@Tag("dev")
class TenantPlatformAdminTest {

    private LoginUser loginUser(Long userId, String tenantId) {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setTenantId(tenantId);
        return user;
    }

    /** 走真实默认构造（ignore = isIgnored || isAdmin），验证拦截器产出的 SQL */
    private String scopedSql(String sql) {
        return new TenantLineInnerInterceptor(new TenantLineHandlerImpl()).parserSingle(sql, null);
    }

    @Test
    void defaultTenantAccountIsPlatformScoped() {
        try (var helper = mockStatic(LoginHelper.class)) {
            helper.when(LoginHelper::getLoginUser).thenReturn(loginUser(4L, TenantContext.DEFAULT_TENANT_ID));
            helper.when(() -> LoginHelper.isSuperAdmin()).thenReturn(false);
            assertTrue(TenantContext.isAdmin());
            String scoped = scopedSql("SELECT * FROM biz_order");
            assertFalse(scoped.contains("tenant_id"), scoped);
        }
    }

    @Test
    void otherTenantAccountStaysScoped() {
        try (var helper = mockStatic(LoginHelper.class)) {
            helper.when(LoginHelper::getLoginUser).thenReturn(loginUser(6L, "tenant-b"));
            helper.when(() -> LoginHelper.isSuperAdmin()).thenReturn(false);
            assertFalse(TenantContext.isAdmin());
            String scoped = scopedSql("SELECT * FROM biz_order");
            assertTrue(scoped.contains("tenant_id = 'tenant-b'"), scoped);
        }
    }

    @Test
    void explicitTenantScopeDisablesPlatformIdentity() {
        try (var helper = mockStatic(LoginHelper.class)) {
            helper.when(LoginHelper::getLoginUser).thenReturn(loginUser(1L, TenantContext.DEFAULT_TENANT_ID));
            helper.when(() -> LoginHelper.isSuperAdmin()).thenReturn(true);
            TenantContext.withTenant("tenant-b", () -> {
                assertFalse(TenantContext.isAdmin());
                String scoped = scopedSql("SELECT * FROM biz_order");
                assertTrue(scoped.contains("tenant_id = 'tenant-b'"), scoped);
            });
        }
    }

    @Test
    void backgroundWithoutLoginKeepsTenantMissing() {
        try (var helper = mockStatic(LoginHelper.class)) {
            helper.when(LoginHelper::getLoginUser).thenReturn(null);
            helper.when(() -> LoginHelper.isSuperAdmin()).thenReturn(false);
            assertFalse(TenantContext.isAdmin());
            assertThrows(org.dromara.common.core.exception.ServiceException.class,
                () -> scopedSql("SELECT * FROM biz_order"));
        }
    }
}
