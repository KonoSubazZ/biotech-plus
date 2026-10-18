package org.dromara.audit.domain.dto;

import java.util.Map;

public record AuditRow(AuditSource source, Map<String, Object> before, Map<String, Object> after) {
}
