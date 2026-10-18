package org.dromara.audit.domain.dto;

import lombok.Data;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 一次显式业务操作的比较数据，仅在当前事务内存活。 */
@Data
public class AuditCapture {
    private String action;
    private String outcome;
    private String operationId;
    private String actorId;
    private String actorName;
    private String source;
    private String tenantId;
    private boolean unrestricted;
    private Date occurredAt;
    private Map<AuditTarget, List<Map<String, Object>>> before = new LinkedHashMap<>();
}
