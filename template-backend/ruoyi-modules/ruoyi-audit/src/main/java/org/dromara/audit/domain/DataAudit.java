package org.dromara.audit.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import java.util.Date;

/** 只追加的审计事件，业务归属不随操作者当前租户变化。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("audit_event")
public class DataAudit extends BaseEntity {
    @TableId
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
