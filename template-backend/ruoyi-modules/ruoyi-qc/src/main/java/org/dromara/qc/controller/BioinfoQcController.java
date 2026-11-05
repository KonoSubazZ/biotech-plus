package org.dromara.qc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.qc.domain.bo.BioinfoQcBo;
import org.dromara.qc.domain.bo.BioinfoQcReceiveBo;
import org.dromara.qc.domain.vo.BioinfoQcReceiveResultVo;
import org.dromara.qc.domain.vo.BioinfoQcTenantVo;
import org.dromara.qc.domain.vo.BioinfoQcVo;
import org.dromara.qc.service.IBioinfoQcService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 生信质控
 * <p>
 * 页面只读：数据全部由上游通过 {@code POST /qc/bioinfo/receive} 推送，故没有新增 / 修改 / 删除接口。
 * 推送方带固定 token（见 ApiTokenFilter）时无需登录，服务端会按 cms_sample.customer_name 解析归属药企。
 *
 * @author <你的名字>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/qc/bioinfo")
public class BioinfoQcController extends BaseController {

    private final IBioinfoQcService bioinfoQcService;

    /**
     * 分页查询生信质控列表
     *
     * @param bo        查询条件（样本编号 / 文库编号 模糊，状态 等值）
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    @SaCheckPermission("qc:bioinfo:list")
    @GetMapping("/list")
    public TableDataInfo<BioinfoQcVo> list(BioinfoQcBo bo, PageQuery pageQuery) {
        return bioinfoQcService.selectPageList(bo, pageQuery);
    }

    /**
     * 样本编号 → 药企解析（排查接口拒收用）：静默查 cms_sample.customer_name 与 sys_tenant
     *
     * @param subbarcode 样本编号
     * @return 解析结果
     */
    @SaCheckPermission("qc:bioinfo:query")
    @GetMapping("/tenant")
    public R<BioinfoQcTenantVo> tenant(@NotBlank(message = "样本编号不能为空") @RequestParam String subbarcode) {
        return R.ok(bioinfoQcService.resolveTenant(subbarcode));
    }

    /**
     * 接收上游推送的生信质控数据（批量；按样本编号有则更新、无则新增）
     *
     * @param bo 推送入参
     * @return 处理统计（新增 / 更新 / 拒收及原因）
     */
    @SaCheckPermission("qc:bioinfo:receive")
    @Log(title = "生信质控接口接收", businessType = BusinessType.INSERT)
    @PostMapping("/receive")
    public R<BioinfoQcReceiveResultVo> receive(@Validated @RequestBody BioinfoQcReceiveBo bo) {
        return R.ok(bioinfoQcService.receive(bo.getItems()));
    }

    /**
     * 获取生信质控详情
     *
     * @param id 主键
     * @return 详情
     */
    @SaCheckPermission("qc:bioinfo:query")
    @GetMapping("/{id}")
    public R<BioinfoQcVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(bioinfoQcService.queryById(id));
    }

    /**
     * 修改生信质控记录（人工修正指标值；样本编号不可改，改完重新判状态）
     *
     * @param bo 业务对象
     * @return 操作结果
     */
    @SaCheckPermission("qc:bioinfo:edit")
    @Log(title = "生信质控", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated @RequestBody BioinfoQcBo bo) {
        return toAjax(bioinfoQcService.updateByBo(bo));
    }

    /**
     * 批量删除生信质控记录（逻辑删除）
     *
     * @param ids 主键集合
     * @return 操作结果
     */
    @SaCheckPermission("qc:bioinfo:remove")
    @Log(title = "生信质控", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(bioinfoQcService.deleteByIds(List.of(ids)));
    }
}
