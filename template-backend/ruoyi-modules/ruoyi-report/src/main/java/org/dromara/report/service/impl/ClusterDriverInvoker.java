package org.dromara.report.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.config.ClusterDriverProperties;
import org.dromara.report.domain.dto.DriverInvokeResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 集群驱动调用器：起一个 {@code scanner.py} 子进程驱动**单个文件**，把脚本 JSON 结果解析出来。
 * <p>
 * 约定（见 {@code report-driver/readme.md}）：
 * <pre>
 * python3 scanner.py &lt;文件绝对路径&gt; &lt;上传根目录&gt;
 * </pre>
 * 脚本自己负责写库（文件级 data_file_status、样本级 analysis_data.drive_status 按「所有文件都 Loaded」重算），
 * 本类不碰数据库 —— 调用方拿结果后再把最新状态读回来展示。
 * <p>
 * 失败一律抛 {@code ServiceException}（含脚本原始输出片段），不返回半成品。
 *
 * @author <你的名字>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClusterDriverInvoker {

    /** 原始输出片段最多带多少字符（异常信息里用，避免整段日志进前端） */
    private static final int RAW_OUTPUT_MAX = 500;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ClusterDriverProperties properties;

    /**
     * 驱动一个文件。
     *
     * @param filePath 文件绝对路径（取 data_file_status.file_path）
     * @return 脚本结果（含 status / message）
     */
    public DriverInvokeResult invokeFile(String filePath) {
        requireConfigured();
        return invoke(filePath);
    }

    public DriverInvokeResult invokeBatch(String analysisDate, String product, String subbarcode) {
        requireBatchEnabled();
        if (analysisDate == null || !analysisDate.matches("[0-9]{8}")) {
            throw new ServiceException("分析日期必须为八位日期");
        }
        requirePathPart(product);
        requirePathPart(subbarcode);
        return invoke(analysisDate + "/" + product + "/" + subbarcode);
    }

    public void requireBatchEnabled() {
        requireConfigured();
        if (!properties.isBatchEnabled()) {
            throw new ServiceException("批次重新解读尚未开放，需完成 scanner 表结构兼容联调后开启 driver.batch-enabled");
        }
    }

    private void requirePathPart(String value) {
        if (value == null || value.isBlank() || value.contains("/") || value.contains("\\")
            || value.equals(".") || value.equals("..")) {
            throw new ServiceException("驱动路径中的产品或样本编号不合法");
        }
    }

    private DriverInvokeResult invoke(String filePath) {
        List<String> command = List.of(properties.getPython(), properties.getScannerPath(),
            filePath, properties.getRootDir());
        log.info("调用集群驱动：{}", String.join(" ", command));
        Process process = null;
        Path outputFile = null;
        try {
            outputFile = Files.createTempFile("report-scanner-", ".log");
            process = new ProcessBuilder(command).redirectErrorStream(true)
                .redirectOutput(outputFile.toFile()).start();
            if (!process.waitFor(properties.getTimeoutSeconds(), TimeUnit.SECONDS)) {
                throw new ServiceException("驱动脚本执行超时：" + filePath);
            }
            String output = Files.readString(outputFile, StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new ServiceException("驱动脚本执行失败：" + truncate(output));
            }
            return parse(output);
        } catch (IOException e) {
            throw new ServiceException("驱动脚本启动失败：" + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("驱动脚本执行被中断：" + filePath);
        } finally {
            cleanup(process, outputFile);
        }
    }

    private void cleanup(Process process, Path outputFile) {
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
        try {
            if (outputFile != null) {
                Files.deleteIfExists(outputFile);
            }
        } catch (IOException e) {
            log.warn("临时驱动日志保留：{}", outputFile, e);
        }
    }

    /** 脚本没配好就别猜：直接报清楚缺什么 */
    private void requireConfigured() {
        if (!properties.isEnabled()) {
            throw new ServiceException("集群驱动未启用（配置 driver.enabled=false）：请联系运维开启后再重新驱动");
        }
        if (!StringUtils.hasText(properties.getScannerPath())) {
            throw new ServiceException("未配置驱动脚本路径（driver.scanner-path），无法重新驱动");
        }
        if (!StringUtils.hasText(properties.getRootDir())) {
            throw new ServiceException("未配置上传根目录（driver.root-dir），无法重新驱动");
        }
    }

    private String readOutput(InputStream inputStream) {
        try {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ServiceException("读取驱动脚本输出失败：" + e.getMessage());
        }
    }

    /**
     * 解析脚本输出：stdout 里是「若干日志行 + 最后一段 JSON」，从最后一个行首的 '{' 开始解析。
     */
    private DriverInvokeResult parse(String output) {
        String text = output == null ? "" : output;
        int jsonStart = text.lastIndexOf("\n{");
        if (text.stripLeading().startsWith("{")) {
            jsonStart = text.indexOf('{') - 1;
        }
        DriverInvokeResult result = new DriverInvokeResult();
        if (jsonStart < 0 && !text.stripLeading().startsWith("{")) {
            result.setStatus("failed");
            result.setMessage("驱动脚本没有返回结果，请查看服务端日志");
            result.setRawOutput(truncate(text));
            log.warn("驱动脚本输出无法解析：{}", truncate(text));
            return result;
        }
        try {
            JsonNode node = MAPPER.readTree(text.substring(jsonStart + 1));
            result.setStatus(node.path("status").asText("failed"));
            result.setMessage(node.path("message").asText(""));
            result.setFileCount(node.path("data").path("file_count").asInt(0));
            return result;
        } catch (IOException e) {
            result.setStatus("failed");
            result.setMessage("驱动脚本返回结果解析失败：" + e.getMessage());
            result.setRawOutput(truncate(text));
            return result;
        }
    }

    private String truncate(String text) {
        if (text == null || text.length() <= RAW_OUTPUT_MAX) {
            return text;
        }
        return text.substring(text.length() - RAW_OUTPUT_MAX);
    }
}
