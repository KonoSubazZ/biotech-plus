package org.dromara.report.domain.dto;

import lombok.Data;

/**
 * 驱动脚本（scanner.py）一次调用的结果 —— 脚本 stdout 里的 JSON 契约：
 * <pre>
 * {"status": "success|failed", "message": "…", "data": {"file_names": "…", "file_count": 9, …}}
 * </pre>
 *
 * @author <你的名字>
 */
@Data
public class DriverInvokeResult {

    /** 脚本返回的状态：success / failed */
    private String status;

    /** 脚本给的说明（原样带回页面，失败原因就在里面） */
    private String message;

    /** 没解析出 JSON 时的原始输出片段（排错用，最长 500 字） */
    private String rawOutput;

    /** 脚本是否报告成功 */
    public boolean isSuccess() {
        return "success".equals(status);
    }
}
