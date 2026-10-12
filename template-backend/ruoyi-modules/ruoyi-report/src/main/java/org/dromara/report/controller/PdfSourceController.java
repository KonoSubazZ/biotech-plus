package org.dromara.report.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.report.service.OnlyOfficePdfConverter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 待转换源文件的回读入口。
 * <p>
 * Document Server 只能按 URL 取源文件，取的时候带不了登录态，所以这里按「短时 token」放行：
 * token 由 {@link OnlyOfficePdfConverter} 在每次转换时下发，只对那一个文件、只在 TTL 内有效，
 * 路径本身放在 {@code security.excludes}（见 application.yml）所以不走 Sa-Token 校验。
 * <p>
 * 只读、只按 token 取，不做目录列举，不接收路径参数。
 *
 * @author <你的名字>
 */
@RequiredArgsConstructor
@RestController
public class PdfSourceController {

    private final OnlyOfficePdfConverter pdfConverter;

    /**
     * 按 token 回读待转换的 DOCX
     *
     * @param token     转换时下发的短时凭证
     * @param response  响应
     */
    @GetMapping("/report/pdf-source/{token}")
    public void source(@PathVariable String token, HttpServletResponse response) throws IOException {
        Path file = pdfConverter.resolveSource(token);
        if (file == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        Files.copy(file, response.getOutputStream());
    }
}
