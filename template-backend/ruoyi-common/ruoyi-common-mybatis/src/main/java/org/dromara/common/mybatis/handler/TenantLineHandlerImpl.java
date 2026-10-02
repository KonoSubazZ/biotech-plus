package org.dromara.common.mybatis.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.TenantContext;

import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** All business tables are isolated by default; only explicit platform tables are shared. */
public class TenantLineHandlerImpl implements TenantLineHandler {
    private static final Set<String> SHARED_TABLES = Set.of("sys_tenant", "sys_menu", "sys_role", "sys_role_menu",
        "sys_client", "sys_config", "sys_dict_type", "sys_dict_data", "sys_oss_config", "gen_table", "gen_table_column");
    private final Supplier<String> tenantId;
    private final BooleanSupplier ignore;

    public TenantLineHandlerImpl() {
        this(TenantContext::getTenantId, () -> TenantContext.isIgnored() || TenantContext.isAdmin());
    }

    public TenantLineHandlerImpl(Supplier<String> tenantId, BooleanSupplier ignore) {
        this.tenantId = tenantId;
        this.ignore = ignore;
    }

    @Override
    public Expression getTenantId() {
        String id = tenantId.get();
        if (id == null || id.isBlank()) throw new ServiceException("缺少租户身份，请重新登录");
        return new StringValue(id);
    }

    @Override
    public boolean ignoreTable(String tableName) {
        String name = tableName.replace("`", "").replace("\"", "").toLowerCase(Locale.ROOT);
        return ignore.getAsBoolean() || SHARED_TABLES.contains(name) || name.startsWith("sj_");
    }
}
