package org.dromara.report.utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 制品目录布局（单一口径，报告与通知共用；一条规则可复刻到任何新文档类型）。
 * <p>
 * 根目录 = {@code report.artifact.root}（容器里固定挂 /app/reports，宿主 /home/liushangzhi/project/reports）：
 * <pre>
 * &lt;root&gt;/report/{docx,pdf,signed}/&lt;yyyyMMdd&gt;/   报告：原始 DOCX / 转化 PDF / 签名后 PDF
 * &lt;root&gt;/report/json/                            报告 JSON（平铺）
 * &lt;root&gt;/notify/{docx,pdf,signed}/&lt;yyyyMMdd&gt;/   通知：原始 DOCX / 转化 PDF / 签名后 PDF
 * &lt;root&gt;/notify/json/                            通知 JSON（平铺）
 * </pre>
 * 同类内三个阶段共用同一文件名前缀（fileBase），靠目录区分阶段。
 *
 * @author <你的名字>
 */
public final class ReportArtifactPaths {

    /** 文档类型：报告 */
    public static final String REPORT = "report";

    /** 文档类型：质控不合格通知 */
    public static final String NOTIFY = "notify";

    /** 阶段：原始 DOCX（docxtpl 渲染产物） */
    public static final String DOCX = "docx";

    /** 阶段：转化 PDF（OnlyOffice 转换产物） */
    public static final String PDF = "pdf";

    /** 阶段：签名后 PDF（上上签回传，最终交付；预留） */
    public static final String SIGNED = "signed";

    /** 阶段：报告/通知的 JSON（平铺，不按日期） */
    public static final String JSON = "json";

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private ReportArtifactPaths() {
    }

    /** 原始 DOCX 目录：&lt;root&gt;/&lt;type&gt;/docx/&lt;yyyyMMdd&gt; */
    public static Path docxDir(String root, String docType, LocalDate date) {
        return stageDir(root, docType, DOCX, date);
    }

    /** 转化 PDF 目录：&lt;root&gt;/&lt;type&gt;/pdf/&lt;yyyyMMdd&gt; */
    public static Path pdfDir(String root, String docType, LocalDate date) {
        return stageDir(root, docType, PDF, date);
    }

    /** 签名后 PDF 目录：&lt;root&gt;/&lt;type&gt;/signed/&lt;yyyyMMdd&gt; */
    public static Path signedDir(String root, String docType, LocalDate date) {
        return stageDir(root, docType, SIGNED, date);
    }

    /** JSON 目录：&lt;root&gt;/&lt;type&gt;/json */
    public static Path jsonDir(String root, String docType) {
        return Paths.get(root, docType, JSON);
    }

    /** &lt;root&gt;/&lt;type&gt;/&lt;stage&gt;/&lt;yyyyMMdd&gt; */
    public static Path stageDir(String root, String docType, String stage, LocalDate date) {
        return Paths.get(root, docType, stage, DAY.format(date));
    }
}
