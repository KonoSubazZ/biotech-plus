package org.dromara.audit.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.audit.domain.DataAudit;
import java.util.Date;

@Data
@AutoMapper(target = DataAudit.class)
public class DataAuditVo {
    private Long id;
    private String tenantId;
    private String operationId;
    private String auditScope;
    private String action;
    private String subbarcode;
    private Long analysisId;
    private Long reportId;
    private String actorId;
    private String actorName;
    private String source;
    private String outcome;
    private Date occurredAt;
    private Integer changeCount;
}
