package org.dromara.qc.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.qc.domain.bo.QcRecordBo;
import org.dromara.qc.domain.vo.QcRecordVo;

import java.util.List;

/**
 * 质控记录 业务层
 * <p>
 * 湿实验质控与生信质控共用本接口，仅 qcCategory 参数不同（wet_lab / bioinfo），
 * 见 docs/context/设计文档/bioinfo-qc.md §1。
 *
 * @author <你的名字>
 */
public interface IQcRecordService {

    /**
     * 分页查询质控记录列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    TableDataInfo<QcRecordVo> selectPageList(QcRecordBo bo, PageQuery pageQuery);

    /**
     * 按主键查询详情
     *
     * @param id 主键
     * @return 详情；不存在时抛 ServiceException
     */
    QcRecordVo queryById(Long id);

    /**
     * 新增
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean insertByBo(QcRecordBo bo);

    /**
     * 修改
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean updateByBo(QcRecordBo bo);

    /**
     * 批量删除
     *
     * @param ids 主键集合
     * @return 是否成功
     */
    Boolean deleteByIds(List<Long> ids);
}
