package org.dromara.report.service;

import org.dromara.report.domain.vo.ReportSignRefreshVo;

/**
 * 报告签名（上上签）—— 接口。
 * <p>
 * 状态口径（2026-10-16 定稿）：签名状态**合并进 {@code analysis_report.report_status}**，
 * 不再单独维护 {@code sign_status} 列：
 * <ul>
 *   <li>43 报告审核成功（瞬时）→ 审核通过后自动送签 → 50 报告签名中；</li>
 *   <li>50 → 回调成功 → 60 报告待发送（53 报告签名成功是瞬时态，不单独落库）；</li>
 *   <li>50 → 回调失败 → 52 报告签名失败（原因写 {@code report_sign_fail_reason}，可重新送签）。</li>
 * </ul>
 * 库列与目录已就位：{@code report_sign_flow_no} / {@code report_sign_time} /
 * {@code report_signed_file_path}（{@code <root>/report/signed/<yyyyMMdd>/}）。
 * <p>
 * 上上签未对接期间由 {@code ReportSignStubService} 顶替（假送签 + 模拟回调，见
 * {@code report.sign.provider=stub}）；接真实通道时新增实现类并换 provider 即可，调用方不改。
 *
 * @author <你的名字>
 */
public interface IReportSignService {

    /**
     * 提交签名：把该报告 {@code <root>/report/pdf/} 下的转化 PDF 送去签。
     *
     * @param reportId 报告ID
     * @return 签名流程号（写入 report_sign_flow_no；不允许送签时抛业务异常）
     */
    String submit(Long reportId);

    /**
     * 处理签名成功回调：把签名后 PDF 落到 {@code <root>/report/signed/}，回填
     * {@code report_signed_file_path}，状态 50 → 60 报告待发送。
     *
     * @param flowNo       签名流程号
     * @param signedPdfUrl 回调给的签名后 PDF 下载地址（未对接期可为空）
     */
    void handleCallback(String flowNo, String signedPdfUrl);

    /**
     * 处理签名失败回调：状态 50 → 52 报告签名失败，原因写 {@code report_sign_fail_reason}（可重新送签）。
     *
     * @param flowNo 签名流程号
     * @param reason 失败原因
     */
    void handleFailure(String flowNo, String reason);

    /**
     * 刷新签名状态：把当前所有「签名中」（50）且有合同号的报告逐个查通道 ——
     * 签署完成的下载签名件落 {@code <root>/report/signed/} 并转 60，通道报错转 52，未完成保持 50。
     * <p>
     * 「样本流转 › 报告签名」页签的手动刷新按钮走这里（批量，逐条互不影响，结果汇总返回）。
     *
     * @return 刷新结果汇总
     */
    ReportSignRefreshVo refreshSigning();
}
