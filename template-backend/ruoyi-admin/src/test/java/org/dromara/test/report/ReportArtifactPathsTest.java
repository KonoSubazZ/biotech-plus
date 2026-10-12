package org.dromara.test.report;

import org.dromara.report.utils.ReportArtifactPaths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.time.LocalDate;

/**
 * 制品目录布局单测（报告与通知完全对称）。
 * 只跑本类：mvn -Plocal -DskipTests=false -Dtest=ReportArtifactPathsTest test
 *
 * @author <你的名字>
 */
@Tag("local")
@DisplayName("制品目录布局")
public class ReportArtifactPathsTest {

    private static final String ROOT = "/app/reports";
    private static final LocalDate DAY = LocalDate.of(2026, 10, 12);

    @Test
    @DisplayName("报告：docx/pdf/signed 按日期分目录，json 平铺")
    void reportDirs() {
        Assertions.assertEquals("/app/reports/report/docx/20261012",
            ReportArtifactPaths.docxDir(ROOT, ReportArtifactPaths.REPORT, DAY).toString());
        Assertions.assertEquals("/app/reports/report/pdf/20261012",
            ReportArtifactPaths.pdfDir(ROOT, ReportArtifactPaths.REPORT, DAY).toString());
        Assertions.assertEquals("/app/reports/report/signed/20261012",
            ReportArtifactPaths.signedDir(ROOT, ReportArtifactPaths.REPORT, DAY).toString());
        Assertions.assertEquals("/app/reports/report/json",
            ReportArtifactPaths.jsonDir(ROOT, ReportArtifactPaths.REPORT).toString());
    }

    @Test
    @DisplayName("通知：与报告完全同构")
    void notifyMirrorsReport() {
        Assertions.assertEquals("/app/reports/notify/docx/20261012",
            ReportArtifactPaths.docxDir(ROOT, ReportArtifactPaths.NOTIFY, DAY).toString());
        Assertions.assertEquals("/app/reports/notify/pdf/20261012",
            ReportArtifactPaths.pdfDir(ROOT, ReportArtifactPaths.NOTIFY, DAY).toString());
        Assertions.assertEquals("/app/reports/notify/signed/20261012",
            ReportArtifactPaths.signedDir(ROOT, ReportArtifactPaths.NOTIFY, DAY).toString());
        Assertions.assertEquals("/app/reports/notify/json",
            ReportArtifactPaths.jsonDir(ROOT, ReportArtifactPaths.NOTIFY).toString());
        // 一条规则：<root>/<type>/<stage>/<yyyyMMdd>
        Assertions.assertEquals(ReportArtifactPaths.stageDir(ROOT, "notify", "pdf", DAY),
            ReportArtifactPaths.pdfDir(ROOT, ReportArtifactPaths.NOTIFY, DAY));
    }

    @Test
    @DisplayName("同类型内跨阶段同前缀、只换目录与扩展名")
    void sameFileBaseAcrossStages() {
        String base = "YKHS260031036-1A_圣域_3-20261012120000-abcd1234";
        Assertions.assertEquals(base + ".docx",
            ReportArtifactPaths.docxDir(ROOT, ReportArtifactPaths.REPORT, DAY)
                .resolve(base + ".docx").getFileName().toString());
        Assertions.assertEquals(base + ".pdf",
            ReportArtifactPaths.pdfDir(ROOT, ReportArtifactPaths.REPORT, DAY)
                .resolve(base + ".pdf").getFileName().toString());
        Assertions.assertEquals(base + ".pdf",
            ReportArtifactPaths.signedDir(ROOT, ReportArtifactPaths.REPORT, DAY)
                .resolve(base + ".pdf").getFileName().toString());
    }

    @Test
    @DisplayName("所有子目录都在根目录内（ReportFileService 越界校验放行）")
    void allUnderRoot() {
        var root = Paths.get(ROOT).toAbsolutePath().normalize();
        for (String type : new String[] {ReportArtifactPaths.REPORT, ReportArtifactPaths.NOTIFY}) {
            Assertions.assertTrue(ReportArtifactPaths.docxDir(ROOT, type, DAY)
                .toAbsolutePath().normalize().startsWith(root));
            Assertions.assertTrue(ReportArtifactPaths.pdfDir(ROOT, type, DAY)
                .toAbsolutePath().normalize().startsWith(root));
            Assertions.assertTrue(ReportArtifactPaths.signedDir(ROOT, type, DAY)
                .toAbsolutePath().normalize().startsWith(root));
            Assertions.assertTrue(ReportArtifactPaths.jsonDir(ROOT, type)
                .toAbsolutePath().normalize().startsWith(root));
        }
    }
}
