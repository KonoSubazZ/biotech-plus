package org.dromara.drive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 驱动入库配置（{@code drive.*}）。
 * <p>
 * 与旧 Python 驱动（{@code report-driver/scanner.py}）的口径对应关系：
 * <ul>
 *   <li>{@code root-dir} = scanner 的第 2 个参数 root_dir，上传根目录；</li>
 *   <li>{@code create-by} = 旧脚本的 {@code created_by: 1}（本仓 create_by 是 sys_user.user_id）；</li>
 *   <li>{@code operator-name} = 旧脚本写 {@code analysis_data.drive_executed_by} 的 "tumor"。</li>
 * </ul>
 * 本模块同步执行（调用方要拿最终状态与文件数），所以没有队列/线程池配置。
 *
 * @author <你的名字>
 */
@Data
@Component
@ConfigurationProperties(prefix = "drive")
public class DriveProperties {

    /** 上传根目录（按环境配在 application-dev.yml / application-prod.yml；未配则调用时直接报错） */
    private String rootDir;

    /** 审计字段 create_by（bigint，本仓是用户 ID） */
    private Long createBy = 1L;

    /** 驱动执行人姓名，写 analysis_data.drive_executed_by */
    private String operatorName = "tumor";

    /** 明细批量插入每批行数（控制单条 INSERT 的大小，避免超出 max_allowed_packet） */
    private int batchInsertSize = 500;

    /** analysis_file_status.file_text 最多保留多少字符（超出截断，原文件路径仍完整保留） */
    private int fileTextMaxLength = 1048576;
}
