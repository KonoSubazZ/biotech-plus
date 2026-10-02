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
import org.dromara.project.domain.bo.ProductConfigBo;
import org.dromara.project.domain.vo.ProductConfigVo;
import org.dromara.project.service.IProductConfigService;
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
 * 产品配置
 * <p>
 * 路由前缀沿用 RuoYi 的 /{模块}/{实体} 约定；
 * 权限点位的命名见 ai-rules/01-readability.md 与设计文档中的角色矩阵。
 *
 * @author <你的名字>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/project/productConfig")
public class ProductConfigController extends BaseController {

    private final IProductConfigService productConfigService;

    /**
     * 分页查询产品配置列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    @SaCheckPermission("project:productConfig:list")
    @GetMapping("/list")
    public TableDataInfo<ProductConfigVo> list(ProductConfigBo bo, PageQuery pageQuery) {
        return productConfigService.selectPageList(bo, pageQuery);
    }

    /**
     * 查询全部启用中的产品（供其它页面的下拉使用）
     *
     * @return 产品列表
     */
    @SaCheckPermission("project:productConfig:list")
    @GetMapping("/activeList")
    public R<List<ProductConfigVo>> activeList() {
        return R.ok(productConfigService.selectActiveList());
    }

    /**
     * 获取产品配置详情
     *
     * @param id 主键
     * @return 详情
     */
    @SaCheckPermission("project:productConfig:query")
    @GetMapping("/{id}")
    public R<ProductConfigVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(productConfigService.queryById(id));
    }

    /**
     * 新增产品配置
     *
     * @param bo 业务对象
     * @return 操作结果
     */
    @SaCheckPermission("project:productConfig:add")
    @Log(title = "产品配置", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ProductConfigBo bo) {
        return toAjax(productConfigService.insertByBo(bo));
    }

    /**
     * 修改产品配置
     *
     * @param bo 业务对象
     * @return 操作结果
     */
    @SaCheckPermission("project:productConfig:edit")
    @Log(title = "产品配置", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ProductConfigBo bo) {
        return toAjax(productConfigService.updateByBo(bo));
    }

    /**
     * 删除产品配置
     * <p>
     * 被质控标准或数据周期策略引用时禁止删除，由 Service 抛 ServiceException。
     *
     * @param ids 主键集合
     * @return 操作结果
     */
    @SaCheckPermission("project:productConfig:remove")
    @Log(title = "产品配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(productConfigService.deleteByIds(List.of(ids)));
    }
}
