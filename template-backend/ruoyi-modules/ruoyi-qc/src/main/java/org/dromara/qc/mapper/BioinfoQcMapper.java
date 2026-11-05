package org.dromara.qc.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.qc.domain.BioinfoQc;
import org.dromara.qc.domain.vo.BioinfoQcVo;

/**
 * 生信质控 数据层
 * <p>
 * 单表增删改查、简单条件组合、排序、计数一律用继承来的 BaseMapperPlus 能力，不写自定义 SQL。
 * 下面两个是接口灌数据时的跨表只读查询（cms_sample ← web_crm 源表落库、sys_tenant 药企目录），
 * 写在 resources/mapper/qc/BioinfoQcMapper.xml。
 *
 * @author <你的名字>
 */
public interface BioinfoQcMapper extends BaseMapperPlus<BioinfoQc, BioinfoQcVo> {

    /**
     * 按样本编号取客户（= 药企名）—— 数据源 cms_sample（web_crm 源表 myapp_webcrmsample 的落库副本）
     * <p>
     * 调用方必须放在 {@code TenantContext.withoutTenant(...)} 里：样本可能属于别的药企，
     * 带租户条件会查不到。
     *
     * @param subbarcode 样本编号
     * @return 客户名（customer_name）；查不到返回 null
     */
    String selectCustomerNameBySubbarcode(@Param("subbarcode") String subbarcode);

    /**
     * 按客户名解析药企编号：先按 tenant_id 精确匹配（客户名就是药企编号时），再按 tenant_name 精确匹配
     *
     * @param customer 客户名（cms_sample.customer_name）
     * @return 药企编号；匹配不到返回 null
     */
    String selectTenantIdByCustomer(@Param("customer") String customer);
}
