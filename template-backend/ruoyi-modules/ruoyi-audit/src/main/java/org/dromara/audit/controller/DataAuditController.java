package org.dromara.audit.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.audit.domain.bo.DataAuditBo;
import org.dromara.audit.domain.vo.DataAuditDetailVo;
import org.dromara.audit.domain.vo.DataAuditVo;
import org.dromara.audit.service.IDataAuditService;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 数据审计只有只读接口，记录只由受控业务事务写入。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/compliance/dataAudit")
public class DataAuditController {
    private final IDataAuditService service;

    @SaCheckPermission("compliance:dataAudit:list")
    @GetMapping("/list")
    public TableDataInfo<DataAuditVo> list(@Valid DataAuditBo bo, PageQuery pageQuery) {
        return service.selectPageList(bo, pageQuery);
    }

    @SaCheckPermission("compliance:dataAudit:query")
    @GetMapping("/{id}")
    public R<DataAuditDetailVo> detail(@PathVariable Long id) {
        return R.ok(service.queryById(id));
    }
}
