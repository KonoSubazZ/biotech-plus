package org.dromara.audit.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Param;
import org.dromara.audit.domain.dto.AuditTarget;
import java.util.List;
import java.util.Map;

/** 显式应用范围已在创建 capture 时冻结，避免回调退出租户作用域后串租户。 */
public interface AuditSourceMapper {
    @InterceptorIgnore(tenantLine = "true")
    List<Map<String, Object>> snapshot(@Param("target") AuditTarget target,
                                      @Param("tenantId") String tenantId,
                                      @Param("restricted") boolean restricted);
}
