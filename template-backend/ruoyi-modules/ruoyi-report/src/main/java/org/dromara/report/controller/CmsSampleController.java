package org.dromara.report.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
import org.dromara.report.domain.bo.CmsSampleBo;
import org.dromara.report.domain.vo.CmsSampleExcelRow;
import org.dromara.report.domain.vo.CmsSampleImportResultVo;
import org.dromara.report.domain.vo.CmsSampleVo;
import org.dromara.report.service.ICmsSampleService;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 样本信息（sample_biotech，药企 CMS 契约表）
 * <p>
 * 页面路由仍沿用菜单「样本管理 › 样本信息」(/qc/sample-info)，
 * 权限点也沿用 report:sampleInfo:*，保证菜单与角色授权无需改动。
 * 只暴露 45 个设计字段；列名不带 biotech_/deprecated_ 前缀。
 *
 * @author <你的名字>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/report/cmsSample")
public class CmsSampleController extends BaseController {

    private final ICmsSampleService cmsSampleService;

    /**
     * 分页查询样本信息
     *
     * @param bo        查询条件（样本编号 / 订单编号 / 客户名称 / 产品名称 / 样本类型 + params 里的送检日期范围）
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    @SaCheckPermission("report:sampleInfo:list")
    @GetMapping("/list")
    public TableDataInfo<CmsSampleVo> list(CmsSampleBo bo, PageQuery pageQuery) {
        return cmsSampleService.selectPageList(bo, pageQuery);
    }

    /**
     * 获取样本信息详情
     *
     * @param id 主键
     * @return 详情
     */
    @SaCheckPermission("report:sampleInfo:query")
    @GetMapping("/{id}")
    public R<CmsSampleVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(cmsSampleService.queryById(id));
    }

    /**
     * 新增样本信息
     *
     * @param bo 业务对象
     * @return 操作结果
     */
    @SaCheckPermission("report:sampleInfo:add")
    @Log(title = "样本信息", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody CmsSampleBo bo) {
        return toAjax(cmsSampleService.insertByBo(bo));
    }

    /**
     * 修改样本信息
     *
     * @param bo 业务对象
     * @return 操作结果
     */
    @SaCheckPermission("report:sampleInfo:edit")
    @Log(title = "样本信息", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody CmsSampleBo bo) {
        return toAjax(cmsSampleService.updateByBo(bo));
    }

    /**
     * 删除样本信息
     *
     * @param ids 主键集合
     * @return 操作结果
     */
    @SaCheckPermission("report:sampleInfo:remove")
    @Log(title = "样本信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(cmsSampleService.deleteByIds(List.of(ids)));
    }

    /**
     * 上传 Excel 批量导入样本信息
     * <p>
     * 表格列与导入模板一致；按「CMS 订单主键 + CMS 样本主键」有则更新、无则新增，
     * 单行出错只记录原因（返回结果里的 errors），不影响其它行。
     *
     * @param file Excel 文件
     * @return 导入结果
     */
    @SaCheckPermission("report:sampleInfo:import")
    @Log(title = "样本信息", businessType = BusinessType.IMPORT)
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<CmsSampleImportResultVo> importData(@RequestPart("file") MultipartFile file) {
        return R.ok(cmsSampleService.importExcel(file));
    }

    /**
     * 下载导入模板（只有表头的 xlsx）
     *
     * @param response 响应体
     */
    @SaCheckPermission("report:sampleInfo:import")
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        ExcelUtil.exportExcel(new ArrayList<>(), "样本信息导入模板", CmsSampleExcelRow.class, response);
    }
}
