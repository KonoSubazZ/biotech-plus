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
      /** 本包覆盖的库（逗号分隔；绑定备份=一组库一个包） */
      dbScope: string;
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

    /** 样本备份列表行（= 后端 BackupSampleRowVo，按 sample_id 聚合） */
    interface SampleRow {
      /** cms_sample.id */
      sampleId: number;
      subbarcode: string;
      projectNo?: string | null;
      orderCode?: string | null;
      productBio?: string | null;
      researchCenter?: string | null;
      /** 文件总数 */
      fileTotal: number;
      /** 已成功数（含跳过） */
      fileDone: number;
      /** 失败数 */
      fileFailed: number;
      /** 跳过数（OSS 已有同 MD5） */
      fileSkipped: number;
      totalBytes?: number | null;
      backedBytes?: number | null;
      /** 样本级状态：0 待备份 / 2 全部成功 / 3 部分成功 / 4 全部失败 */
      backupStatus?: number | null;
      lastBackupAt?: string | null;
    }

    /** 样本备份文件明细（= 后端 BackupSampleFileVo） */
    interface SampleFile {
      id: number;
      reportId?: number | null;
      fileCategory: string;
      fileName?: string | null;
      serverHost?: string | null;
      serverPath?: string | null;
      fileSize?: number | null;
      fileMd5?: string | null;
      ossPath?: string | null;
      ossMetaMd5?: string | null;
      /** 0 待备份 / 1 备份中 / 2 成功 / 3 失败 / 4 跳过 */
      backupStatus?: number | null;
      backupTime?: string | null;
      failReason?: string | null;
    }

    /** 样本备份查询参数 */
    type SampleSearchParams = CommonType.RecordNullable<
      {
        subbarcode?: string;
        projectNo?: string;
        orderCode?: string;
        productBio?: string;
        backupStatus?: number;
        beginDate?: string;
        endDate?: string;
      } & Common.CommonSearchParams
    >;

    /** 查询参数（全量与增量共用） */
    type DbBackupSearchParams = CommonType.RecordNullable<
      {
        /** 覆盖的库（包含该值即命中） */
        dbScope?: string;
        beginDate?: string;
        endDate?: string;
      } & Common.CommonSearchParams
    >;
  }
}
