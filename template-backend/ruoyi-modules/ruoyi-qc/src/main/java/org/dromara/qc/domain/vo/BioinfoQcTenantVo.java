package org.dromara.qc.domain.vo;

import lombok.Data;

/**
 * 样本编号 → 药企 的解析结果（静默查询，不落在任何租户作用域内）
 *
 * @author <你的名字>
 */
@Data
public class BioinfoQcTenantVo {

    /** 入参样本编号 */
    private String subbarcode;

    /** cms_sample.customer_name（= 药企名） */
    private String customerName;

    /** 解析出的药企编号（sys_tenant.tenant_id） */
    private String tenantId;

    /** 是否解析成功：false 表示样本查不到、或客户名匹配不到启用中的药企 */
    private Boolean found;
}
