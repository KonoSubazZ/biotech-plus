package org.dromara.audit.domain.vo;

import lombok.Data;
import java.util.List;

@Data
public class DataAuditDetailVo {
    private DataAuditVo event;
    private List<AuditChangeVo> changes;
}
