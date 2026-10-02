package org.dromara.common.core.service;

/** Tenant validation used by common persistence code without depending on the system module. */
public interface TenantService {
    void checkTenant(String tenantId);
    String getUserTenantId(Long userId);
}
