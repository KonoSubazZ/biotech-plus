package org.dromara.audit.domain.dto;

public record AuditIdentity(String tenantId, String subbarcode, Long analysisId, Long reportId) {
}
