package org.dromara.qc.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.qc.domain.bo.BioinfoQcBo;
import org.dromara.qc.domain.vo.BioinfoQcReceiveResultVo;
import org.dromara.qc.domain.vo.BioinfoQcTenantVo;
import org.dromara.qc.domain.vo.BioinfoQcVo;

import java.util.List;

/**
 * 生信质控 业务层
 * <p>
 * 数据只从接口进来（{@link #receive}），页面只读：没有新增/修改/删除。
 *
 * @author <你的名字>
 */
public interface IBioinfoQcService {

    /**
     * 分页查询生信质控列表
     *
     * @param bo        查询条件（样本编号 / 文库编号 模糊，状态 等值）
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    TableDataInfo<BioinfoQcVo> selectPageList(BioinfoQcBo bo, PageQuery pageQuery);

    /**
     * 按主键查询详情
     *
     * @param id 主键
     * @return 详情；不存在时抛 ServiceException
     */
    BioinfoQcVo queryById(Long id);

    /**
     * 样本编号 → 药企：查 sample_file.customer_name，再按它匹配 sys_tenant（静默，不限定租户）
     *
     * @param subbarcode 样本编号
     * @return 解析结果（含 found 标记与命中的客户名）
     */
    BioinfoQcTenantVo resolveTenant(String subbarcode);

    /**
     * 按质控标准算出「不达标项」描述（给不合格通知单的「不合格原因」用）
     *
     * @param subbarcode 样本编号
     * @return 如 {@code raw_base 1.30（要求 ≥2）；probe_capture_ratio 66.41（要求 ≥70）}；
     *     没有记录 / 没有标准 / 全部达标都返回空串
     */
    String failureDescription(String subbarcode);

    /**
     * 修改生信质控记录（人工修正指标值；样本编号是业务键，不允许改）
     * <p>
     * 改完按当前阈值重新判定 status。
     *
     * @param bo 业务对象（必须带主键）
     * @return 是否成功
     */
    Boolean updateByBo(BioinfoQcBo bo);

    /**
     * 批量删除生信质控记录（逻辑删除）
     *
     * @param ids 主键集合
     * @return 是否成功
     */
    Boolean deleteByIds(List<Long> ids);

    /**
     * 接收上游推送的生信质控数据：逐行解析药企后按串行落库，落库前算 status
     *
     * @param items 质控记录（一行一个样本）
     * @return 处理统计（新增 / 更新 / 拒收及原因）
     */
    BioinfoQcReceiveResultVo receive(List<BioinfoQcBo> items);
}
