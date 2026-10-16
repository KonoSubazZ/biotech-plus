package org.dromara.report.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.report.domain.bo.SampleFlowHandleBo;
import org.dromara.report.domain.bo.SampleFlowQueryBo;
import org.dromara.report.domain.vo.SampleFlowHandleVo;
import org.dromara.report.domain.vo.SampleFlowRowVo;

/**
 * 样本流转 业务接口（报告管理 › 样本流转）
 *
 * @author <你的名字>
 */
public interface ISampleFlowService {

    /**
     * 分页查询样本流转列表（按页签给不同的状态与可见性过滤）。
     *
     * @param bo        查询条件（tab / 分析日期 / 样本编号 / 产品 / 显示全部）
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    TableDataInfo<SampleFlowRowVo> selectPageList(SampleFlowQueryBo bo, PageQuery pageQuery);

    /**
     * 页签「处理」：按页签决定是跳转报告解读某个页签（跳转前按需建/复用报告记录），
     * 还是就地更新状态（认领 30→31、40→41，数据随后自动出现在下一个页签）。
     *
     * @param bo 入参（tab / analysisId）
     * @return 处理结果（跳转目标页签 + reportId，或状态更新后的提示）
     */
    SampleFlowHandleVo handle(SampleFlowHandleBo bo);
}
