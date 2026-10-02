package org.dromara.test;

import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.dromara.common.mybatis.handler.TenantLineHandlerImpl;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.TenantContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Expected: CRUD SQL is scoped; admin/global tables are shared; missing context fails closed. */
@Tag("dev")
class TenantIsolationTest {
    private TenantLineInnerInterceptor interceptor(String tenantId, boolean admin) {
        return new TenantLineInnerInterceptor(new TenantLineHandlerImpl(() -> tenantId, () -> admin));
    }

    @Test
    void scopesSelectUpdateDeleteAndJoin() {
        var plugin = interceptor("tenant-a", false);
        for (String sql : new String[]{"SELECT * FROM biz_order WHERE id = 9",
            "UPDATE biz_order SET name = 'changed' WHERE id = 9", "DELETE FROM biz_order WHERE id = 9"}) {
            String parsed = plugin.parserSingle(sql, null);
            assertTrue(parsed.contains("tenant_id = 'tenant-a'"), parsed);
        }
        String join = plugin.parserSingle("SELECT o.id FROM biz_order o JOIN biz_item i ON i.order_id = o.id", null);
        assertTrue(join.contains("o.tenant_id = 'tenant-a'"), join);
        assertTrue(join.contains("i.tenant_id = 'tenant-a'"), join);
    }

    @Test
    void insertsTenantAndDoesNotFilterAdminOrSharedTables() {
        assertTrue(interceptor("tenant-a", false).parserSingle("INSERT INTO biz_order (id) VALUES (9)", null)
            .contains("tenant_id"));
        assertFalse(interceptor("tenant-a", true).parserSingle("SELECT * FROM biz_order", null).contains("tenant_id"));
        assertFalse(interceptor("tenant-a", false).parserSingle("SELECT * FROM sys_menu", null).contains("tenant_id"));
    }

    @Test
    void missingTenantFailsClosed() {
        assertThrows(ServiceException.class, () -> interceptor(null, false).parserSingle("SELECT * FROM biz_order", null));
    }

    @Test
    void internalContextRestoresAfterFailure() {
        assertFalse(TenantContext.isIgnored());
        assertThrows(IllegalStateException.class, () -> TenantContext.withoutTenant(() -> {
            assertTrue(TenantContext.isIgnored());
            throw new IllegalStateException("expected");
        }));
        assertFalse(TenantContext.isIgnored());
        TenantContext.withTenant("tenant-a", () -> assertEquals("tenant-a", TenantContext.getTenantId()));
        assertThrows(ServiceException.class, TenantContext::getTenantId);
    }
}
