package org.dromara.qc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
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
import org.dromara.qc.domain.bo.WetLabQcBo;
import org.dromara.qc.domain.bo.WetLabQcReceiveBo;
import org.dromara.qc.domain.vo.WetLabQcReceiveResultVo;
import org.dromara.qc.domain.vo.WetLabQcVo;
import org.dromara.qc.service.IWetLabQcService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 湿实验质控
 * <p>
 * 表是宽表（一行一个样本，每个质控项一列），形态对齐生信质控。
 * 数据全部由上游通过 {@code POST /qc/wetLabQc/receive} 推送，故没有新增 / 修改接口，只有查询与删除。
 * 推送方带固定 token（见 ApiTokenInterceptor，路径配在 application.yml 的 api.token.paths）时无需登录，
 * 服务端按 sample_file.subbarcode 取样本自身归属药企，把行落在该药企名下。
 *
 * @author <你的名字>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/qc/wetLabQc")
public class WetLabQcController extends BaseController {

    private final IWetLabQcService wetLabQcService;

    /**
     * 分页查询湿实验质控列表（一行一个样本）
     *
     * @param bo        查询条件（样本条码 模糊）
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    @SaCheckPermission("qc:wetLabQc:list")
    @GetMapping("/list")
    public TableDataInfo<WetLabQcVo> list(WetLabQcBo bo, PageQuery pageQuery) {
        return wetLabQcService.selectPageList(bo, pageQuery);
    }

    /**
     * 获取湿实验质控详情
     *
     * @param id 主键
     * @return 详情
     */
    @SaCheckPermission("qc:wetLabQc:query")
    @GetMapping("/{id}")
    public R<WetLabQcVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(wetLabQcService.queryById(id));
    }

    /**
     * 接收上游推送的湿实验质控数据（批量，一行一个样本；按样本编号有则合并、无则新增）
     *
     * @param bo 推送入参
     * @return 处理统计（新增 / 覆盖 / 拒收及原因）
     */
    @SaCheckPermission("qc:wetLabQc:receive")
    @Log(title = "湿实验质控接口接收", businessType = BusinessType.INSERT)
    @PostMapping("/receive")
    public R<WetLabQcReceiveResultVo> receive(@Validated @RequestBody WetLabQcReceiveBo bo) {
        return R.ok(wetLabQcService.receive(bo.getItems()));
    }

    /**
     * 修改湿实验质控记录（人工修正质控值；样本编号不可改，改完按质控标准重判 status）
     *
     * @param bo 业务对象
     * @return 操作结果
     */
    @SaCheckPermission("qc:wetLabQc:edit")
    @Log(title = "湿实验质控", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated @RequestBody WetLabQcBo bo) {
        return toAjax(wetLabQcService.updateByBo(bo));
    }

    /**
     * 批量删除湿实验质控记录（逻辑删除）
     *
     * @param ids 主键集合
     * @return 操作结果
     */
    @SaCheckPermission("qc:wetLabQc:remove")
    @Log(title = "湿实验质控", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(wetLabQcService.deleteByIds(List.of(ids)));
    }
}
