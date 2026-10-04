package org.dromara.report.controller;

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
import org.dromara.report.domain.bo.ReportTemplateBo;
import org.dromara.report.domain.vo.ReportTemplateVo;
import org.dromara.report.service.IReportTemplateService;
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
import java.util.Map;

/**
 * 报告模板配置（report_template）
 * <p>
 * 一份模板 = 一个 DOCX 实体文件路径（template_path，预留）+ 一条输出范围配置（module_code）。
 * 模板与产品是多对多（product_template），在模板表单里用「关联产品」维护。
 *
 * @author <你的名字>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/report/template")
public class ReportTemplateController extends BaseController {

    private final IReportTemplateService reportTemplateService;

    /**
     * 分页查询报告模板列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    @SaCheckPermission("report:template:list")
    @GetMapping("/list")
    public TableDataInfo<ReportTemplateVo> list(ReportTemplateBo bo, PageQuery pageQuery) {
        return reportTemplateService.selectPageList(bo, pageQuery);
    }

    /**
     * 产品下拉选项（模板表单的「关联产品」用）
     *
     * @return 产品列表
     */
    @SaCheckPermission("report:template:query")
    @GetMapping("/productOptions")
    public R<List<Map<String, Object>>> productOptions() {
        return R.ok(reportTemplateService.selectProductOptions());
    }

    /**
     * 获取报告模板详情
     *
     * @param templateId 主键
     * @return 详情
     */
    @SaCheckPermission("report:template:query")
    @GetMapping("/{templateId}")
    public R<ReportTemplateVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long templateId) {
        return R.ok(reportTemplateService.queryById(templateId));
    }

    /**
     * 新增报告模板（含关联产品）
     *
     * @param bo 业务对象
     * @return 操作结果
     */
    @SaCheckPermission("report:template:add")
    @Log(title = "报告模板", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ReportTemplateBo bo) {
        return toAjax(reportTemplateService.insertByBo(bo));
    }

    /**
     * 修改报告模板（含关联产品）
     *
     * @param bo 业务对象
     * @return 操作结果
     */
    @SaCheckPermission("report:template:edit")
    @Log(title = "报告模板", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ReportTemplateBo bo) {
        return toAjax(reportTemplateService.updateByBo(bo));
    }

    /**
     * 删除报告模板（被报告引用时禁止删除）
     *
     * @param ids 主键集合
     * @return 操作结果
     */
    @SaCheckPermission("report:template:remove")
    @Log(title = "报告模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(reportTemplateService.deleteByIds(List.of(ids)));
    }
}
