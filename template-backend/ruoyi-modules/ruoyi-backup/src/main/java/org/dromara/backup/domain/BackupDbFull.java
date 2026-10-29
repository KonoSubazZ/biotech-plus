package org.dromara.backup.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * 数据库全量备份记录 backup_db_full
 * <p>
 * 一库一份：带恢复基线位点（mysqldump --master-data=2 里那条 CHANGE MASTER）与排除表清单。
 * 只存「成功上报的事实」——失败由脚本写日志，不入库（本期口径）。
 *
 * @author liushangzhi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("backup_db_full")
public class BackupDbFull extends BaseEntity {

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    // @fields:start
    /** 实例标识（backup.yml 的 host + 容器名口径） */
    private String instance;

    /** 库名 */
    private String dbName;

    /** 备份日期 */
    private Date backupDate;

    /** 类型：weekly 周定 / manual 手工补跑 */
    private String backupType;

    /** 本份 dump 排除的表（脚本按 backup.yml 展开后的实际表名，逗号分隔） */
    private String excludedTables;

    /** dump 内 CREATE TABLE 数（已扣除排除表） */
    private Integer tableCount;

    /** 该库实际表数（对账：tableCount + 排除数） */
    private Integer dbTableTotal;

    /** 基线 binlog 文件名 */
    private String binlogFile;

    /** 基线位点（PITR 起点） */
    private Long binlogPos;

    /** 本地压缩包路径 */
    private String localPath;

    /** 本地压缩包 MD5 */
    private String localMd5;

    /** 压缩包字节数 */
    private Long localSize;

    /** 本地是否已按保留策略清理：0 在 / 1 已清（OSS 仍在） */
    private Integer localPruned;

    /** OSS 对象路径（oss.enabled=false 时为空） */
    private String ossPath;

    /** OSS x-oss-meta-file-md5 回读值 */
    private String ossMetaMd5;

    /** 全流程耗时（毫秒） */
    private Long durationMs;

    /** 执行时 backup.yml 的 sha1 */
    private String configHash;

    /** 脚本 git 短 commit */
    private String scriptVersion;

    /** 预留状态：0 待 / 1 中 / 2 成功（本期只写 2） */
    private Integer backupStatus;

    /** 开始时间 */
    private Date startedAt;

    /** 完成时间 */
    private Date finishedAt;
    // @fields:end
}
