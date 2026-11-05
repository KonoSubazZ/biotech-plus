package org.dromara.test.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.config.ReportRendererProperties;
import org.dromara.report.domain.vo.ReportTemplateVo;
import org.dromara.report.service.impl.ReportDocxRenderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 模板文件定位：root（配置）+ template_path（库里的相对路径）。
 * <p>
 * 只跑本类：mvn -Plocal -DskipTests=false -Dtest=ReportTemplateFileLocateTest test
 */
@Tag("local")
class ReportTemplateFileLocateTest {

    private ReportDocxRenderer renderer(Path root) {
        ReportRendererProperties props = new ReportRendererProperties();
        props.setTemplatesRoot(root.toString());
        return new ReportDocxRenderer(props, new ObjectMapper());
    }

    private ReportTemplateVo template(String name, String path) {
        ReportTemplateVo vo = new ReportTemplateVo();
        vo.setTemplateName(name);
        vo.setTemplatePath(path);
        return vo;
    }

    @Test
    void rootLevelPath(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("A.docx"), "x");
        assertEquals(root.resolve("A.docx"),
            renderer(root).locateTemplate(template("展示名可以和文件名不同", "A.docx")));
    }

    @Test
    void pathWithSubDirectory(@TempDir Path root) throws Exception {
        Files.createDirectories(root.resolve("notify"));
        Files.writeString(root.resolve("notify/N.docx"), "x");
        assertEquals(root.resolve("notify/N.docx"),
            renderer(root).locateTemplate(template("通知模板", "notify/N.docx")));
    }

    @Test
    void blankPathFallsBackToTemplateName(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("老模板.docx"), "x");
        assertEquals(root.resolve("老模板.docx"),
            renderer(root).locateTemplate(template("老模板", null)));
    }

    @Test
    void traversalIsRejected(@TempDir Path root) {
        ServiceException e = assertThrows(ServiceException.class,
            () -> renderer(root).locateTemplate(template("t", "../outside.docx")));
        assertTrue(e.getMessage().contains("不合法"));
    }

    @Test
    void missingFileReportsFullPath(@TempDir Path root) {
        ServiceException e = assertThrows(ServiceException.class,
            () -> renderer(root).locateTemplate(template("t", "missing.docx")));
        assertTrue(e.getMessage().contains("模板文件不存在"));
        assertTrue(e.getMessage().contains("missing.docx"));
    }
}
