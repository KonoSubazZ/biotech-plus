package org.dromara.backup.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * binlog 增量归档记录 backup_db_binlog
 * <p>
 * 一天一条：起点必须等于上一条的终点（位点链），终点取备份瞬间的 SHOW MASTER STATUS。
 * 注意 mysqlbinlog 的 --raw 模式会忽略 --stop-position，所以终点只能记录、回放时才截断。
 *
 * @author liushangzhi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("backup_db_binlog")
public class BackupDbBinlog extends BaseEntity {

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    // @fields:start
    /** 实例标识 */
    private String instance;

    /** 归档日期 */
    private Date backupDate;

    /** 本包覆盖的库（恢复时按 --database 过滤回放） */
    private String dbScope;

    /** 起点 binlog 文件名（= 上一条 end 或全量基线） */
    private String startFile;

    /** 起点位点 */
    private Long startPos;

    /** 终点 binlog 文件名（备份瞬间 SHOW MASTER STATUS） */
    private String endFile;

    /** 终点位点 */
    private Long endPos;

    /** binlog 文件个数 */
    private Integer fileCount;

    /** 本包总字节 */
    private Long totalSize;

    /** 文件清单 JSON：[{file,size,md5}] */
    private String filesJson;

    /** 本地目录 */
    private String localDir;

    /** 本地是否已清理：0 在 / 1 已清 */
    private Integer localPruned;

    /** OSS 前缀 */
    private String ossPrefix;

    /** 耗时（毫秒） */
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
