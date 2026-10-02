package org.dromara.qc.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.qc.domain.bo.QcStandardBo;
import org.dromara.qc.domain.vo.QcStandardVo;

import java.util.List;

/**
 * 质控标准 业务层
 * <p>
 * 接口先合并（骨架），实现由各 owner 分别完成 —— 见 ai-rules/03-collaboration.md §2。
 *
 * @author <你的名字>
 */
public interface IQcStandardService {

    /**
     * 分页查询质控标准列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    TableDataInfo<QcStandardVo> selectPageList(QcStandardBo bo, PageQuery pageQuery);

    /**
     * 查询全部启用中的产品
     *
     * @return 产品列表
     */
    List<QcStandardVo> selectActiveList();

    /**
     * 按主键查询详情
     *
     * @param id 主键
     * @return 详情；不存在时抛 ServiceException
     */
    QcStandardVo queryById(Long id);

    /**
     * 新增
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean insertByBo(QcStandardBo bo);

    /**
     * 修改
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean updateByBo(QcStandardBo bo);

    /**
     * 批量删除
     *
     * @param ids 主键集合
     * @return 是否成功
     */
    Boolean deleteByIds(List<Long> ids);
}
