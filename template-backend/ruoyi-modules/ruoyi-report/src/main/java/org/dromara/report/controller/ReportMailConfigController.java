package org.dromara.report.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.report.domain.ReportMailConfig;
import org.dromara.report.domain.bo.ReportMailConfigBo;
import org.dromara.report.domain.bo.ReportMailConfigQueryBo;
import org.dromara.report.domain.bo.ReportMailTestBo;
import org.dromara.report.domain.vo.ReportMailConfigVo;
import org.dromara.report.service.IReportMailConfigService;
import org.dromara.report.service.impl.ReportMailSender;
import org.dromara.report.service.impl.ReportMailVariables;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 报告邮件配置（报告管理 › 邮件配置）
 * <p>
 * 一个租户可以配**多条**发件邮箱：列表页管增删改，其中一条是「默认邮箱」，
 * 报告发送页默认用它、也可以改成别的（候选口径见 {@code /report/interpretation/mail-preview}）。
 * 列表与详情都**不回显密码**，只给 {@code passwordSet}；保存时密码传空串表示不修改。
 *
 * @author <你的名字>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/report/mail-config")
public class ReportMailConfigController extends BaseController {

    private final IReportMailConfigService mailConfigService;

    private final ReportMailSender mailSender;

    private final ReportMailVariables mailVariables;

    /**
     * 分页查询邮件配置（默认邮箱排最前；密码只回「是否已设置」）
     *
     * @param bo        查询条件（发件邮箱模糊）
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    @SaCheckPermission("report:mailConfig:list")
    @GetMapping("/list")
    public TableDataInfo<ReportMailConfigVo> list(ReportMailConfigQueryBo bo, PageQuery pageQuery) {
        return mailConfigService.selectPageList(bo, pageQuery);
    }

    /**
     * 按 id 查一条（编辑回显用）
     *
     * @param id 配置ID
     * @return 配置
     */
    @SaCheckPermission("report:mailConfig:list")
    @GetMapping("/{id}")
    public R<ReportMailConfigVo> getInfo(@PathVariable Long id) {
        return R.ok(mailConfigService.queryById(id));
    }

    /**
     * 新增一条发件邮箱
     *
     * @param bo 入参
     * @return 操作结果
     */
    @SaCheckPermission("report:mailConfig:edit")
    @RepeatSubmit()
    @Log(title = "邮件配置-新增", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody @Validated ReportMailConfigBo bo) {
        bo.setId(null);
        mailConfigService.save(bo);
        return R.ok();
    }

    /**
     * 修改一条发件邮箱（password 空串 = 不修改已保存的密码）
     *
     * @param bo 入参（必须带 id）
     * @return 操作结果
     */
    @SaCheckPermission("report:mailConfig:edit")
    @RepeatSubmit()
    @Log(title = "邮件配置-修改", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody @Validated ReportMailConfigBo bo) {
        if (bo.getId() == null) {
            return R.fail("缺少配置ID");
        }
        mailConfigService.save(bo);
        return R.ok();
    }

    /**
     * 删除一条发件邮箱（逻辑删；删掉的若是默认，会自动把最早的一条补成默认）
     *
     * @param id 配置ID
     * @return 操作结果
     */
    @SaCheckPermission("report:mailConfig:edit")
    @Log(title = "邮件配置-删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        mailConfigService.remove(id);
        return R.ok();
    }

    /**
     * 把某条设为「默认邮箱」（同租户其它行会被清成非默认）
     *
     * @param id 配置ID
     * @return 操作结果
     */
    @SaCheckPermission("report:mailConfig:edit")
    @RepeatSubmit()
    @Log(title = "邮件配置-设为默认", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/default")
    public R<Void> setDefault(@PathVariable Long id) {
        mailConfigService.changeDefault(id);
        return R.ok();
    }

    /**
     * 邮件模板可用变量 + 样例值（页面提示与试算用；与保存期校验同一份白名单）
     *
     * @return {variables: [...], sample: {变量: 样例值}}
     */
    @SaCheckPermission("report:mailConfig:list")
    @GetMapping("/variables")
    public R<Map<String, Object>> variables() {
        Map<String, Object> result = new HashMap<>();
        result.put("variables", mailVariables.getVariables());
        result.put("sample", mailVariables.getSample());
        return R.ok(result);
    }

    /**
     * 发送测试邮件（用指定配置真实发一封，验证该发件邮箱的账号密码可用）
     *
     * @param bo 入参（收件人 + 可选的配置ID；不传配置ID 用默认邮箱）
     * @return 操作结果
     */
    @SaCheckPermission("report:mailConfig:test")
    @RepeatSubmit()
    @Log(title = "邮件配置-测试发送", businessType = BusinessType.OTHER)
    @PostMapping("/test")
    public R<Void> test(@RequestBody @Validated ReportMailTestBo bo) {
        ReportMailConfig config = mailConfigService.loadForSend(bo.getConfigId());
        mailSender.sendTest(config, bo.getTo());
        return R.ok();
    }
}
