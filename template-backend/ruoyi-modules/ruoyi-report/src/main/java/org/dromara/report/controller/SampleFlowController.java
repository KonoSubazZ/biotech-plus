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
import org.dromara.report.domain.bo.SampleFlowHandleBo;
import org.dromara.report.domain.bo.SampleFlowQueryBo;
import org.dromara.report.domain.vo.SampleFlowHandleVo;
import org.dromara.report.domain.vo.SampleFlowRowVo;
import org.dromara.report.service.ISampleFlowService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 样本流转（报告管理 › 样本流转）
 * <p>
 * 9 个页签的流转视图：驱动异常 / 生信看点 / 生信审核 / 报告待解读 / 报告解读中 / 报告待审核 /
 * 报告审核中 / 报告签名 / 报告发送。
 * <ul>
 *   <li>GET /report/flow/list：按页签（tab）分页查询，每行 = 一个分析批次 + 该批次最新报告；</li>
 *   <li>POST /report/flow/handle：页签「处理」——跳转报告解读对应页签，或就地认领（更新状态后数据自动流转）。</li>
 * </ul>
 * 权限口径：列表 = report:flow:list；处理 = report:flow:process + 所属页签的权限点
 * （report:flow:{driver|bioinfo-view|bioinfo-review|pending-interpret|interpreting|pending-review|
 * reviewing|signing|sending}，在 service 里逐次校验）。
 *
 * @author <你的名字>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/report/flow")
public class SampleFlowController extends BaseController {

    private final ISampleFlowService sampleFlowService;

    /**
     * 分页查询样本流转列表（按页签）
     *
     * @param bo        查询条件（tab 必填 / 分析日期 / 样本编号 / 产品 / 显示全部）
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    @SaCheckPermission("report:flow:list")
    @GetMapping("/list")
    public TableDataInfo<SampleFlowRowVo> list(SampleFlowQueryBo bo, PageQuery pageQuery) {
        return sampleFlowService.selectPageList(bo, pageQuery);
    }

    /**
     * 页签「处理」
     *
     * @param bo 入参（tab / analysisId）
     * @return 跳转目标（targetTab + reportId）或状态更新结果
     */
    @SaCheckPermission("report:flow:process")
    @RepeatSubmit()
    @Log(title = "样本流转-处理", businessType = BusinessType.UPDATE)
    @PostMapping("/handle")
    public R<SampleFlowHandleVo> handle(@RequestBody @Validated SampleFlowHandleBo bo) {
        return R.ok(sampleFlowService.handle(bo));
    }
}
