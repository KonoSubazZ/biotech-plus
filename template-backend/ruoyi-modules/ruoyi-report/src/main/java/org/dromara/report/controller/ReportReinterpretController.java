package org.dromara.report.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.report.domain.bo.InterpretationReinterpretBo;
import org.dromara.report.domain.vo.AnalysisReportVo;
import org.dromara.report.service.impl.ReportReinterpretService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/report/interpretation")
public class ReportReinterpretController {
    private final ReportReinterpretService service;

    @SaCheckPermission("report:interpretation:enter")
    @RepeatSubmit
    @Log(title = "重新解读", businessType = BusinessType.INSERT)
    @PostMapping("/reinterpret")
    public R<AnalysisReportVo> reinterpret(@RequestBody @Validated InterpretationReinterpretBo bo) {
        return R.ok(service.reinterpret(bo));
    }
}
