package org.dromara.backup.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.backup.domain.BackupDbFull;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * 全量备份上报对象
 * <p>
 * 校验都在默认组（Controller 用 @Validated 触发）：脚本漏传关键字段时直接 400，
 * 不写 groups 的约束在分组校验下不会执行，这里刻意不用分组。
 *
 * @author liushangzhi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = BackupDbFull.class, reverseConvertGenerate = false)
public class BackupDbFullBo extends BaseEntity {

    /** 实例标识；不传时取默认 */
    @Size(max = 64, message = "实例标识长度不能超过 64")
    private String instance;

    /** 库名 */
    @NotBlank(message = "库名不能为空")
    @Size(max = 64, message = "库名长度不能超过 64")
    private String dbName;

    /** 备份日期（yyyy-MM-dd） */
    @NotNull(message = "备份日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date backupDate;

    /** 类型：weekly / manual */
    @Pattern(regexp = "weekly|manual", message = "类型只能是 weekly 或 manual")
    private String backupType;

    /** 排除的表（逗号分隔） */
    @Size(max = 1000, message = "排除表清单长度不能超过 1000")
    private String excludedTables;

    /** dump 内 CREATE TABLE 数 */
    private Integer tableCount;

    /** 该库实际表数 */
    private Integer dbTableTotal;

    /** 基线 binlog 文件 */
    @Size(max = 64, message = "binlog 文件名长度不能超过 64")
    private String binlogFile;

    /** 基线位点 */
    private Long binlogPos;

    /** 本地压缩包路径 */
    @Size(max = 1024, message = "本地路径长度不能超过 1024")
    private String localPath;

    /** 本地压缩包 MD5 */
    @Size(max = 32, message = "MD5 长度不能超过 32")
    private String localMd5;

    /** 压缩包字节数 */
    private Long localSize;

    /** 本地是否已清理 */
    private Integer localPruned;

    /** OSS 对象路径 */
    @Size(max = 1024, message = "OSS 路径长度不能超过 1024")
    private String ossPath;

    /** OSS 回读 MD5 */
    @Size(max = 32, message = "MD5 长度不能超过 32")
    private String ossMetaMd5;

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
