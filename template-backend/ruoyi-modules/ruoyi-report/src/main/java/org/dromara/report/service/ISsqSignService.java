package org.dromara.report.service;

import java.nio.file.Path;

/**
 * 上上签（iPaaS 网关）送签通道：按场景把一个 PDF 送去签。
 * <p>
 * 参考实现：{@code ~/project/ssq_api.py}（UAT 已跑通）。链路对三个场景一致：
 * 定位关键字拿坐标（LocateByMultiKeywords）→ 组装签署单（roles + appendingSignLabels）→
 * 送签（SendContractsSync）。差异只在关键字、角色与坐标偏移（见各方法说明）。
 * <p>
 * 本阶段口径：签名人账号按 routeOrder 固定（1/3 = 刘尚智，2 = 罗阔，见 impl 的 {@code PERSON_SIGNERS}），
 * 进配置后即可换成按业务数据取；送签后拿合同号，签署完成由调用方再调
 * {@link #queryContractStatus(String)} + {@link #downloadSignedPdf(String, java.nio.file.Path, String)}
 * 把签名件取回落盘（报告实体维度的流程仍由 {@code IReportSignService} 负责，本接口只管通道）。
 * <p>
 * 失败一律抛 {@code ServiceException}（含网关原始响应摘要），不返回半成品。
 *
 * @author <你的名字>
 */
public interface ISsqSignService {

    /**
     * 报告延迟：反馈人 / 复核人 签名 + 本企业印章（自动盖章位压在「反馈时间」）。
     *
     * @param pdfPath 待签署 PDF 的绝对路径（须含关键字：反馈人、复核人、反馈时间、复核时间）
     * @return 上上签合同号
     */
    String sendReportDelay(String pdfPath);

    /**
     * 报告交付：报告解读人 / 报告复核人 / 审批人 三方签名 + 本企业印章。
     *
     * @param pdfPath 待签署 PDF 的绝对路径（须含关键字：报告解读人：、报告复核人：、审批人：）
     * @return 上上签合同号
     */
    String sendReportDelivery(String pdfPath);

    /**
     * 生信质控不合格：反馈人 / 复核人 签名 + 本企业自动盖章（自动签 + 无验证方式）。
     *
     * @param pdfPath 待签署 PDF 的绝对路径（关键字同报告延迟）
     * @return 上上签合同号
     */
    String sendQcUnqualified(String pdfPath);

    /**
     * 查询合同签署状态（网关 {@code best-signquery-contract-info}）。
     * 文档没给状态枚举，本期原样返回网关文案、由调用方按「完成词」判断，联调确认字段后收敛。
     *
     * @param contractId 送签时拿到的合同号
     * @return 网关返回的状态描述（原样）
     */
    String queryContractStatus(String contractId);

    /**
     * 下载签名后 PDF 并落盘。
     *
     * @param contractId 合同号
     * @param targetDir  目标目录（如 {@code <root>/notify/signed/<yyyyMMdd>}），不存在则创建
     * @param fileBase   目标文件名（不带扩展名），与同记录的 PDF 同基名
     * @return 落盘后的 PDF 路径
     */
    Path downloadSignedPdf(String contractId, Path targetDir, String fileBase);
}
