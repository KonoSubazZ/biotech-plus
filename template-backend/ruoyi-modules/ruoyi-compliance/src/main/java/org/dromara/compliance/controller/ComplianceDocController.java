package org.dromara.compliance.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
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
import org.dromara.compliance.domain.bo.ComplianceDocBo;
import org.dromara.compliance.domain.vo.ComplianceDocFileVo;
import org.dromara.compliance.domain.vo.ComplianceDocVo;
import org.dromara.compliance.service.IComplianceDocService;
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

import java.io.IOException;
import java.util.List;

/**
 * 3Q 文档管理
 * <p>
 * 记录类（文档库）：可增删改，删除是软删（实体 @TableLogic）。
 *
 * @author liushangzhi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/compliance/complianceDoc")
public class ComplianceDocController extends BaseController {

    private final IComplianceDocService complianceDocService;

    /**
     * 分页查询文档列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    @SaCheckPermission("compliance:complianceDoc:list")
    @GetMapping("/list")
    public TableDataInfo<ComplianceDocVo> list(ComplianceDocBo bo, PageQuery pageQuery) {
        return complianceDocService.selectPageList(bo, pageQuery);
    }

    /**
     * 获取文档详情
     *
     * @param id 主键
     * @return 详情
     */
    @SaCheckPermission("compliance:complianceDoc:query")
    @GetMapping("/{id}")
    public R<ComplianceDocVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(complianceDocService.queryById(id));
    }

    /**
     * 新增文档
     *
     * @param bo 业务对象
     * @return 操作结果
     */
    @SaCheckPermission("compliance:complianceDoc:add")
    @Log(title = "3Q 文档管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ComplianceDocBo bo) {
        return toAjax(complianceDocService.insertByBo(bo));
    }

    /**
     * 修改文档
     *
     * @param bo 业务对象
     * @return 操作结果
     */
    @SaCheckPermission("compliance:complianceDoc:edit")
    @Log(title = "3Q 文档管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ComplianceDocBo bo) {
        return toAjax(complianceDocService.updateByBo(bo));
    }

    /**
     * 批量删除文档（软删）
     *
     * @param ids 主键集合
     * @return 操作结果
     */
    @SaCheckPermission("compliance:complianceDoc:remove")
    @Log(title = "3Q 文档管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(complianceDocService.deleteByIds(List.of(ids)));
    }

    /**
     * 上传「文档」附件
     * <p>
     * 附件存服务器固定目录，**同名文件直接覆盖**；返回保存后的文件名供表单回填。
     *
     * @param file 上传文件
     * @return 保存后的文件名
     */
    @SaCheckPermission("compliance:complianceDoc:add")
    @Log(title = "3Q 文档管理", businessType = BusinessType.INSERT)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<ComplianceDocFileVo> upload(@RequestPart("file") MultipartFile file) {
        return R.ok(new ComplianceDocFileVo(complianceDocService.uploadFile(file)));
    }

    /**
     * 下载某条记录关联的「文档」
     *
     * @param id       记录主键
     * @param response 响应，直接写入文件流
     * @throws IOException 写响应流失败
     */
    @SaCheckPermission("compliance:complianceDoc:query")
    @GetMapping("/download/{id}")
    public void download(@NotNull(message = "主键不能为空") @PathVariable Long id,
                         HttpServletResponse response) throws IOException {
        complianceDocService.downloadFile(id, response);
    }
}
