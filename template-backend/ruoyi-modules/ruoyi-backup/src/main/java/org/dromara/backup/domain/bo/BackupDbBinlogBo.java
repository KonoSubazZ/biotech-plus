package org.dromara.backup.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.backup.domain.BackupDbBinlog;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * binlog 增量归档上报对象
 *
 * @author liushangzhi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = BackupDbBinlog.class, reverseConvertGenerate = false)
public class BackupDbBinlogBo extends BaseEntity {

    /** 实例标识 */
    @Size(max = 64, message = "实例标识长度不能超过 64")
    private String instance;

    /** 归档日期（yyyy-MM-dd） */
    @NotNull(message = "归档日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date backupDate;

    /** 覆盖的库（逗号分隔） */
    @Size(max = 200, message = "库范围长度不能超过 200")
    private String dbScope;

    /** 起点文件名 */
    @NotBlank(message = "起点 binlog 文件名不能为空")
    @Size(max = 64, message = "起点文件名长度不能超过 64")
    private String startFile;

    /** 起点位点 */
    @NotNull(message = "起点位点不能为空")
    private Long startPos;

    /** 终点文件名 */
    @NotBlank(message = "终点 binlog 文件名不能为空")
    @Size(max = 64, message = "终点文件名长度不能超过 64")
    private String endFile;

    /** 终点位点 */
    @NotNull(message = "终点位点不能为空")
    private Long endPos;

    /** 文件个数 */
    private Integer fileCount;

    /** 总字节 */
    private Long totalSize;

    /** 文件清单 JSON */
    private String filesJson;

    /** 本地目录 */
    @Size(max = 1024, message = "本地目录长度不能超过 1024")
    private String localDir;

    /** 本地是否已清理 */
    private Integer localPruned;

    /** OSS 前缀 */
    @Size(max = 1024, message = "OSS 前缀长度不能超过 1024")
    private String ossPrefix;

    /** 耗时（毫秒） */
    private Long durationMs;

    /** 配置 sha1 */
    @Size(max = 40, message = "配置 sha1 长度不能超过 40")
    private String configHash;

    /** 脚本版本 */
    @Size(max = 32, message = "脚本版本长度不能超过 32")
    private String scriptVersion;

    /** 状态：本期固定 2 */
    private Integer backupStatus;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startedAt;

    /** 完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishedAt;
}
