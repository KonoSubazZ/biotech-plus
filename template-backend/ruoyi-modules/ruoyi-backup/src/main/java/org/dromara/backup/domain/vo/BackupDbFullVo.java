package org.dromara.backup.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.backup.domain.BackupDbFull;

import java.util.Date;

/**
 * 全量备份记录视图对象
 *
 * @author liushangzhi
 */
@Data
@NoArgsConstructor
@AutoMapper(target = BackupDbFull.class)
public class BackupDbFullVo {

    /** 主键 */
    private Long id;

    /** 实例标识 */
    private String instance;

    /** 库名 */
    private String dbName;

    /** 备份日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date backupDate;

    /** 类型：weekly / manual */
    private String backupType;

    /** 排除的表（逗号分隔） */
    private String excludedTables;

    /** dump 内表数（已扣排除） */
    private Integer tableCount;

    /** 库实际表数 */
    private Integer dbTableTotal;

    /** 基线 binlog 文件 */
    private String binlogFile;

    /** 基线位点 */
    private Long binlogPos;

    /** 本地压缩包路径 */
    private String localPath;

    /** 本地 MD5 */
    private String localMd5;

    /** 压缩包字节数 */
    private Long localSize;

    /** 本地是否已清理 */
    private Integer localPruned;

    /** OSS 路径 */
    private String ossPath;

    /** OSS 回读 MD5 */
    private String ossMetaMd5;

    /** 耗时（毫秒） */
    private Long durationMs;

    /** 配置 sha1 */
    private String configHash;

    /** 脚本版本 */
    private String scriptVersion;

    /** 预留状态 */
    private Integer backupStatus;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startedAt;

    /** 完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishedAt;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
