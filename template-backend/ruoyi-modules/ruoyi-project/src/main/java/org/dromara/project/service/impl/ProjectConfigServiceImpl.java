package org.dromara.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.project.domain.ProjectConfig;
import org.dromara.project.domain.bo.ProjectConfigBo;
import org.dromara.project.domain.bo.ProjectCenterBo;
import org.dromara.project.domain.vo.ProjectConfigVo;
import org.dromara.project.mapper.ProjectConfigMapper;
import org.dromara.project.service.IProjectConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 项目配置 业务层处理
 * <p>
 * 风格要点（对齐 template-backend/docs/backend-code-standard.md）：
 * 查询条件集中在 buildQueryWrapper 里「声明 → 基础条件 → 分支条件 → 排序」展开，
 * 不在方法体内堆链式调用；缺数据抛明确异常，不返回裸 null。
 *
 * @author levi
 */
@RequiredArgsConstructor
@Service
public class ProjectConfigServiceImpl implements IProjectConfigService {

    private final ProjectConfigMapper baseMapper;

    @Override
    public TableDataInfo<ProjectConfigVo> selectPageList(ProjectConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ProjectConfig> wrapper = buildQueryWrapper(bo);
        Page<ProjectConfigVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public ProjectConfigVo queryById(Long id) {
        ProjectConfigVo vo = baseMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("项目配置不存在：id=" + id);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(ProjectConfigBo bo) {
        validateProjectNoUnique(bo.getProjectNo(), null);
        ProjectConfig entity = MapstructUtils.convert(bo, ProjectConfig.class);

        // 项目行身份：parent_id=0，研究中心两列空串（唯一键里这两列一起构成「一项目一行」）
        entity.setParentId(0L);
        entity.setResearchCenterNo("");
        entity.setResearchCenterName("");

        // 唯一键 uk_project_config_project_no 不含 del_flag：同一个项目编号被软删过时，
        // 直接 insert 会撞唯一键报「系统错误」。这里恢复旧行并写入新值，
        // 用户视角就是「项目删掉之后还能重新配一遍」。
        Long deletedId = baseMapper.selectDeletedIdByProjectNo(bo.getProjectNo());
        if (deletedId != null) {
            entity.setId(deletedId);
            baseMapper.restoreById(deletedId);
            return baseMapper.updateById(entity) > 0;
        }
        return baseMapper.insert(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(ProjectConfigBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("修改项目配置必须提供主键");
        }
        // 先确认记录存在，避免把「改不到」当成「改成功」
        ProjectConfigVo existing = queryById(bo.getId());
        assertProjectNoUnchanged(bo.getProjectNo(), existing.getProjectNo());
        ProjectConfig entity = MapstructUtils.convert(bo, ProjectConfig.class);
        // 项目编号建档后不可改：置 null 让 MyBatis-Plus 跳过该列（不写库、也不参与唯一键冲突）
        entity.setProjectNo(null);
        // 项目行身份不可通过项目编辑接口改变：parent_id 置 null 跳过；
        // 研究中心两列必须写空串而不是 null —— 它们是 NOT NULL 且实体上是 @TableField(updateStrategy = ALWAYS)，
        // 置 null 会真的把 NULL 写进库，直接报「Column 'research_center_no' cannot be null」。
        entity.setParentId(null);
        entity.setResearchCenterNo("");
        entity.setResearchCenterName("");
        return baseMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要删除的项目配置");
        }
        // 删项目连带软删它下面的研究中心，避免留下无主的中心行
        baseMapper.delete(Wrappers.<ProjectConfig>lambdaQuery().in(ProjectConfig::getParentId, ids));
        baseMapper.deleteByIds(ids);
        return Boolean.TRUE;
    }

    @Override
    public List<ProjectConfigVo> selectCentersByParentId(Long parentId) {
        if (parentId == null) {
            throw new ServiceException("缺少项目主键");
        }
        LambdaQueryWrapper<ProjectConfig> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ProjectConfig::getParentId, parentId);
        wrapper.orderByAsc(ProjectConfig::getId);
        return baseMapper.selectVoList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertCenterByBo(ProjectCenterBo bo) {
        if (bo.getParentId() == null) {
            throw new ServiceException("新增研究中心必须指定所属项目");
        }
        ProjectConfigVo parent = queryById(bo.getParentId());
        if (parent.getParentId() != null && parent.getParentId() != 0L) {
            throw new ServiceException("只能给项目挂研究中心，不能挂在研究中心下");
        }
        String centerNo = StringUtils.trim(bo.getResearchCenterNo());
        String centerName = StringUtils.trim(bo.getResearchCenterName());
        assertResearchCenterUnique(parent.getProjectNo(), centerNo, centerName, null);

        ProjectConfig entity = MapstructUtils.convert(bo, ProjectConfig.class);
        entity.setParentId(parent.getId());
        entity.setProjectNo(parent.getProjectNo());
        // 项目级字段只活在项目行上，中心行一律置空（合同三项同理）
        entity.setProjectName("");
        entity.setCustomer("");
        entity.setContractName(null);
        entity.setContractCode(null);
        entity.setStudyPlanNo(null);
        entity.setResearchCenterNo(centerNo);
        entity.setResearchCenterName(centerName);

        // 唯一键不含 del_flag：同编号同名中心被软删过，直接 insert 会撞键报「系统错误」——
        // 与项目行同一套处理：恢复旧行再写新值。
        Long deletedId = baseMapper.selectDeletedCenterId(parent.getProjectNo(), centerNo, centerName);
        if (deletedId != null) {
            entity.setId(deletedId);
            baseMapper.restoreById(deletedId);
            return baseMapper.updateById(entity) > 0;
        }
        return baseMapper.insert(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCenterByBo(ProjectCenterBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("修改研究中心必须提供主键");
        }
        ProjectConfigVo existing = queryById(bo.getId());
        if (existing.getParentId() == null || existing.getParentId() == 0L) {
            throw new ServiceException("该记录是项目行，不能走研究中心修改接口");
        }
        String centerNo = StringUtils.trim(bo.getResearchCenterNo());
        String centerName = StringUtils.trim(bo.getResearchCenterName());
        assertResearchCenterUnique(existing.getProjectNo(), centerNo, centerName, existing.getId());

        ProjectConfig entity = MapstructUtils.convert(bo, ProjectConfig.class);
        // 归属关系不可改：父节点、项目编号、项目级字段一律不写库
        entity.setParentId(null);
        entity.setProjectNo(null);
        entity.setProjectName(null);
        entity.setCustomer(null);
        entity.setContractName(null);
        entity.setContractCode(null);
        entity.setStudyPlanNo(null);
        entity.setResearchCenterNo(centerNo);
        entity.setResearchCenterName(centerName);
        return baseMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCentersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要删除的研究中心");
        }
        long projectRows = baseMapper.selectCount(Wrappers.<ProjectConfig>lambdaQuery()
            .in(ProjectConfig::getId, ids)
            .eq(ProjectConfig::getParentId, 0L));
        if (projectRows > 0) {
            throw new ServiceException("所选记录里混入了项目行，请分开删除");
        }
        baseMapper.deleteByIds(ids);
        return Boolean.TRUE;
    }

    /**
     * 组装查询条件：声明 Wrapper → 基础条件 → 分支条件 → 排序。
     * 搜索栏只有「项目编号」「客户名称」两项，其余条件不在这里凭空发散。
     */
    private LambdaQueryWrapper<ProjectConfig> buildQueryWrapper(ProjectConfigBo bo) {
        LambdaQueryWrapper<ProjectConfig> wrapper = Wrappers.lambdaQuery();

        // 列表只列项目行（parent_id=0）；研究中心行走展开区单独接口
        wrapper.eq(ProjectConfig::getParentId, 0L);
        wrapper.like(StringUtils.isNotBlank(bo.getProjectNo()), ProjectConfig::getProjectNo, bo.getProjectNo());
        wrapper.like(StringUtils.isNotBlank(bo.getCustomer()), ProjectConfig::getCustomer, bo.getCustomer());

        wrapper.orderByDesc(ProjectConfig::getId);
        return wrapper;
    }

    /**
     * 项目编号建档后不可修改：入参给了不同值就明确拒绝，别让「改了但没生效」变成隐性行为。
     * 入参为空视为「不打算改」，直接放行。
     */
    private void assertProjectNoUnchanged(String requested, String current) {
        if (StringUtils.isBlank(requested) || StringUtils.equals(requested, current)) {
            return;
        }
        throw new ServiceException("项目编号建档后不可修改：" + current, 400);
    }

    /**
     * 校验项目编号唯一（新增时用；同一租户下一个项目只能一条配置）。
     * 排除自身，避免「改其它字段」时误判冲突。
     */
    private void validateProjectNoUnique(String projectNo, Long excludeId) {
        if (StringUtils.isBlank(projectNo)) {
            return;
        }
        LambdaQueryWrapper<ProjectConfig> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ProjectConfig::getProjectNo, projectNo);
        wrapper.eq(ProjectConfig::getParentId, 0L);
        wrapper.ne(excludeId != null, ProjectConfig::getId, excludeId);
        if (baseMapper.exists(wrapper)) {
            throw new ServiceException("项目编号已存在：" + projectNo, 409);
        }
    }

    /**
     * 同一项目下（研究中心编号, 研究中心名称）唯一 —— 与唯一键 uk_project_config_node 对齐，先给友好提示。
     */
    private void assertResearchCenterUnique(String projectNo, String centerNo, String centerName, Long excludeId) {
        LambdaQueryWrapper<ProjectConfig> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ProjectConfig::getProjectNo, projectNo);
        wrapper.eq(ProjectConfig::getResearchCenterNo, centerNo);
        wrapper.eq(ProjectConfig::getResearchCenterName, centerName);
        wrapper.gt(ProjectConfig::getParentId, 0L);
        wrapper.ne(excludeId != null, ProjectConfig::getId, excludeId);
        if (baseMapper.exists(wrapper)) {
            throw new ServiceException("该项目下研究中心已存在：" + centerNo + " / " + centerName, 409);
        }
    }
}
