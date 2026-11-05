package org.dromara.backup.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.backup.domain.BackupDbBinlog;

import java.util.Date;

/**
 * binlog 增量归档视图对象
 * <p>
 * chainState 不是库里的列：Service 现算「本条起点是否等于上一条终点」，
 * 值域 origin（链起点）/ linked（接上）/ broken（未接上）。前端不自己算，避免两处口径。
 *
 * @author liushangzhi
 */
@Data
@NoArgsConstructor
@AutoMapper(target = BackupDbBinlog.class)
public class BackupDbBinlogVo {

    /** 主键 */
    private Long id;

    /** 实例标识 */
    private String instance;

    /** 归档日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date backupDate;

    /** 覆盖的库 */
    private String dbScope;

    /** 起点文件名 */
    private String startFile;

    /** 起点位点 */
    private Long startPos;

    /** 终点文件名 */
    private String endFile;

    /** 终点位点 */
    private Long endPos;

    /** 文件个数 */
    private Integer fileCount;

    /** 总字节 */
    private Long totalSize;

    /** 文件清单 JSON */
    private String filesJson;

    /** 本地目录 */
    private String localDir;

    /** 本地是否已清理 */
    private Integer localPruned;

    /** OSS 前缀 */
    private String ossPrefix;

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

    /** 位点链状态：origin / linked / broken */
    private String chainState;
}
