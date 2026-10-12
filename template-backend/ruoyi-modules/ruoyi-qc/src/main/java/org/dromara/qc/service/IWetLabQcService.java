package org.dromara.qc.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.qc.domain.bo.WetLabQcBo;
import org.dromara.qc.domain.vo.WetLabQcReceiveResultVo;
import org.dromara.qc.domain.vo.WetLabQcVo;

import java.util.List;

/**
 * 湿实验质控 服务层
 *
 * @author <你的名字>
 */
public interface IWetLabQcService {

    /**
     * 分页查询湿实验质控列表（一行一个样本）
     *
     * @param bo        查询条件（样本条码 模糊，质控状态 等值）
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    TableDataInfo<WetLabQcVo> selectPageList(WetLabQcBo bo, PageQuery pageQuery);

    /**
     * 按主键取一条记录
     *
     * @param id 主键
     * @return 记录
     */
    WetLabQcVo queryById(Long id);

    /**
     * 人工修改质控值（只覆盖请求里带了的字段，样本编号不可改；改完按质控标准重判 status）
     *
     * @param bo 业务对象（必须带 id）
     * @return 是否成功
     */
    Boolean updateByBo(WetLabQcBo bo);

    /**
     * 批量逻辑删除
     *
     * @param ids 主键集合
     * @return 是否成功
     */
    Boolean deleteByIds(List<Long> ids);

    /**
     * 接收上游推送的湿实验质控数据（批量，一行一个样本；按样本编号有则合并更新、无则新增）
     *
     * @param items 推送入参
     * @return 处理统计（新增 / 覆盖 / 拒收及原因）
     */
    WetLabQcReceiveResultVo receive(List<WetLabQcBo> items);

    /**
     * 该样本的不达标项描述（给「处理」弹窗的「不合格原因」用），如
     * {@code DNA总量（ng） 88（要求 ≥100）；终文库浓度（ng/uL） 0.5（要求 ≥1）}
     * <p>
     * 与生信质控同一套口径：阈值读 qc_standard，等级类质控项（阈值非数值）自然跳过。
     *
     * @param subbarcode 样本编号
     * @return 不达标项（用「；」分隔）；没有记录 / 没有标准 / 全部达标都返回空串
     */
    String failureDescription(String subbarcode);
}
