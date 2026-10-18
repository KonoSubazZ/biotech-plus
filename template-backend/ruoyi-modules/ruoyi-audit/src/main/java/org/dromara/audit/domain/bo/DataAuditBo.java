package org.dromara.audit.domain.bo;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 所有租户与对象限制由后端再次校验。 */
@Data
public class DataAuditBo {
    @Size(max = 160)
    private String subbarcode;
    private Long analysisId;
    private Long reportId;
    @Size(max = 16)
    private String auditScope;
    @Size(max = 100)
    private String action;
    @Size(max = 120)
    private String actorName;
    private String beginTime;
    private String endTime;
    private Boolean ascending;
    private Boolean includeSampleCommon;
}
