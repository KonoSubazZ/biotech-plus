package org.dromara.report.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 集群驱动（外部 scanner.py）调用配置（driver.*）。
 * <p>
 * 样本流转「驱动异常」页签与集群对接页签的「重新驱动」按钮由后端**调脚本**实现：
 * {@code <python> <scanner-path> <文件绝对路径> <root-dir>}，脚本自己把文件级/样本级状态写回库，
 * 后端只负责触发 + 把最新状态读回来给页面（不重复实现驱动口径）。
 * <p>
 * 默认 {@code enabled=false}：没配好路径/解释器之前不猜、不兜底，按钮会明确报「未启用」。
 * 部署时用环境变量覆盖，例如 {@code DRIVER_SCANNER_PATH} / {@code DRIVER_ROOT_DIR}。
 *
 * @author <你的名字>
 */
@Data
@Component
@ConfigurationProperties(prefix = "driver")
public class ClusterDriverProperties {

    /** 是否启用「重新驱动」能力 */
    private boolean enabled = false;

    /** 外部驱动完成当前表结构兼容联调后才能开启。 */
    private boolean batchEnabled = false;

    /** python 解释器（后端容器里通常是 python3） */
    private String python = "python3";

    /** 驱动脚本 scanner.py 的绝对路径 */
    private String scannerPath;

    /** 上传根目录（scanner 的第 2 个参数 root_dir，用于从绝对路径里剥离相对路径） */
    private String rootDir;

    /** 单次驱动超时（秒） */
    private long timeoutSeconds = 300;
}
