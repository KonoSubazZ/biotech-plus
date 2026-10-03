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
import org.dromara.report.domain.bo.SampleInfoBo;
import org.dromara.report.domain.vo.SampleInfoExcelRow;
import org.dromara.report.domain.vo.SampleInfoImportResultVo;
import org.dromara.report.domain.vo.SampleInfoVo;
import org.dromara.report.service.ISampleInfoService;
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
 * 样本信息（sample_file）
 * <p>
 * 路由前缀沿用 RuoYi 的 /{模块}/{实体} 约定。
 * 质控模块（湿实验质控 / 生信质控）的 qc_record.subbarcode 关联本表的样本编号，
 * 所以样本编号是业务键：新增查重、导入按它 upsert、删除检查是否被质控记录引用。
 *
 * @author <你的名字>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/report/sampleInfo")
public class SampleInfoController extends BaseController {

    private final ISampleInfoService sampleInfoService;

    /**
     * 分页查询样本信息
     *
     * @param bo        查询条件（样本编号 / 姓名 / 客户 / 录单癌种 / 录单产品 + params 里的委托日期范围）
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    @SaCheckPermission("report:sampleInfo:list")
    @GetMapping("/list")
    public TableDataInfo<SampleInfoVo> list(SampleInfoBo bo, PageQuery pageQuery) {
        return sampleInfoService.selectPageList(bo, pageQuery);
    }

    /**
     * 获取样本信息详情
     *
     * @param id 主键
     * @return 详情
     */
    @SaCheckPermission("report:sampleInfo:query")
    @GetMapping("/{id}")
    public R<SampleInfoVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(sampleInfoService.queryById(id));
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
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SampleInfoBo bo) {
        return toAjax(sampleInfoService.insertByBo(bo));
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
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SampleInfoBo bo) {
        return toAjax(sampleInfoService.updateByBo(bo));
    }

    /**
     * 删除样本信息（被质控记录引用时禁止删除，由 Service 抛 ServiceException）
     *
     * @param ids 主键集合
     * @return 操作结果
     */
    @SaCheckPermission("report:sampleInfo:remove")
    @Log(title = "样本信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(sampleInfoService.deleteByIds(List.of(ids)));
    }

    /**
     * 上传 Excel 批量导入样本信息
     * <p>
     * 表格列与导入模板一致；按「样本编号」有则更新、无则新增，
     * 单行出错只记录原因（返回结果里的 errors），不影响其它行。
     *
     * @param file Excel 文件
     * @return 导入结果
     */
    @SaCheckPermission("report:sampleInfo:import")
    @Log(title = "样本信息", businessType = BusinessType.IMPORT)
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<SampleInfoImportResultVo> importData(@RequestPart("file") MultipartFile file) {
        return R.ok(sampleInfoService.importExcel(file));
    }

    /**
     * 下载导入模板（只有表头的 xlsx，表头即列名约定）
     *
     * @param response 响应体
     */
    @SaCheckPermission("report:sampleInfo:import")
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        ExcelUtil.exportExcel(new ArrayList<>(), "样本信息导入模板", SampleInfoExcelRow.class, response);
    }
}
