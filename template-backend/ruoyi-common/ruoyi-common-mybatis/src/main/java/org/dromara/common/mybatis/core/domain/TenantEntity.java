package org.dromara.common.mybatis.core.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Base class for tenant-owned business records. Tenant ownership cannot be changed by an update. */
@Data
@EqualsAndHashCode(callSuper = true)
public class TenantEntity extends BaseEntity {
    @TableField(fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER)
    private String tenantId;
}
