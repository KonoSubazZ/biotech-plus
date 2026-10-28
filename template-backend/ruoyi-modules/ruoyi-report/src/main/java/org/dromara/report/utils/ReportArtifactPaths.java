package org.dromara.report.utils;

import org.dromara.common.core.exception.ServiceException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * 制品目录布局（单一口径，报告与通知共用；一条规则可复刻到任何新文档类型）。
 * <p>
 * 根目录 = {@code report.artifact.root}（容器里固定挂 /app/reports，宿主 /home/liushangzhi/project/reports）：
 * <pre>
 * 报告（四个阶段都用 analysis_report.analysis_date 作日期目录，不再按报告/版本拆目录）：
 * &lt;root&gt;/report/json/&lt;yyyyMMdd&gt;/     正式生成 JSON
 * &lt;root&gt;/report/docx/&lt;yyyyMMdd&gt;/     原始 DOCX
 * &lt;root&gt;/report/pdf/&lt;yyyyMMdd&gt;/      转化 PDF
 * &lt;root&gt;/report/signed/&lt;yyyyMMdd&gt;/   签名后 PDF（日期同样取 analysis_date，不取签名当天）
 * &lt;root&gt;/report/rejected/&lt;yyyyMMdd&gt;/ 审核驳回后移出的原始制品
 *
 * 通知（沿用按生成当天分目录）：
 * &lt;root&gt;/notify/{docx,pdf,signed}/&lt;yyyyMMdd&gt;/
 * &lt;root&gt;/notify/json/
 * </pre>
 * 同一轮生成内各阶段共用同一文件名前缀（fileBase），靠目录区分阶段。
 *
 * @author <你的名字>
 */
public final class ReportArtifactPaths {

    /** 文档类型：报告 */
    public static final String REPORT = "report";

    /** 文档类型：质控不合格通知 */
    public static final String NOTIFY = "notify";

    /** 阶段：原始 DOCX（报告与通知同名） */
    public static final String DOCX = "docx";

    /** 阶段：转化 PDF */
    public static final String PDF = "pdf";

    /** 阶段：签名后 PDF（上上签回传，最终交付） */
    public static final String SIGNED = "signed";

    /** 阶段：报告/通知的 JSON */
    public static final String JSON = "json";

    /** 审核驳回归档目录 */
    public static final String REJECTED = "rejected";

    /** 报告阶段顺序（建目录、发布、清理都按这个顺序） */
    public static final String[] REPORT_STAGES = {JSON, DOCX, PDF, SIGNED};

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 日期目录名必须是 8 位数字（analysis_date 口径） */
    private static final Pattern DAY_PATTERN = Pattern.compile("\\d{8}");

    private ReportArtifactPaths() {
    }

    /**
     * 报告阶段目录：{@code <root>/report/<stage>/<analysisDate>}。
     * <p>
     * 目录已存在时直接复用，调用方 createDirectories 是幂等的。
     *
     * @param root         制品根目录
     * @param stage        json / docx / pdf / signed
     * @param analysisDate 分析日期（analysis_report.analysis_date，yyyyMMdd）
     * @return 阶段目录
     */
    public static Path reportDir(String root, String stage, String analysisDate) {
        return Paths.get(root, REPORT, stage, requireDay(analysisDate));
    }

    /** 审核驳回目录：{@code <root>/report/rejected/<analysisDate>} */
    public static Path rejectedDir(String root, String analysisDate) {
        return Paths.get(root, REPORT, REJECTED, requireDay(analysisDate));
    }

    /** 当日日期串（yyyyMMdd）：通知等"以生成当天为准"的场景用 */
    public static String day(LocalDate date) {
        return DAY.format(date == null ? LocalDate.now() : date);
    }

    /**
     * 校验并返回日期目录名：必须是 8 位数字，否则直接报错（不猜日期，避免把报告落到来路不明的目录）。
     *
     * @param analysisDate 分析日期
     * @return 校验通过的日期串
     */
    public static String requireDay(String analysisDate) {
        String value = analysisDate == null ? "" : analysisDate.trim();
        if (!DAY_PATTERN.matcher(value).matches()) {
            throw new ServiceException("分析日期不合法（需 yyyyMMdd）：" + analysisDate, 400);
        }
        return value;
    }

    /** 原始 DOCX 目录（通知）：<root>/<type>/docx/<yyyyMMdd> */
    public static Path docxDir(String root, String docType, LocalDate date) {
        return stageDir(root, docType, DOCX, date);
    }

    /** 转化 PDF 目录（通知）：<root>/<type>/pdf/<yyyyMMdd> */
    public static Path pdfDir(String root, String docType, LocalDate date) {
        return stageDir(root, docType, PDF, date);
    }

    /** 签名后 PDF 目录（通知）：<root>/<type>/signed/<yyyyMMdd> */
    public static Path signedDir(String root, String docType, LocalDate date) {
        return stageDir(root, docType, SIGNED, date);
    }

    /** JSON 目录（通知）：<root>/<type>/json */
    public static Path jsonDir(String root, String docType) {
        return Paths.get(root, docType, JSON);
    }

    /** <root>/<type>/<stage>/<yyyyMMdd> */
    public static Path stageDir(String root, String docType, String stage, LocalDate date) {
        return Paths.get(root, docType, stage, DAY.format(date));
    }
}
