package org.dromara.audit.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/** 一次业务事件中，一条实际记录的受控前后快照。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("audit_change")
public class AuditChange extends BaseEntity {
    @TableId
    private Long id;
    private String tenantId;
    private Long eventId;
    private String tableName;
    private String objectKey;
    private String changeType;
    private String beforeJson;
    private String afterJson;
    private String changedFieldsJson;
}
