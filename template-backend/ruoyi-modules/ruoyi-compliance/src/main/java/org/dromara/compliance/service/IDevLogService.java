package org.dromara.compliance.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.compliance.domain.bo.DevLogBo;
import org.dromara.compliance.domain.vo.DevLogVo;

/**
 * 开发记录 业务层
 * <p>
 * append-only：不提供删除方法（合规上开发记录不可删改，只能新增/更正）。
 *
 * @author liushangzhi
 */
public interface IDevLogService {

    /**
     * 分页查询开发记录列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    TableDataInfo<DevLogVo> selectPageList(DevLogBo bo, PageQuery pageQuery);

    /**
     * 按主键查询详情
     *
     * @param id 主键
     * @return 详情；不存在时抛 ServiceException
     */
    DevLogVo queryById(Long id);

    /**
     * 新增
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean insertByBo(DevLogBo bo);

    /**
     * 修改
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean updateByBo(DevLogBo bo);
}
