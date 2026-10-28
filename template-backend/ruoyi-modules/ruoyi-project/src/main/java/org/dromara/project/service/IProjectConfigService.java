package org.dromara.project.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.project.domain.bo.ProjectConfigBo;
import org.dromara.project.domain.bo.ProjectCenterBo;
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

    /**
     * 某项目下的研究中心列表（按 id 升序，即录入顺序）
     *
     * @param parentId 项目行主键
     * @return 研究中心列表
     */
    List<ProjectConfigVo> selectCentersByParentId(Long parentId);

    /**
     * 新增研究中心（挂到指定项目行下）
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean insertCenterByBo(ProjectCenterBo bo);

    /**
     * 修改研究中心（只能改中心行，归属关系不可变）
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean updateCenterByBo(ProjectCenterBo bo);

    /**
     * 批量删除研究中心（逻辑删除；混入项目行直接拒绝）
     *
     * @param ids 主键集合
     * @return 是否成功
     */
    Boolean deleteCentersByIds(List<Long> ids);
}
