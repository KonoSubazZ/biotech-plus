package org.dromara.report.service;

/**
 * 报告签名（上上签）—— 预留接口，本期不实现（上上签未接入，无实现类、无 Controller 调用）。
 * <p>
 * 约定链路：转化 PDF（{@code <root>/report/pdf/<yyyyMMdd>/}）→ {@link #submit} 送签
 * （记 {@code sign_flow_no}、{@code sign_status=SIGNING}）→ 上上签回调 → {@link #handleCallback}
 * 解回签名后 PDF，落 {@code <root>/report/signed/<yyyyMMdd>/}，回填
 * {@code report_signed_file_path} / {@code sign_status=SIGNED} / {@code sign_time}。
 * <p>
 * 目录与库列已就位（见 {@code ReportArtifactPaths} 与 {@code update_5.6.17-report-sign-path.sql}），
 * 接上上签时只需补实现类；交付侧已按「signed → pdf → docx」回退。
 *
 * @author <你的名字>
 */
public interface IReportSignService {

    /**
     * 提交签名：把该报告 {@code <root>/report/pdf/} 下的转化 PDF 送去签。
     *
     * @param reportId 报告ID
     * @return 上上签流程号（写入 sign_flow_no；提交失败抛业务异常）
     */
    String submit(Long reportId);

    /**
     * 处理上上签回调：下载签名后 PDF 落 signed/，回填库列与状态。
     *
     * @param flowNo       上上签流程号
     * @param signedPdfUrl 回调给的签名后 PDF 下载地址
     */
    void handleCallback(String flowNo, String signedPdfUrl);
}
