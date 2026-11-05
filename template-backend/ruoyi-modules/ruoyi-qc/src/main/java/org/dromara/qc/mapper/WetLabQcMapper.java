package org.dromara.qc.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.qc.domain.WetLabQc;
import org.dromara.qc.domain.vo.WetLabQcVo;

/**
 * 湿实验质控 数据层
 * <p>
 * 单表增删改查、简单条件组合、排序、计数一律用继承来的 BaseMapperPlus 能力。
 * 下面几个是接口灌数据时用的：① 按样本编号取归属药企（cms_sample.tenant_id）；
 * ② 「含已逻辑删除行」的取行 + 清理（唯一键 (tenant_id, subbarcode) 不含 del_flag）。
 * 写在 resources/mapper/qc/WetLabQcMapper.xml。
 *
 * @author <你的名字>
 */
public interface WetLabQcMapper extends BaseMapperPlus<WetLabQc, WetLabQcVo> {

    /**
     * 按样本编号取「归属药企编号」（cms_sample.tenant_id）—— 样本自身的归属，服务端可信
     * <p>
     * 调用方必须放在 {@code TenantContext.withoutTenant(...)} 里：样本可能属于别的药企，
     * 带租户条件会查不到（此刻也还没有该药企的租户作用域）。
     *
     * @param subbarcode 样本编号
     * @return 药企编号；查不到返回 null
     */
    String selectTenantIdBySubbarcode(@Param("subbarcode") String subbarcode);

    /**
     * 按样本编号取行，**包含已逻辑删除的行**
     * <p>
     * 唯一键 uk_wet_lab_qc_subbarcode(tenant_id, subbarcode) 不含 del_flag：删过的行还占着这个键，
     * 接口重复推送时必须先把它恢复/清掉，否则新插会撞唯一键。租户条件由拦截器自动附加。
     *
     * @param subbarcode 样本编号
     * @return 行（含 delFlag）；没有返回 null
     */
    WetLabQc selectRowIncludingDeleted(@Param("subbarcode") String subbarcode);

    /**
     * 物理清掉一行（只用于「已逻辑删除的行占着唯一键」这一种情况，见 selectRowIncludingDeleted）
     *
     * @param id 主键
     */
    void purgeById(@Param("id") Long id);
}
