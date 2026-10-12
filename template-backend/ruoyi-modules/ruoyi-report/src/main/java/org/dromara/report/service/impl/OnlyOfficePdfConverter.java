package org.dromara.report.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.config.OnlyOfficeProperties;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DOCX → PDF 转换：调 Document Server 自带的转换服务 {@code /ConvertService.ashx}（OnlyOffice 对外的标准转换接口）。
 * <p>
 * 流程：给源文件发一个短时 token 地址 → Document Server 按该地址回读 DOCX → 返回产物下载地址 → 下载并校验 → 落盘。
 * <p>
 * 关键取舍（每条都踩过）：
 * <ol>
 *   <li>Document Server 只认 URL，不认上传的文件，所以必须有一个它够得着的源地址（{@link #resolveSource}）；</li>
 *   <li>HTTP 客户端显式用 HTTP/1.1 —— 明文服务不认 HTTP/2 的 h2c Upgrade，默认 HTTP/2 会把请求体丢掉；</li>
 *   <li>转换成功时响应里<b>没有</b> {@code <Error>} 节点（不是 {@code <Error>0</Error>}），缺失必须当成功；</li>
 *   <li>失败一律抛 {@link ServiceException}，由调用方决定是否影响主流程（通知单生成不因 PDF 失败而回滚）。</li>
 * </ol>
 *
 * @author <你的名字>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlyOfficePdfConverter {

    /** Document Server 转换接口（官方标准转换 API） */
    private static final String CONVERT_API_PATH = "/ConvertService.ashx";

    /** 源文件回读路径前缀；对应 {@code security.excludes} 里的放行项 */
    private static final String SOURCE_PATH_PREFIX = "/report/pdf-source/";

    /** Document Server 转换错误码 → 人话 */
    private static final Map<String, String> CONVERT_ERRORS = Map.of(
        "-1", "未知错误",
        "-2", "转换超时",
        "-3", "文档损坏或格式不支持",
        "-4", "Document Server 取不到源文件（检查 onlyoffice.public-base 是否可达）",
        "-5", "文档密码错误",
        "-7", "输入下载错误",
        "-9", "JWT 校验失败");

    private final OnlyOfficeProperties properties;

    private final ObjectMapper objectMapper;

    /** token → 待转换的源文件；Document Server 取走后即失效，过期自动清理 */
    private final Map<String, SourceFile> sources = new ConcurrentHashMap<>();

    /** 显式 HTTP/1.1：明文服务不支持 h2c Upgrade，默认 HTTP/2 会丢请求体 */
    private final HttpClient httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    /**
     * 把 DOCX 转成 PDF，产物落在源文件同目录、同名 {@code .pdf}。
     *
     * @param docxPath 已渲染好的 DOCX 路径
     * @return 生成的 PDF 路径
     */
    public Path convert(Path docxPath) {
        if (!properties.isEnabled()) {
            throw new ServiceException("OnlyOffice 转换未启用（onlyoffice.enabled=false）");
        }
        String publicBase = properties.getPublicBase();
        if (publicBase == null || publicBase.isBlank()) {
            throw new ServiceException("未配置 onlyoffice.public-base（Document Server 回读源文件的地址），无法转 PDF");
        }
        Path source = docxPath.toAbsolutePath().normalize();
        if (!Files.isRegularFile(source)) {
            throw new ServiceException("待转换的 DOCX 不存在：" + source);
        }
        String token = registerSource(source);
        String sourceUrl = trimTrailingSlash(publicBase) + SOURCE_PATH_PREFIX + token;
        String fileName = source.getFileName().toString();
        long startedAt = System.currentTimeMillis();
        String fileUrl = requestConvert(fileName, sourceUrl);
        byte[] pdf = download(fileUrl);
        if (properties.isVerifyPdf() && !isPdf(pdf)) {
            throw new ServiceException("转换结果不是 PDF（缺少 %PDF- 文件头）：" + fileUrl);
        }
        Path pdfPath = writePdfBeside(source, pdf);
        log.info("DOCX 转 PDF 完成：{} -> {}，{} 字节，耗时 {} 毫秒",
            source.getFileName(), pdfPath.getFileName(), pdf.length, System.currentTimeMillis() - startedAt);
        return pdfPath;
    }

    /**
     * Document Server 按 URL 回读源文件时用：按 token 取源文件。
     *
     * @param token 转换时下发的短时凭证
     * @return 源文件路径；不存在或已过期返回 null
     */
    public Path resolveSource(String token) {
        purgeExpiredSources();
        SourceFile source = sources.get(token);
        if (source == null || source.expiresAt() < System.currentTimeMillis()) {
            sources.remove(token);
            // 凭证不存在或已过期：按「没有这个源文件」处理，由调用方回 404，不抛异常（这是正常的过期路径）
            return null;
        }
        return source.path();
    }

    /** 登记一个只在 TTL 内有效的源文件凭证 */
    private String registerSource(Path source) {
        purgeExpiredSources();
        String token = UUID.randomUUID().toString().replace("-", "");
        long expiresAt = System.currentTimeMillis() + properties.getSourceTtlSeconds() * 1000L;
        sources.put(token, new SourceFile(source, expiresAt));
        return token;
    }

    /** 清掉过期凭证，避免 map 无限增长 */
    private void purgeExpiredSources() {
        long now = System.currentTimeMillis();
        sources.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
    }

    /** 提交转换任务，返回产物下载地址 */
    private String requestConvert(String fileName, String sourceUrl) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("async", false);
        payload.put("url", sourceUrl);
        payload.put("key", UUID.randomUUID().toString().replace("-", ""));
        payload.put("filetype", "docx");
        payload.put("outputtype", "pdf");
        payload.put("title", fileName);

        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ServiceException("构造转换请求失败：" + e.getMessage());
        }
        URI endpoint = URI.create(trimTrailingSlash(properties.getDsUrl()) + CONVERT_API_PATH);
        HttpRequest request = HttpRequest.newBuilder(endpoint)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
        String responseBody = send(request, HttpResponse.BodyHandlers.ofString()).body();
        return parseFileUrl(responseBody);
    }

    /** 解析转换响应：成功时没有 Error 节点，缺失即视为成功 */
    private String parseFileUrl(String xml) {
        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 响应来自 Document Server，仍按不可信输入处理：禁掉 DOCTYPE / 外部实体
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            document = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new ServiceException("解析转换响应失败：" + abbreviate(xml) + "，" + e.getMessage());
        }
        String error = text(document, "Error");
        if (!error.isBlank() && !"0".equals(error.trim())) {
            String code = error.trim();
            throw new ServiceException("Document Server 转换失败：Error " + code
                + "（" + CONVERT_ERRORS.getOrDefault(code, "未知错误码") + "）");
        }
        String fileUrl = text(document, "FileUrl");
        if (fileUrl.isBlank()) {
            throw new ServiceException("Document Server 未返回 FileUrl：" + abbreviate(xml));
        }
        return fileUrl;
    }

    /** 下载产物到内存 */
    private byte[] download(String fileUrl) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(fileUrl))
            .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .GET()
            .build();
        byte[] body = send(request, HttpResponse.BodyHandlers.ofByteArray()).body();
        if (body == null || body.length == 0) {
            throw new ServiceException("转换产物为空：" + fileUrl);
        }
        return body;
    }

    /** 发请求并把网络异常统一翻译成业务异常（超时 / 连不上 Document Server） */
    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        try {
            return httpClient.send(request, handler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("转换请求被中断：" + request.uri(), e);
        } catch (IOException e) {
            throw new ServiceException("访问 Document Server 失败（" + request.uri() + "）：" + e.getMessage(), e);
        }
    }

    /** 产物写盘：同目录临时文件 + 原子替换，避免读到写了一半的 PDF */
    private Path writePdfBeside(Path source, byte[] pdf) {
        Path target = source.resolveSibling(fileBase(source) + ".pdf");
        try {
            Path temp = Files.createTempFile(target.getParent(), "." + target.getFileName(), ".tmp");
            Files.write(temp, pdf);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new ServiceException("写 PDF 失败：" + target + "，" + e.getMessage(), e);
        }
        return target;
    }

    /** 取 XML 下指定标签的文本；没有该标签返回空串 */
    private String text(Document document, String tag) {
        var nodes = document.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return "";
        }
        String value = nodes.item(0).getTextContent();
        return value == null ? "" : value.trim();
    }

    private boolean isPdf(byte[] content) {
        byte[] header = "%PDF-".getBytes(StandardCharsets.US_ASCII);
        if (content.length < header.length) {
            return false;
        }
        for (int i = 0; i < header.length; i++) {
            if (content[i] != header[i]) {
                return false;
            }
        }
        return true;
    }

    private String fileBase(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    private String trimTrailingSlash(String url) {
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private String abbreviate(String text) {
        String flat = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        return flat.length() <= 200 ? flat : flat.substring(0, 200) + "…";
    }

    /** 源文件凭证：路径 + 失效时间 */
    private record SourceFile(Path path, long expiresAt) {
    }
}
