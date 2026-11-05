package org.dromara.test.report;

import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.dromara.common.mybatis.handler.TenantLineHandlerImpl;
import org.dromara.common.satoken.utils.TenantContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("local")
class ReportRevisionTenantSqlTest {
    @Test
    void businessQueriesRemainScoped() {
        TenantLineInnerInterceptor interceptor = new TenantLineInnerInterceptor(new TenantLineHandlerImpl());
        String sql = TenantContext.withTenant("fixture-a", () -> interceptor.parserSingle(
            "SELECT id FROM report_version_sequence WHERE client = ? FOR UPDATE", null));
        Assertions.assertTrue(sql.contains("tenant_id = 'fixture-a'"), sql);
    }

    @Test
    void reviewerExceptionDoesNotInjectAnUnreachableTenantCondition() {
        TenantLineInnerInterceptor interceptor = new TenantLineInnerInterceptor(new TenantLineHandlerImpl());
        String input = "SELECT u.user_id FROM sys_user u JOIN sys_user_role ur ON u.user_id=ur.user_id "
            + "WHERE u.user_id=? AND (u.tenant_id=? OR u.tenant_id='000000')";
        String sql = TenantContext.withTenant("fixture-a", () -> TenantContext.withoutTenant(
            () -> interceptor.parserSingle(input, null)));
        Assertions.assertFalse(sql.contains("tenant_id = 'fixture-a'"), sql);
        Assertions.assertTrue(sql.contains("'000000'"), sql);
        Assertions.assertFalse(TenantContext.isIgnored());
    }
}
