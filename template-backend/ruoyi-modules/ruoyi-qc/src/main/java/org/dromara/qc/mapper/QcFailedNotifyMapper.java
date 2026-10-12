package org.dromara.qc.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.qc.domain.QcFailedNotify;
import org.dromara.qc.domain.vo.QcFailedNotifyPrefillVo;
import org.dromara.qc.domain.vo.QcNotifySampleVo;
import org.dromara.qc.domain.vo.QcFailedNotifyVo;

/**
 * 样本质控不合格通知 数据层
 * <p>
 * 单表增删改查用继承来的 BaseMapperPlus 能力；
 * 「按样本编号回填样本信息」是跨表只读查询（sample_file），写在 resources/mapper/qc/QcFailedNotifyMapper.xml。
 *
 * @author <你的名字>
 */
public interface QcFailedNotifyMapper extends BaseMapperPlus<QcFailedNotify, QcFailedNotifyVo> {

    /**
     * 按样本编号取通知单要回填的样本字段（sample_file）
     * <p>
     * 调用方必须放在 {@code TenantContext.withoutTenant(...)} 里：样本可能属于别的药企，
     * 带租户条件会查不到。
     *
     * @param subbarcode 样本编号
     * @return 样本字段；查不到返回 null
     */
    QcNotifySampleVo selectSampleForNotify(@Param("subbarcode") String subbarcode);

    /**
     * 按样本编号取归属药企（sample_file.tenant_id）—— 通知单与质控接口同一口径：
     * 取样本自身的归属，不走「客户名 → sys_tenant」（客户名对不上时会让通知也生成不了）。
     * 调用方必须放在 {@code TenantContext.withoutTenant(...)} 里。
     *
     * @param subbarcode 样本编号
     * @return 药企编号；查不到返回 null
     */
    String selectTenantIdBySubbarcode(@Param("subbarcode") String subbarcode);

    /**
     * 取该样本最近一条通知记录（用于回显「已生成」与上次填写的内容）
     *
     * @param subbarcode 样本编号
     * @return 预填数据（记录字段）；没有记录返回 null
     */
    QcFailedNotifyPrefillVo selectLatestBySubbarcode(@Param("subbarcode") String subbarcode);
}
