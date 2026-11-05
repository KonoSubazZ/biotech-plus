package org.dromara.report.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DOCX 渲染器配置（report.renderer.*）。
 * <p>
 * 默认值按「本机 / 容器里 python3 已装 docxtpl」设计；三种情况可覆盖：
 * <ul>
 *   <li>用 venv：设 {@code REPORT_RENDERER_PYTHON_COMMAND=/opt/venv/bin/python}；</li>
 *   <li>脚本放镜像固定路径：设 {@code REPORT_RENDERER_SCRIPT=/app/scripts/render_report_docx.py}；</li>
 *   <li>渲染慢的模板：设 {@code REPORT_RENDERER_TIMEOUT_SECONDS=120}。</li>
 * </ul>
 * 参考工程（cool-admin）用同一组环境变量名，部署配方可直接照搬。
 *
 * @author <你的名字>
 */
@Data
@Component
@ConfigurationProperties(prefix = "report.renderer")
public class ReportRendererProperties {

    /** python 可执行文件；生产/容器建议写绝对路径（默认按 PATH 找 python3） */
    private String pythonCommand = "python3";

    /** 渲染脚本路径；留空 = 用 classpath 里的 scripts/render_report_docx.py（随 jar 交付，不用配） */
    private String script;

    /**
     * 模板固定目录：所有模板文件放这一个目录，模板行用 template_path 记相对路径
     * （可带子目录，如 {@code notify/xxx.docx}）；绝对路径不入库，换环境只改这个配置。
     * 留空 = 渲染/上传直接报错（不做 classpath 兜底，模板必须由运维或后台上传到该目录）。
     * 环境变量 REPORT_RENDERER_TEMPLATES_ROOT。
     */
    private String templatesRoot;

    /** 单次渲染超时（秒）；小于 1 按 1 秒处理 */
    private long timeoutSeconds = 60;
}
