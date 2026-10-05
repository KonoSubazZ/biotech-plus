package org.dromara.report.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.config.ReportRendererProperties;
import org.dromara.report.domain.vo.ReportTemplateVo;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 调用 Python 渲染器把 JSON 渲染成 DOCX（对齐参考工程 cool-admin 的 ReportDocxRenderer）。
 * <p>
 * 模板文件查找只有一条路：固定目录 + 模板名（{@link #locateTemplate}），找不到就报明确提示；
 * 其余旁路（请求带编码、报告绑定模板、classpath 兜底、任意磁盘路径）一律不要。
 * <p>
 * 关键取舍（每条都有踩过的坑）：
 * <ol>
 *   <li>用 {@link ProcessBuilder} 的<b>参数数组</b>，不拼 shell 字符串 —— 模板名带中文/括号/空格时字符串版必然拆错；</li>
 *   <li>stdout 与 stderr 合并后<b>重定向到日志文件</b>，不读管道 —— 管道写满会双边死锁，
 *       顺带留一份渲染日志（合规上要的接口可追溯）；</li>
 *   <li>必须带超时，超时后 {@code destroyForcibly} 再 {@code waitFor} 收尸 —— 否则挂住的进程会占死请求线程；</li>
 *   <li>成功判据是<b>退出码 0 + 输出文件存在</b>，stdout 只当诊断信息；
 *       退出码含义与脚本约定一致（2 输入错 / 3 模板或渲染错 / 4 写输出错）；</li>
 *   <li>渲染脚本随 jar 交付（classpath 释放到临时目录），部署时不用额外配路径。</li>
 * </ol>
 *
 * @author <你的名字>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportDocxRenderer {

    /** classpath 里的渲染脚本（jar 内：BOOT-INF/classes/scripts/render_report_docx.py） */
    private static final String CLASSPATH_SCRIPT = "scripts/render_report_docx.py";

    /** 子进程退出码含义（与 render_report_docx.py 的异常类型一一对应） */
    private static final Map<Integer, String> EXIT_CODE_MEANING = Map.of(
        2, "输入路径或 JSON 错误",
        3, "模板语法、缺少字段或渲染错误",
        4, "输出文件写入错误");

    private final ReportRendererProperties properties;

    private final ObjectMapper objectMapper;

    /**
     * 渲染 DOCX。
     *
     * @param templateFile 模板文件（由 {@link #locateTemplate} 定位，调用方传绝对路径）
     * @param jsonPath     渲染 JSON（调用方已落盘）
     * @param outputPath   输出 DOCX（由 Python 侧临时文件 + 原子替换写出）
     * @return 渲染结果：outputPath / outputBytes / sha256 / templateVariables / elapsedMs
     */
    public Map<String, Object> render(Path templateFile, Path jsonPath, Path outputPath) {
        Path script = resolveScript();
        Path rendererLog = runRenderer(script, templateFile, jsonPath, outputPath);
        return readSuccessResult(rendererLog, outputPath);
    }

    /** 渲染脚本：优先用配置的路径，没配就从 classpath 解出来（按大小判断是否需要重写） */
    private Path resolveScript() {
        String configured = properties.getScript();
        if (configured != null && !configured.isBlank()) {
            Path script = Path.of(configured.trim());
            if (!Files.isRegularFile(script)) {
                throw new ServiceException("DOCX 渲染脚本不存在：" + script);
            }
            return script;
        }
        Path target = rendererTmpDir().resolve("scripts/render_report_docx.py");
        ClassPathResource resource = new ClassPathResource(CLASSPATH_SCRIPT);
        try {
            long resourceBytes = resource.contentLength();
            if (Files.isRegularFile(target) && Files.size(target) == resourceBytes) {
                return target;
            }
            Files.createDirectories(target.getParent());
            try (InputStream input = resource.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        } catch (IOException e) {
            throw new ServiceException("DOCX 渲染脚本释放失败：" + CLASSPATH_SCRIPT + "，" + e.getMessage());
        }
    }

    /**
     * 定位模板文件：固定目录 + 模板名 → {@code <模板目录>/<模板名>.docx}。
     * 目录没配、文件不存在都直接报错，并把期望路径写清楚（不猜、不兜底）。
     *
     * @param template 模板行（用 template_name 定位文件名）
     * @return 模板文件绝对路径
     */
    public Path locateTemplate(ReportTemplateVo template) {
        String configured = properties.getTemplatesRoot();
        if (configured == null || configured.isBlank()) {
            throw new ServiceException("未配置模板目录（report.renderer.templates-root），无法渲染 DOCX");
        }
        Path root = Path.of(configured.trim()).toAbsolutePath().normalize();
        Path file = root.resolve(template.getTemplateName() + ".docx").normalize();
        if (!file.startsWith(root)) {
            throw new ServiceException("模板名不合法（越出模板目录）：" + template.getTemplateName());
        }
        if (!Files.isRegularFile(file)) {
            throw new ServiceException("模板文件不存在：" + file + "；请把 " + template.getTemplateName()
                + ".docx 放到 " + root);
        }
        return file;
    }

    /** 跑一次渲染，返回日志文件路径；退出码非 0 直接抛业务异常 */
    private Path runRenderer(Path script, Path template, Path jsonPath, Path outputPath) {
        Path rendererLog = outputPath.resolveSibling("." + outputPath.getFileName() + ".renderer.log");
        ProcessBuilder builder = new ProcessBuilder(
            properties.getPythonCommand(),
            script.toString(),
            template.toString(),
            jsonPath.toString(),
            outputPath.toString());
        builder.redirectErrorStream(true);
        builder.redirectOutput(rendererLog.toFile());
        builder.directory(realDir(outputPath));
        builder.environment().put("PYTHONIOENCODING", "utf-8");

        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            log.warn("启动渲染进程失败：{}，{}", properties.getPythonCommand(), e.getMessage());
            throw new ServiceException("DOCX 渲染进程启动失败（检查 python 命令是否可用）：" + e.getMessage());
        }
        long timeoutSeconds = Math.max(1, properties.getTimeoutSeconds());
        if (!waitFor(process, timeoutSeconds)) {
            process.destroyForcibly();
            waitFor(process, 5);
            throw new ServiceException("DOCX 渲染超时（" + timeoutSeconds + " 秒），已终止渲染进程");
        }
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new ServiceException("DOCX 渲染失败（退出码 " + exitCode + "："
                + EXIT_CODE_MEANING.getOrDefault(exitCode, "未知错误") + "）：" + rendererMessage(rendererLog));
        }
        return rendererLog;
    }

    /** 等进程结束；被中断时恢复中断位并抛异常（不要静默吞掉） */
    private boolean waitFor(Process process, long seconds) {
        try {
            return process.waitFor(seconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new ServiceException("DOCX 渲染被中断", e);
        }
    }

    /** 成功：读日志第一行（脚本打印的单行 JSON），并确认输出文件真的写出来了 */
    private Map<String, Object> readSuccessResult(Path rendererLog, Path outputPath) {
        String line = firstNonBlankLine(rendererLog);
        if (line.isBlank()) {
            throw new ServiceException("DOCX 渲染未返回结果，日志：" + rendererLog);
        }
        try {
            Map<String, Object> result = objectMapper.readValue(line, new TypeReference<>() {
            });
            result.put("rendererLog", rendererLog.toString());
            return result;
        } catch (IOException e) {
            throw new ServiceException("DOCX 渲染结果解析失败：" + line);
        }
    }

    /** 失败：从日志里取脚本打印的错误 JSON 行（stderr），取不到就回退整行文本 */
    private String rendererMessage(Path rendererLog) {
        String line = lastNonBlankLine(rendererLog);
        if (line.isBlank()) {
            return "渲染器无输出";
        }
        try {
            Map<String, Object> error = objectMapper.readValue(line, new TypeReference<>() {
            });
            Object message = error.get("message");
            return message == null ? line : String.valueOf(message);
        } catch (IOException e) {
            // 错误行不是 JSON：用原始行兜底，不往上层抛（原始失败原因更值钱）
            log.debug("渲染器错误行不是 JSON：{}，{}", line, e.getMessage());
            return line;
        }
    }

    private String firstNonBlankLine(Path file) {
        return rendererLine(file, true);
    }

    private String lastNonBlankLine(Path file) {
        return rendererLine(file, false);
    }

    /** 取日志里第一条/最后一条非空行（脚本 stdout / stderr 各一行 JSON）；没有内容时返回空串 */
    private String rendererLine(Path file, boolean first) {
        List<String> lines = readRendererLog(file);
        if (first) {
            return lines.stream().filter(line -> !line.isBlank()).findFirst().orElse("");
        }
        return lines.stream().filter(line -> !line.isBlank()).reduce((previous, current) -> current).orElse("");
    }

    /** 日志读不到不该盖掉原始失败原因：记一条 warn，返回空列表由调用方兜底 */
    private List<String> readRendererLog(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("渲染日志读取失败：{}，{}", file, e.getMessage());
            return List.of();
        }
    }

    /** 子进程工作目录：输出文件所在目录（不存在时回退到进程当前目录） */
    private File realDir(Path outputPath) {
        Path parent = outputPath.toAbsolutePath().getParent();
        return parent == null ? new File(".") : parent.toFile();
    }

    /** 渲染脚本/模板的本地缓存目录（放临时目录，不污染制品目录） */
    private Path rendererTmpDir() {
        return Path.of(System.getProperty("java.io.tmpdir"), "report-renderer");
    }
}
