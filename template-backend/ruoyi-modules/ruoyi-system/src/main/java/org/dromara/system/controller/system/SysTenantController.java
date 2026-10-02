package org.dromara.system.controller.system;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysTenant;
import org.dromara.system.service.impl.SysTenantServiceImpl;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/system/tenant")
@RequiredArgsConstructor
public class SysTenantController {
    private final SysTenantServiceImpl service;

    private void checkAdmin() {
        if (!LoginHelper.isSuperAdmin()) throw new ServiceException("仅超级管理员可以管理租户");
    }

    @GetMapping("/list")
    public TableDataInfo<SysTenant> list(SysTenant tenant, PageQuery page) {
        checkAdmin();
        return service.list(tenant, page);
    }

    @GetMapping("/options")
    public R<List<SysTenant>> options() { return R.ok(service.options()); }

    @GetMapping("/{id}")
    public R<SysTenant> detail(@PathVariable Long id) {
        checkAdmin();
        return R.ok(service.detail(id));
    }

    @PostMapping
    @RepeatSubmit
    @Log(title = "租户管理", businessType = BusinessType.INSERT)
    public R<Void> create(@Valid @RequestBody SysTenant tenant) {
        checkAdmin();
        service.create(tenant);
        return R.ok();
    }

    @PutMapping
    @RepeatSubmit
    @Log(title = "租户管理", businessType = BusinessType.UPDATE)
    public R<Void> update(@Valid @RequestBody SysTenant tenant) {
        checkAdmin();
        service.update(tenant);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @Log(title = "租户管理", businessType = BusinessType.DELETE)
    public R<Void> delete(@PathVariable Long[] ids) {
        checkAdmin();
        service.delete(Arrays.asList(ids));
        return R.ok();
    }
}
