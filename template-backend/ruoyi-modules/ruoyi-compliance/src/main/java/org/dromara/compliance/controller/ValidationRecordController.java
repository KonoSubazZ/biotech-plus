package org.dromara.compliance.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
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
import org.dromara.compliance.domain.bo.ValidationRecordBo;
import org.dromara.compliance.domain.vo.ValidationRecordFileVo;
import org.dromara.compliance.domain.vo.ValidationRecordVo;
import org.dromara.compliance.service.IValidationRecordService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 3Q 验证记录
 * <p>
 * append-only：不提供删除接口与 `:remove` 权限点位（合规上验证记录不可删改）。
 *
 * @author liushangzhi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/compliance/validationRecord")
public class ValidationRecordController extends BaseController {

    private final IValidationRecordService validationRecordService;

    /**
     * 分页查询验证记录列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    @SaCheckPermission("compliance:validationRecord:list")
    @GetMapping("/list")
    public TableDataInfo<ValidationRecordVo> list(ValidationRecordBo bo, PageQuery pageQuery) {
        return validationRecordService.selectPageList(bo, pageQuery);
    }

    /**
     * 获取验证记录详情
     *
     * @param id 主键
     * @return 详情
     */
    @SaCheckPermission("compliance:validationRecord:query")
    @GetMapping("/{id}")
    public R<ValidationRecordVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(validationRecordService.queryById(id));
    }

    /**
     * 新增验证记录
     *
     * @param bo 业务对象
     * @return 操作结果
     */
    @SaCheckPermission("compliance:validationRecord:add")
    @Log(title = "3Q 验证记录", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ValidationRecordBo bo) {
        return toAjax(validationRecordService.insertByBo(bo));
    }

    /**
     * 修改验证记录
     *
     * @param bo 业务对象
     * @return 操作结果
     */
    @SaCheckPermission("compliance:validationRecord:edit")
    @Log(title = "3Q 验证记录", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ValidationRecordBo bo) {
        return toAjax(validationRecordService.updateByBo(bo));
    }

    /**
     * 上传「验证文档」附件
     * <p>
     * 附件存服务器固定目录，**同名文件直接覆盖**；返回保存后的文件名供表单回填。
     *
     * @param file 上传文件
     * @return 保存后的文件名
     */
    @SaCheckPermission("compliance:validationRecord:add")
    @Log(title = "3Q 验证记录", businessType = BusinessType.INSERT)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<ValidationRecordFileVo> upload(@RequestPart("file") MultipartFile file) {
        return R.ok(new ValidationRecordFileVo(validationRecordService.uploadFile(file)));
    }

    /**
     * 下载某条记录关联的「验证文档」
     *
     * @param id       记录主键
     * @param response 响应，直接写入文件流
     * @throws IOException 写响应流失败
     */
    @SaCheckPermission("compliance:validationRecord:query")
    @GetMapping("/download/{id}")
    public void download(@NotNull(message = "主键不能为空") @PathVariable Long id,
                         HttpServletResponse response) throws IOException {
        validationRecordService.downloadFile(id, response);
    }
}
