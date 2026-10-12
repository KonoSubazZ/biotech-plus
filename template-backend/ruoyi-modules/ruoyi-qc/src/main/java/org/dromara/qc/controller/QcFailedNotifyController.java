package org.dromara.qc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.qc.domain.bo.QcFailedNotifyBo;
import org.dromara.qc.domain.vo.QcFailedNotifyPrefillVo;
import org.dromara.qc.domain.vo.QcFailedNotifyResultVo;
import org.dromara.qc.service.IQcFailedNotifyService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * 样本质控不合格通知（处理）接口
 * <p>
 * 页面只做三件事：打开弹窗（预填）→ 编辑 → 生成文档；没有增删改查。
 *
 * @author <你的名字>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/qc/notify")
public class QcFailedNotifyController extends BaseController {

    private final IQcFailedNotifyService notifyService;

    /**
     * 处理弹窗预填：表头按样本编号回填 sample_file 字段 + 默认值 + 上次生成内容
     *
     * @param subbarcode  样本编号
     * @param qcCategory  质控类型（wet_lab 湿实验 / bioinfo 生信；为空按 bioinfo）
     * @return 预填数据
     */
    @SaCheckPermission("qc:notify:query")
    @GetMapping("/prefill")
    public R<QcFailedNotifyPrefillVo> prefill(
        @NotBlank(message = "样本编号不能为空") @RequestParam String subbarcode,
        @RequestParam(required = false) String qcCategory) {
        return R.ok(notifyService.prefill(subbarcode, qcCategory));
    }

    /**
     * 保存并生成通知文档（JSON → docxtpl → DOCX，并落一条生成记录）
     *
     * @param bo 表单
     * @return 生成结果
     */
    /**
     * 下载已生成的通知文档（按生成记录 id；前端要带令牌取 blob，不能直接 a href）
     *
     * @param id       生成记录 id
     * @param response 响应
     */
    @SaCheckPermission("qc:notify:query")
    @GetMapping("/{id}/docx")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        File file = notifyService.loadDocx(id);
        FileUtils.setAttachmentResponseHeader(response, file.getName());
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        Files.copy(file.toPath(), response.getOutputStream());
    }

    /**
     * 下载已生成通知单的 PDF（OnlyOffice 转换产物；转换失败时该记录没有 PDF）
     *
     * @param id       生成记录 id
     * @param response 响应
     */
    @SaCheckPermission("qc:notify:query")
    @GetMapping("/{id}/pdf")
    public void downloadPdf(@PathVariable Long id, HttpServletResponse response) throws IOException {
        File file = notifyService.loadPdf(id);
        FileUtils.setAttachmentResponseHeader(response, file.getName());
        response.setContentType("application/pdf");
        Files.copy(file.toPath(), response.getOutputStream());
    }

    @SaCheckPermission("qc:notify:generate")
    @Log(title = "质控不合格通知", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/generate")
    public R<QcFailedNotifyResultVo> generate(@Validated @RequestBody QcFailedNotifyBo bo) {
        return R.ok(notifyService.generate(bo));
    }
}
