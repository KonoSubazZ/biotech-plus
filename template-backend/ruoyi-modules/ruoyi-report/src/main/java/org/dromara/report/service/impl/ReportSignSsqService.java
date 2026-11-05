package org.dromara.report.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.domain.vo.ReportSignRefreshVo;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.service.IReportSignService;
import org.dromara.report.service.ISsqSignService;
import org.dromara.report.utils.ReportArtifactPaths;
import org.dromara.report.utils.ReportStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.dromara.report.service.impl.revision.ReportOperationLock;

/**
 * 报告签名：真实上上签通道（`report.sign.provider=ssq`）。
 * <p>
 * 与假实现（{@link ReportSignStubService}）同一接口，区别只在这里真的连通道：
 * <ul>
 *   <li>{@link #submit}：拿报告转化后的 PDF 调 {@link ISsqSignService#sendReportDelivery}（三方签署 + 本企业印章），
 *       合同号落 {@code report_sign_flow_no}，状态 43/52 → 50，写 {@code report_sign_started_at}；</li>
 *   <li>{@link #handleCallback} / {@link #refreshSigning}：按合同号查签署状态（{@code COMPLETE} → 下载签名件落
 *       {@code <root>/report/signed/<yyyyMMdd>/}，回填 {@code report_signed_file_path} 与 {@code report_sign_time}，
 *       状态 → 60 报告待发送；失败 → 52 + 原因；未完成保持 50）。</li>
 * </ul>
 * 送签的三个个人签署人当前固定为「刘尚智 / 18034334041」（口径 2026-10-17，先跑通通道；
 * 后续按报告维度的解读人/复核人/病理师参数化，见 skill 的 §8）。
 *
 * @author <你的名字>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "report.sign.provider", havingValue = "ssq")
public class ReportSignSsqService implements IReportSignService {

    /** 上上签「签署完成」状态值（UAT 实测 data.status） */
    private static final String SSQ_COMPLETE = "COMPLETE";

    private final InterpretationMapper interpretationMapper;
    private final ReportOperationLock operationLock;

    private final ISsqSignService ssqSignService;

    /** 制品根目录（容器里固定挂 /app/reports；可在 application.yml 里用 report.artifact.root 覆盖） */
    @Value("${report.artifact.root:/app/reports}")
    private String artifactRoot;

    /**
     * 送签。
     * <p>
     * <b>独立事务（REQUIRES_NEW）</b>：审核通过后自动送签时，送签失败（通道连不上等）只影响送签这一步，
     * 不能把「审核通过」那个事务一起标记成 rollback-only —— 否则调用方即使 catch 了异常，
     * 提交时仍会 UnexpectedRollbackException，状态回滚到 41、页面报「发生未知异常」（实测踩到过）。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String submit(Long reportId) {
        return operationLock.call(reportId, () -> submitLocked(reportId));
    }

    private String submitLocked(Long reportId) {
        Map<String, Object> report = interpretationMapper.selectReportRow(reportId);
        if (report == null) {
            throw new ServiceException("报告不存在或已删除：reportId=" + reportId);
        }
        int status = toInt(report.get("status"));
        if (status != ReportStatus.REVIEW_APPROVED && status != ReportStatus.SIGN_FAILED) {
            throw new ServiceException("当前状态（" + ReportStatus.label(status) + "）不允许送签：reportId=" + reportId);
        }
        String pdfPath = text(report.get("reportFilePath"));
        if (!StringUtils.hasText(pdfPath)) {
            throw new ServiceException("报告还没有转化后的 PDF，无法送签：reportId=" + reportId
                + "（请先在「报告预览」生成报告）");
        }
        Path pdf = Path.of(pdfPath);
        if (!Files.isReadable(pdf)) {
            throw new ServiceException("报告 PDF 不可读，无法送签：reportId=" + reportId + "，path=" + pdfPath);
        }

        String contractId = ssqSignService.sendReportDelivery(pdfPath);
        int affected = interpretationMapper.updateReportSigning(reportId, contractId);
        if (affected == 0) {
            throw new ServiceException("报告状态已变化，请刷新后重试：reportId=" + reportId);
        }
        log.info("报告已送签（上上签）：reportId={} 合同号={} pdf={}", reportId, contractId, pdfPath);
        return contractId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleCallback(String flowNo, String signedPdfUrl) {
        Map<String, Object> row = requireReportByFlowNo(flowNo);
        operationLock.run(toLong(row.get("reportId")), () -> handleCallbackLocked(flowNo, signedPdfUrl));
    }

    private void handleCallbackLocked(String flowNo, String signedPdfUrl) {
        Map<String, Object> report = requireReportByFlowNo(flowNo);
        Long reportId = toLong(report.get("reportId"));
        // 上上签回调与手动「刷新签名」同一套逻辑：查状态 → 完成则下载签名件落盘并转 60
        syncOne(reportId, flowNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleFailure(String flowNo, String reason) {
        Map<String, Object> row = requireReportByFlowNo(flowNo);
        operationLock.run(toLong(row.get("reportId")), () -> handleFailureLocked(flowNo, reason));
    }

    private void handleFailureLocked(String flowNo, String reason) {
        Map<String, Object> report = requireReportByFlowNo(flowNo);
        Long reportId = toLong(report.get("reportId"));
        String failReason = StringUtils.hasText(reason) ? reason : "签名通道返回失败";
        int affected = interpretationMapper.updateReportSignResultIfCurrent(
            reportId, flowNo, ReportStatus.SIGN_FAILED, null, failReason);
        if (affected == 0) {
            throw new ServiceException("报告不在「签名中」，无法标记签名失败：reportId=" + reportId);
        }
        log.info("报告签名失败：reportId={} 合同号={} 原因={}", reportId, flowNo, failReason);
    }

    @Override
    public ReportSignRefreshVo refreshSigning() {
        List<Map<String, Object>> pending = interpretationMapper.selectReportsInSigning();
        ReportSignRefreshVo vo = new ReportSignRefreshVo();
        vo.setTotal(pending.size());
        List<String> messages = new ArrayList<>();
        for (Map<String, Object> row : pending) {
            Long reportId = toLong(row.get("reportId"));
            String flowNo = text(row.get("signFlowNo"));
            try {
                syncOne(reportId, flowNo);
            } catch (RuntimeException e) {
                // 单条失败不影响其它报告；原因汇总给页面
                messages.add("报告 #" + reportId + " 刷新失败：" + e.getMessage());
            }
        }
        // 按库里最新状态统计「完成 / 失败 / 仍签名中」（同步成功的已从 50 变成 60）
        int[] counts = countByStatus(pending);
        vo.setCompleted(counts[0]);
        vo.setFailed(vo.getFailed() + counts[1]);
        vo.setSigning(counts[2]);
        vo.setMessages(messages);
        log.info("刷新报告签名状态：共 {} 条，完成 {}，失败 {}，仍签名中 {}", vo.getTotal(), vo.getCompleted(),
            vo.getFailed(), vo.getSigning());
        return vo;
    }

    /** 刷新后按库里的最新状态统计（避免依赖调用前的快照）：返回 [完成, 失败, 仍签名中] */
    private int[] countByStatus(List<Map<String, Object>> rows) {
        int completed = 0;
        int failed = 0;
        int signing = 0;
        for (Map<String, Object> row : rows) {
            Integer code = interpretationMapper.selectReportSignStatus(toLong(row.get("reportId")));
            if (code == null) {
                continue;
            }
            if (code == ReportStatus.PENDING_SEND) {
                completed++;
            } else if (code == ReportStatus.SIGN_FAILED) {
                failed++;
            } else if (code == ReportStatus.SIGNING) {
                signing++;
            }
        }
        return new int[]{completed, failed, signing};
    }

    /** 同步单个报告的签署状态：查通道 → 完成则下载签名件落盘并转 60；异常抛给调用方处理 */
    private void syncOne(Long reportId, String flowNo) {
        operationLock.run(reportId, () -> syncOneLocked(reportId, flowNo));
    }

    private void syncOneLocked(Long reportId, String flowNo) {
        Map<String, Object> current = interpretationMapper.selectReportRow(reportId);
        if (current == null || toInt(current.get("status")) != ReportStatus.SIGNING
            || !flowNo.equals(text(current.get("signFlowNo")))) {
            throw new ServiceException("签名合同已失效或报告已完成签名，请刷新");
        }
        if (!StringUtils.hasText(flowNo)) {
            throw new ServiceException("该报告没有签名合同号，无法查询签署状态：reportId=" + reportId);
        }
        String channelStatus = ssqSignService.queryContractStatus(flowNo);
        if (!SSQ_COMPLETE.equalsIgnoreCase(channelStatus)) {
            log.info("报告仍在签署中：reportId={} 合同号={} 通道状态={}", reportId, flowNo, channelStatus);
            return;
        }

        Map<String, Object> report = interpretationMapper.selectReportRow(reportId);
        if (report == null) {
            throw new ServiceException("报告不存在或已删除：reportId=" + reportId);
        }
        String reportName = text(report.get("reportName"));
        // 签名件目录同样取 analysis_report.analysis_date（不取签名当天），与原始文件落在同一天目录；
        // 文件名沿用原 PDF 的基名，便于回溯到是哪份报告签回来的
        Path signedDir = ReportArtifactPaths.reportDir(artifactRoot, ReportArtifactPaths.SIGNED,
            ReportArtifactPaths.requireDay(text(report.get("analysisDate"))));
        String pdfPath = text(report.get("reportFilePath"));
        String fileBase;
        if (StringUtils.hasText(pdfPath)) {
            fileBase = Path.of(pdfPath).getFileName().toString().replaceAll("\\.pdf$", "");
        } else {
            fileBase = StringUtils.hasText(reportName) ? reportName : "report-" + reportId;
        }
        Path signed = ssqSignService.downloadSignedPdf(flowNo, signedDir, fileBase);

        int affected = interpretationMapper.updateReportSignResultIfCurrent(reportId, flowNo, ReportStatus.PENDING_SEND,
            signed.toString(), null);
        if (affected == 0) {
            throw new ServiceException("报告不在「签名中」，无法确认签名成功：reportId=" + reportId);
        }
        log.info("报告签名完成：reportId={} 合同号={} 签名件={}", reportId, flowNo, signed);
    }

    private Map<String, Object> requireReportByFlowNo(String flowNo) {
        if (!StringUtils.hasText(flowNo)) {
            throw new ServiceException("签名合同号不能为空");
        }
        Map<String, Object> report = interpretationMapper.selectReportBySignFlowNo(flowNo);
        if (report == null) {
            throw new ServiceException("找不到该签名合同号对应的报告：contractId=" + flowNo);
        }
        return report;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int toInt(Object value) {
        return Integer.parseInt(String.valueOf(value).trim());
    }

    private Long toLong(Object value) {
        return value == null ? null : Long.valueOf(String.valueOf(value).trim());
    }
}
