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
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.qc.domain.bo.SampleNotifyBo;
import org.dromara.qc.domain.bo.SampleNotifyQueryBo;
import org.dromara.qc.domain.bo.SampleNotifySendBo;
import org.dromara.qc.domain.vo.SampleNotifyMailPreviewVo;
import org.dromara.qc.domain.vo.SampleNotifyPrefillVo;
import org.dromara.qc.domain.vo.SampleNotifyResultVo;
import org.dromara.qc.domain.vo.SampleNotifySendVo;
import org.dromara.qc.domain.vo.SampleNotifySignVo;
import org.dromara.qc.domain.vo.SampleNotifyVo;
import org.dromara.qc.service.ISampleNotifyDeliveryService;
import org.dromara.qc.service.ISampleNotifyService;
import org.dromara.report.domain.vo.DeliveryEmailLogVo;
import org.dromara.report.service.IDeliveryEmailLogService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * 样本通知台账 接口（四类通知：延期 / 样本不合格 / 湿实验不合格 / 干实验不合格）
 * <p>
 * 生成侧：预填 / 生成 / 列表 / 详情 / 下载；交付侧：送签 / 刷新签署 / 提交 / 发送。
 *
 * @author <你的名字>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/qc/notify")
public class SampleNotifyController extends BaseController {

    private final ISampleNotifyService notifyService;
    private final ISampleNotifyDeliveryService deliveryService;

    private final IDeliveryEmailLogService deliveryEmailLogService;

    /**
     * 处理弹窗预填：表头按样本编号回填 + 默认值 + 上次生成内容
     *
     * @param subbarcode 样本编号
     * @param notifyType 通知类型（delay / sample / wet_lab / bioinfo）
     * @return 预填数据
     */
    @SaCheckPermission("qc:notify:query")
    @GetMapping("/prefill")
    public R<SampleNotifyPrefillVo> prefill(
        @NotBlank(message = "样本编号不能为空") @RequestParam String subbarcode,
        @NotBlank(message = "通知类型不能为空") @RequestParam String notifyType) {
        return R.ok(notifyService.prefill(subbarcode, notifyType));
    }

    /**
     * 异常处理分页：只返回「已签名且已提交」的记录
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @SaCheckPermission("qc:notify:list")
    @GetMapping("/page")
    public TableDataInfo<SampleNotifyVo> page(SampleNotifyQueryBo bo, PageQuery pageQuery) {
        return notifyService.selectPageList(bo, pageQuery);
    }

    /**
     * 通知详情
     *
     * @param id 通知记录 id
     * @return 详情
     */
    @SaCheckPermission("qc:notify:query")
    @GetMapping("/{id}")
    public R<SampleNotifyVo> detail(@PathVariable Long id) {
        return R.ok(notifyService.queryById(id));
    }

    /** 发送记录：该通知的邮件发送历史（报告与通知共用 delivery_email_log） */
    @SaCheckPermission("qc:notify:query")
    @GetMapping("/{id}/mail-logs")
    public R<List<DeliveryEmailLogVo>> mailLogs(@PathVariable Long id) {
        return R.ok(deliveryEmailLogService.listByTarget(IDeliveryEmailLogService.TARGET_NOTIFY, id));
    }

    /**
     * 生成通知文档（JSON → DOCX → PDF → 落台账；同日未签名未提交可覆盖）
     *
     * @param bo 表单
     * @return 生成结果
     */
    @SaCheckPermission("qc:notify:generate")
    @Log(title = "样本通知生成", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/generate")
    public R<SampleNotifyResultVo> generate(@Validated @RequestBody SampleNotifyBo bo) {
        return R.ok(notifyService.generate(bo));
    }

    /**
     * 送签（按通知类型选上上签场景）
     *
     * @param id 通知记录 id
     * @return 合同号
     */
    @SaCheckPermission("qc:notify:sign")
    @Log(title = "样本通知送签", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/{id}/sign")
    public R<SampleNotifySignVo> sign(@PathVariable Long id) {
        return R.ok(deliveryService.sign(id));
    }

    /**
     * 刷新签署状态
     *
     * @param id 通知记录 id
     * @return 刷新结果
     */
    @SaCheckPermission("qc:notify:sign")
    @PostMapping("/{id}/refresh-sign")
    public R<SampleNotifySignVo> refreshSign(@PathVariable Long id) {
        return R.ok(deliveryService.refreshSign(id));
    }

    /**
     * 提交：签名完成后把通知推进到「待处理」
     *
     * @param id 通知记录 id
     * @return 操作结果
     */
    @SaCheckPermission("qc:notify:submit")
    @Log(title = "样本通知提交", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/{id}/submit")
    public R<Void> submit(@PathVariable Long id) {
        deliveryService.submit(id);
        return R.ok();
    }

    /**
     * 发送页参数（发件邮箱 / 收件人 / 抄送 / 标题 / 正文 / 附件名）
     *
     * @param id 通知记录 id
     * @return 发送页参数
     */
    @SaCheckPermission("qc:notify:send")
    @GetMapping("/{id}/mail-preview")
    public R<SampleNotifyMailPreviewVo> mailPreview(@PathVariable Long id) {
        return R.ok(deliveryService.mailPreview(id));
    }

    /**
     * 发送邮件
     *
     * @param id 通知记录 id
     * @param bo 页面填写的发送表单
     * @return 发送结果
     */
    @SaCheckPermission("qc:notify:send")
    @Log(title = "样本通知发送", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/{id}/send")
    public R<SampleNotifySendVo> send(@PathVariable Long id, @Validated @RequestBody SampleNotifySendBo bo) {
        return R.ok(deliveryService.send(id, bo));
    }

    /**
     * 下载已生成的通知文档
     *
     * @param id       通知记录 id
     * @param response 响应
     */
    @SaCheckPermission("qc:notify:query")
    @GetMapping("/{id}/docx")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        writeFile(notifyService.loadDocx(id), response,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    /**
     * 下载通知单 PDF
     *
     * @param id       通知记录 id
     * @param response 响应
     */
    @SaCheckPermission("qc:notify:query")
    @GetMapping("/{id}/pdf")
    public void downloadPdf(@PathVariable Long id, HttpServletResponse response) throws IOException {
        writeFile(notifyService.loadPdf(id), response, "application/pdf");
    }

    /**
     * 下载签名后 PDF
     *
     * @param id       通知记录 id
     * @param response 响应
     */
    @SaCheckPermission("qc:notify:query")
    @GetMapping("/{id}/signed-pdf")
    public void downloadSignedPdf(@PathVariable Long id, HttpServletResponse response) throws IOException {
        writeFile(notifyService.loadSignedPdf(id), response, "application/pdf");
    }

    private void writeFile(File file, HttpServletResponse response, String contentType) throws IOException {
        FileUtils.setAttachmentResponseHeader(response, file.getName());
        response.setContentType(contentType);
        Files.copy(file.toPath(), response.getOutputStream());
    }
}
