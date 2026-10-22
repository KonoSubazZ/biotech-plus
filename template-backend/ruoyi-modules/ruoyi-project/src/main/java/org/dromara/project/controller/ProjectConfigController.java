package org.dromara.project.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.project.domain.bo.ProjectConfigBo;
import org.dromara.project.domain.vo.ProjectConfigVo;
import org.dromara.project.service.IProjectConfigService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 项目配置
 * <p>
 * 路由前缀沿用 RuoYi 的 /{模块}/{实体} 约定；
 * 权限点位的命名见 ai-rules/01-readability.md 与设计文档中的角色矩阵。
 *
 * @author levi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/project/projectConfig")
public class ProjectConfigController extends BaseController {

    private final IProjectConfigService projectConfigService;

    /**
     * 分页查询项目配置列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    @SaCheckPermission("project:projectConfig:list")
    @GetMapping("/list")
    public TableDataInfo<ProjectConfigVo> list(ProjectConfigBo bo, PageQuery pageQuery) {
        return projectConfigService.selectPageList(bo, pageQuery);
    }

    /**
     * 获取项目配置详情
     *
     * @param id 主键
     * @return 详情
     */
    @SaCheckPermission("project:projectConfig:query")
    @GetMapping("/{id}")
    public R<ProjectConfigVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(projectConfigService.queryById(id));
    }

    /**
     * 新增项目配置
     *
     * @param bo 业务对象
     * @return 操作结果
     */
    @SaCheckPermission("project:projectConfig:add")
    @Log(title = "项目配置", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ProjectConfigBo bo) {
        return toAjax(projectConfigService.insertByBo(bo));
    }

    /**
     * 修改项目配置
     *
     * @param bo 业务对象
     * @return 操作结果
     */
    @SaCheckPermission("project:projectConfig:edit")
    @Log(title = "项目配置", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ProjectConfigBo bo) {
        return toAjax(projectConfigService.updateByBo(bo));
    }

    /**
     * 删除项目配置（逻辑删除；同一项目编号之后可以重新配一遍，新增时会恢复旧行）
     *
     * @param ids 主键集合
     * @return 操作结果
     */
    @SaCheckPermission("project:projectConfig:remove")
    @Log(title = "项目配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(projectConfigService.deleteByIds(List.of(ids)));
    }
}
