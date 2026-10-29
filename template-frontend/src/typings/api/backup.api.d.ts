/**
 * 数据备份类型定义
 * <p>
 * 库里只有两张事实表：backup_db_full / backup_db_binlog（统一 backup_ 前缀）。
 * 策略与凭据在宿主机 backup/conf/backup.yml，页面只读，不提供任何写操作。
 */
declare namespace Api {
  namespace Backup {
    /** 全量备份记录（= 后端 BackupDbFullVo） */
    interface DbFull {
      id: number;
      instance: string;
      /** 库名 */
      dbName: string;
      /** 备份日期（yyyy-MM-dd） */
      backupDate: string;
      /** weekly 周定 / manual 手工补跑 */
      backupType: string;
      /** 本份 dump 排除的表（逗号分隔，空串 = 全备） */
      excludedTables?: string | null;
      /** dump 内表数（已扣排除） */
      tableCount?: number | null;
      /** 库实际表数 */
      dbTableTotal?: number | null;
      /** 基线 binlog 文件名 */
      binlogFile?: string | null;
      /** 基线位点（PITR 起点） */
      binlogPos?: number | null;
      localPath?: string | null;
      localMd5?: string | null;
      /** 压缩包字节数 */
      localSize?: number | null;
      /** 0 在本地 / 1 已按策略清理 */
      localPruned?: number | null;
      ossPath?: string | null;
      ossMetaMd5?: string | null;
      durationMs?: number | null;
      /** 执行时 backup.yml 的 sha1（页面显示前 8 位） */
      configHash?: string | null;
      scriptVersion?: string | null;
      backupStatus?: number | null;
      startedAt?: string | null;
      finishedAt?: string | null;
      createTime?: string | null;
    }

    /** binlog 增量归档记录（= 后端 BackupDbBinlogVo） */
    interface DbBinlog {
      id: number;
      instance: string;
      /** 归档日期（yyyy-MM-dd） */
      backupDate: string;
      /** 覆盖的库（逗号分隔） */
      dbScope: string;
      startFile: string;
      startPos: number;
      endFile: string;
      endPos: number;
      fileCount: number;
      totalSize: number;
      /** 文件清单 JSON：[{file,size,md5}] */
      filesJson?: string | null;
      localDir?: string | null;
      localPruned?: number | null;
      ossPrefix?: string | null;
      durationMs?: number | null;
      configHash?: string | null;
      scriptVersion?: string | null;
      backupStatus?: number | null;
      startedAt?: string | null;
      finishedAt?: string | null;
      createTime?: string | null;
      /** 位点链：origin 链起点 / linked 接上 / broken 未接上（后端现算） */
      chainState?: 'origin' | 'linked' | 'broken' | string;
    }

    /** 汇总（页面四张卡片） */
    interface DbSummary {
      latestFullDate?: string | null;
      latestFullOk?: number | null;
      latestFullTotal?: number | null;
      latestFullSize?: number | null;
      lastBinlogDate?: string | null;
      lastBinlogAt?: string | null;
      lastBinlogSize?: number | null;
      lastBinlogFileCount?: number | null;
      dbCount?: number | null;
      dbTableTotal?: number | null;
      excludedTableCount?: number | null;
      localFullCopies?: number | null;
      prunedFullCopies?: number | null;
      localBinlogDays?: number | null;
      ossLatestAt?: string | null;
    }

    /** 查询参数（全量与增量共用） */
    type DbBackupSearchParams = CommonType.RecordNullable<
      {
        dbName?: string;
        beginDate?: string;
        endDate?: string;
      } & Common.CommonSearchParams
    >;
  }
}
