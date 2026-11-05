package org.dromara.qc.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.qc.domain.SampleNotify;
import org.dromara.qc.domain.bo.SampleNotifyQueryBo;
import org.dromara.qc.domain.vo.QcNotifySampleVo;
import org.dromara.qc.domain.vo.SampleNotifyPrefillVo;
import org.dromara.qc.domain.vo.SampleNotifyVo;

import java.util.List;

/**
 * 样本通知台账 数据层
 * <p>
 * 单表增删改查用继承来的 BaseMapperPlus 能力；
 * 「按样本编号回填样本信息」「列表 join 用户」「按项目/中心取收件邮箱」是跨表只读查询，
 * 写在 resources/mapper/qc/SampleNotifyMapper.xml。
 *
 * @author <你的名字>
 */
public interface SampleNotifyMapper extends BaseMapperPlus<SampleNotify, SampleNotifyVo> {

    /**
     * 按样本编号取通知要回填的样本字段（cms_sample）
     * <p>
     * 调用方必须放在 {@code TenantContext.withoutTenant(...)} 里：样本可能属于别的药企，
     * 带租户条件会查不到。
     *
     * @param subbarcode 样本编号
     * @return 样本字段；查不到返回 null
     */
    QcNotifySampleVo selectSampleForNotify(@Param("subbarcode") String subbarcode);

    /**
     * 取同一订单下的兄弟样本（延期通知表头展示用：样本编号/类型/数量用「；」拼接）
     *
     * @param orderNo 订单编号（cms_sample.order_no）
     * @return 同订单样本（按 id 升序）；没有返回空列表
     */
    List<QcNotifySampleVo> selectSiblingsByOrder(@Param("orderNo") String orderNo);

    /**
     * 按样本编号取归属药企（cms_sample.tenant_id）—— 取样本自身的归属，不走「客户名 → sys_tenant」。
     * 调用方必须放在 {@code TenantContext.withoutTenant(...)} 里。
     *
     * @param subbarcode 样本编号
     * @return 药企编号；查不到返回 null
     */
    String selectTenantIdBySubbarcode(@Param("subbarcode") String subbarcode);

    /**
     * 取该样本该类型最近一条通知记录（回显「已生成」与上次填写的内容）
     *
     * @param subbarcode 样本编号
     * @param notifyType 通知类型
     * @return 预填数据；没有记录返回 null
     */
    SampleNotifyPrefillVo selectLatestByType(@Param("subbarcode") String subbarcode,
                                             @Param("notifyType") String notifyType);

    /**
     * 按「样本 + 类型 + 日期」取当天记录（同日覆盖规则用）
     *
     * @param subbarcode 样本编号
     * @param notifyType 通知类型
     * @param notifyDate 通知日期 yyyyMMdd
     * @return 记录；没有返回 null
     */
    SampleNotify selectByDayAndType(@Param("subbarcode") String subbarcode,
                                    @Param("notifyType") String notifyType,
                                    @Param("notifyDate") String notifyDate);

    /**
     * 异常处理分页：只返回「已签名且已提交」的记录（pending / sent / failed）
     *
     * @param page 分页参数
     * @param q    查询条件
     * @return 分页结果
     */
    List<SampleNotifyVo> selectPageList(IPage<SampleNotifyVo> page, @Param("q") SampleNotifyQueryBo q);

    /**
     * 项目行抄送邮箱（project_config.cc_email，parent_id=0）
     *
     * @param projectNo 项目编号
     * @return 邮箱串；查不到返回 null
     */
    String selectProjectCcEmail(@Param("projectNo") String projectNo);

    /**
     * 研究中心行收件邮箱（project_config 中心行的 receive_email，按项目 + 中心名称匹配）
     *
     * @param projectNo  项目编号
     * @param centerName 研究中心名称（样本的 research_center）
     * @return 邮箱串；查不到返回 null
     */
    String selectCenterReceiveEmail(@Param("projectNo") String projectNo,
                                    @Param("centerName") String centerName);
}
