package org.dromara.report.service;

import org.dromara.report.domain.vo.CmsSampleRefreshVo;

import java.time.LocalDate;
import java.util.Map;

/**
 * CMS 样本同步：拉取 → 45 字段比对 → 只更新有变化的非空字段 → 写 sync_log。
 *
 * @author <你的名字>
 */
public interface ICmsSyncService {

    /**
     * 按样本编号刷新单条（行内「刷新」按钮）。
     *
     * @param subbarcode 诺禾编号
     * @param operator   操作人（登录名）
     * @return 变更明细；changed=false 表示与 CMS 一致
     */
    CmsSampleRefreshVo syncBySubbarcode(String subbarcode, String operator);

    /**
     * 按日期区间批量同步（定时任务 / 历史补数据），单次最多 14 天。
     *
     * @param start    起始日期（含）
     * @param end      结束日期（含）
     * @param operator 操作人（SYSTEM 表示定时任务）
     * @return scanned / inserted / updated / unchanged / failed 统计
     */
    Map<String, Object> syncRange(LocalDate start, LocalDate end, String operator);
}
