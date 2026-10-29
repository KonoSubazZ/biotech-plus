package org.dromara.project.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.project.domain.bo.ProjectConfigBo;
import org.dromara.project.domain.bo.ProjectCenterBo;
import org.dromara.project.domain.vo.CenterExcelRow;
import org.dromara.project.domain.vo.CenterImportResultVo;
import org.dromara.project.domain.vo.ProjectConfigVo;
import org.dromara.project.service.IProjectConfigService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * 某项目下的研究中心列表
     *
     * @param id 项目行主键
     * @return 研究中心列表
     */
    @SaCheckPermission("project:projectConfig:query")
    @GetMapping("/{id}/centers")
    public R<List<ProjectConfigVo>> centers(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(projectConfigService.selectCentersByParentId(id));
    }

    /**
     * 新增研究中心
     *
     * @param bo 业务对象
     * @return 操作结果
     */
    @SaCheckPermission("project:projectConfig:add")
    @Log(title = "研究中心", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/center")
    public R<Void> addCenter(@Validated @RequestBody ProjectCenterBo bo) {
        return toAjax(projectConfigService.insertCenterByBo(bo));
    }

    /**
     * 修改研究中心
     *
     * @param bo 业务对象
     * @return 操作结果
     */
    @SaCheckPermission("project:projectConfig:edit")
    @Log(title = "研究中心", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/center")
    public R<Void> editCenter(@Validated @RequestBody ProjectCenterBo bo) {
        return toAjax(projectConfigService.updateCenterByBo(bo));
    }

    /**
     * 删除研究中心（逻辑删除）
     *
     * @param ids 主键集合
     * @return 操作结果
     */
    @SaCheckPermission("project:projectConfig:remove")
    @Log(title = "研究中心", businessType = BusinessType.DELETE)
    @DeleteMapping("/center/{ids}")
    public R<Void> removeCenter(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(projectConfigService.deleteCentersByIds(List.of(ids)));
    }

    /**
     * 导出某项目下的研究中心清单（同一份文件就是导入模板；空项目导出只有表头）。
     * <p>
     * 前端用 useDownload 走 POST + 流式下载，所以这里用 POST 而不是 GET。
     *
     * @param id       项目行主键
     * @param response 响应体（xlsx 流）
     */
    @SaCheckPermission("project:projectConfig:export")
    @Log(title = "研究中心", businessType = BusinessType.EXPORT)
    @PostMapping("/{id}/center/export")
    public void exportCenters(@NotNull(message = "主键不能为空") @PathVariable Long id, HttpServletResponse response) {
        List<CenterExcelRow> rows = projectConfigService.exportCenters(id);
        ExcelUtil.exportExcel(rows, "研究中心", CenterExcelRow.class, response);
    }

    /**
     * 批量导入研究中心：先全量校验，任一行不通过就整体不导入。
     * <p>
     * 按「研究中心编号」匹配：对上就改、对不上就新增；不删除任何中心。
     * <p>
     * 刻意不加 {@code @RepeatSubmit}：它按「URL + 参数」去重，两次上传不同文件也是同一个 URL，
     * 会被判成重复提交直接 500（踩过）。防连点交给前端按钮 loading。
     *
     * @param id   项目行主键
     * @param file xlsx/xls（导出的那份文件，改完/追加后原样传回）
     * @return 导入结果（committed=false 表示校验没过、一行都没写）
     */
    @SaCheckPermission("project:projectConfig:import")
    @Log(title = "研究中心", businessType = BusinessType.IMPORT)
    @PostMapping(value = "/{id}/center/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<CenterImportResultVo> importCenters(@NotNull(message = "主键不能为空") @PathVariable Long id,
                                                 @RequestPart("file") MultipartFile file) {
        return R.ok(projectConfigService.importCenters(id, file));
    }
}
