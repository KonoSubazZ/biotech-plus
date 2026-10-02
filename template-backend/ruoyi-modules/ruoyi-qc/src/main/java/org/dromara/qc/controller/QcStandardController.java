package org.dromara.qc.controller;

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
import org.dromara.qc.domain.bo.QcStandardBo;
import org.dromara.qc.domain.vo.QcStandardVo;
import org.dromara.qc.service.IQcStandardService;
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
 * 质控标准
 * <p>
 * 路由前缀沿用 RuoYi 的 /{模块}/{实体} 约定；
 * 权限点位的命名见 ai-rules/01-readability.md 与设计文档中的角色矩阵。
 *
 * @author <你的名字>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/qc/qcStandard")
public class QcStandardController extends BaseController {

    private final IQcStandardService qcStandardService;

    /**
     * 分页查询质控标准列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    @SaCheckPermission("qc:qcStandard:list")
    @GetMapping("/list")
    public TableDataInfo<QcStandardVo> list(QcStandardBo bo, PageQuery pageQuery) {
        return qcStandardService.selectPageList(bo, pageQuery);
    }

    /**
     * 查询全部启用中的产品（供其它页面的下拉使用）
     *
     * @return 产品列表
     */
    @SaCheckPermission("qc:qcStandard:list")
    @GetMapping("/activeList")
    public R<List<QcStandardVo>> activeList() {
        return R.ok(qcStandardService.selectActiveList());
    }

    /**
     * 获取质控标准详情
     *
     * @param id 主键
     * @return 详情
     */
    @SaCheckPermission("qc:qcStandard:query")
    @GetMapping("/{id}")
    public R<QcStandardVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(qcStandardService.queryById(id));
    }

    /**
     * 新增质控标准
     *
     * @param bo 业务对象
     * @return 操作结果
     */
    @SaCheckPermission("qc:qcStandard:add")
    @Log(title = "质控标准", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody QcStandardBo bo) {
        return toAjax(qcStandardService.insertByBo(bo));
    }

    /**
     * 修改质控标准
     *
     * @param bo 业务对象
     * @return 操作结果
     */
    @SaCheckPermission("qc:qcStandard:edit")
    @Log(title = "质控标准", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody QcStandardBo bo) {
        return toAjax(qcStandardService.updateByBo(bo));
    }

    /**
     * 删除质控标准
     * <p>
     * 被质控标准或数据周期策略引用时禁止删除，由 Service 抛 ServiceException。
     *
     * @param ids 主键集合
     * @return 操作结果
     */
    @SaCheckPermission("qc:qcStandard:remove")
    @Log(title = "质控标准", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(qcStandardService.deleteByIds(List.of(ids)));
    }
}
