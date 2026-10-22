package org.dromara.project.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.project.domain.bo.ProjectConfigBo;
import org.dromara.project.domain.vo.ProjectConfigVo;

import java.util.List;

/**
 * 项目配置 服务层
 *
 * @author levi
 */
public interface IProjectConfigService {

    /**
     * 分页查询项目配置列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    TableDataInfo<ProjectConfigVo> selectPageList(ProjectConfigBo bo, PageQuery pageQuery);

    /**
     * 按主键查询详情
     *
     * @param id 主键
     * @return 详情；不存在时抛 ServiceException
     */
    ProjectConfigVo queryById(Long id);

    /**
     * 新增项目配置
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean insertByBo(ProjectConfigBo bo);

    /**
     * 修改项目配置
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean updateByBo(ProjectConfigBo bo);

    /**
     * 批量删除项目配置（逻辑删除）
     *
     * @param ids 主键集合
     * @return 是否成功
     */
    Boolean deleteByIds(List<Long> ids);
}
