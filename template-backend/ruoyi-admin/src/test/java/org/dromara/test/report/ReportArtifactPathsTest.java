package org.dromara.test.report;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.utils.ReportArtifactPaths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

/**
 * 制品目录布局单测（报告按 analysis_date 分目录；通知沿用按生成当天分目录）。
 * 只跑本类：mvn -Plocal -DskipTests=false -Dtest=ReportArtifactPathsTest test
 *
 * @author <你的名字>
 */
@Tag("local")
@DisplayName("制品目录布局")
public class ReportArtifactPathsTest {

    private static final String ROOT = "/app/reports";
    private static final String DAY = "20261027";
    private static final LocalDate LOCAL_DAY = LocalDate.of(2026, 10, 27);

    @Test
    @DisplayName("报告：json/docx/pdf/signed 四个阶段都按 analysis_date 分目录")
    void reportDirsUseAnalysisDate() {
        Assertions.assertEquals("/app/reports/report/json/20261027",
            ReportArtifactPaths.reportDir(ROOT, ReportArtifactPaths.JSON, DAY).toString());
        Assertions.assertEquals("/app/reports/report/docx/20261027",
            ReportArtifactPaths.reportDir(ROOT, ReportArtifactPaths.DOCX, DAY).toString());
        Assertions.assertEquals("/app/reports/report/pdf/20261027",
            ReportArtifactPaths.reportDir(ROOT, ReportArtifactPaths.PDF, DAY).toString());
        Assertions.assertEquals("/app/reports/report/signed/20261027",
            ReportArtifactPaths.reportDir(ROOT, ReportArtifactPaths.SIGNED, DAY).toString());
        // 报告阶段顺序固定，建目录/发布都按它遍历
        Assertions.assertArrayEquals(new String[] {"json", "docx", "pdf", "signed"},
            ReportArtifactPaths.REPORT_STAGES);
    }

    @Test
    @DisplayName("驳回目录：report/rejected/<analysis_date>")
    void rejectedDir() {
        Assertions.assertEquals("/app/reports/report/rejected/20261027",
            ReportArtifactPaths.rejectedDir(ROOT, DAY).toString());
    }

    @Test
    @DisplayName("通知：沿用 <type>/<stage>/<yyyyMMdd>，json 平铺")
    void notifyKeepsOwnLayout() {
        Assertions.assertEquals("/app/reports/notify/docx/20261027",
            ReportArtifactPaths.docxDir(ROOT, ReportArtifactPaths.NOTIFY, LOCAL_DAY).toString());
        Assertions.assertEquals("/app/reports/notify/pdf/20261027",
            ReportArtifactPaths.pdfDir(ROOT, ReportArtifactPaths.NOTIFY, LOCAL_DAY).toString());
        Assertions.assertEquals("/app/reports/notify/signed/20261027",
            ReportArtifactPaths.signedDir(ROOT, ReportArtifactPaths.NOTIFY, LOCAL_DAY).toString());
        Assertions.assertEquals("/app/reports/notify/json",
            ReportArtifactPaths.jsonDir(ROOT, ReportArtifactPaths.NOTIFY).toString());
        Assertions.assertEquals(ReportArtifactPaths.stageDir(ROOT, "notify", "pdf", LOCAL_DAY),
            ReportArtifactPaths.pdfDir(ROOT, ReportArtifactPaths.NOTIFY, LOCAL_DAY));
    }

    @Test
    @DisplayName("日期串必须是 8 位数字，否则报错（不猜日期）")
    void invalidDayRejected() {
        Assertions.assertEquals(DAY, ReportArtifactPaths.requireDay(" 20261027 "));
        Assertions.assertThrows(ServiceException.class, () -> ReportArtifactPaths.requireDay(null));
        Assertions.assertThrows(ServiceException.class, () -> ReportArtifactPaths.requireDay(""));
        Assertions.assertThrows(ServiceException.class, () -> ReportArtifactPaths.requireDay("2026-10-27"));
        Assertions.assertThrows(ServiceException.class,
            () -> ReportArtifactPaths.reportDir(ROOT, ReportArtifactPaths.JSON, "2026102"));
    }

    @Test
    @DisplayName("day(null) 输出当天 yyyyMMdd（通知按生成当天用）")
    void dayIsEightDigits() {
        Assertions.assertTrue(ReportArtifactPaths.day(null).matches("\\d{8}"));
        Assertions.assertEquals("20261027", ReportArtifactPaths.day(LOCAL_DAY));
    }

    @Test
    @DisplayName("同类型内跨阶段同前缀、只换目录与扩展名")
    void sameFileBaseAcrossStages() {
        String base = "圣域_TKHS260146718-1A_SOP-CTP-009-NG0101-T002全血样本基因检测报告_V1";
        Assertions.assertEquals(base + ".docx",
            ReportArtifactPaths.reportDir(ROOT, ReportArtifactPaths.DOCX, DAY)
                .resolve(base + ".docx").getFileName().toString());
        Assertions.assertEquals(base + ".pdf",
            ReportArtifactPaths.reportDir(ROOT, ReportArtifactPaths.SIGNED, DAY)
                .resolve(base + ".pdf").getFileName().toString());
    }
}
