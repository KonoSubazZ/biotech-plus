package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/** Lightweight tenant directory; no package or subscription features. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
public class SysTenant extends BaseEntity {
    @TableId
    private Long id;
    @NotBlank(message = "租户编号不能为空")
    @Pattern(regexp = "[A-Za-z0-9_-]{1,20}", message = "租户编号只能包含字母、数字、下划线或连字符，长度不超过20")
    private String tenantId;
    @NotBlank(message = "租户名称不能为空")
    @Size(max = 100, message = "租户名称不能超过100个字符")
    private String tenantName;
    @NotBlank(message = "租户状态不能为空")
    @Pattern(regexp = "[01]", message = "租户状态无效")
    private String status;
    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
