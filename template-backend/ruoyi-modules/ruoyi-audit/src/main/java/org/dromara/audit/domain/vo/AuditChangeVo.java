package org.dromara.audit.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.audit.domain.AuditChange;

@Data
@AutoMapper(target = AuditChange.class)
public class AuditChangeVo {
    private Long id;
    private Long eventId;
    private String tableName;
    private String objectKey;
    private String changeType;
    private String beforeJson;
    private String afterJson;
    private String changedFieldsJson;
}
