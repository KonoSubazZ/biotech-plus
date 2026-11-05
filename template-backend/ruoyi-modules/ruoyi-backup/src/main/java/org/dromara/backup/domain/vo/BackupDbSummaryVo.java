package org.dromara.backup.domain.vo;

import lombok.Data;

/**
 * 数据库备份汇总（页面四张卡片 + 表格头）
 * <p>
 * 全部现算，不落库：库里只有事实行。
 *
 * @author liushangzhi
 */
@Data
public class BackupDbSummaryVo {

    /** 最近一次全量的日期（yyyy-MM-dd） */
    private String latestFullDate;

    /** 该日成功的库数 */
    private Integer latestFullOk;

    /** 该日应有记录的库数 */
    private Integer latestFullTotal;

    /** 该日压缩包总字节 */
    private Long latestFullSize;

    /** 最近一次增量日期 */
    private String lastBinlogDate;

    /** 最近一次增量执行时刻（HH:mm:ss） */
    private String lastBinlogAt;

    /** 最近一次增量大小 */
    private Long lastBinlogSize;

    /** 最近一次增量文件数 */
    private Integer lastBinlogFileCount;

    /** 涉及的库数 */
    private Integer dbCount;

    /** 库表总数 */
    private Integer dbTableTotal;

    /** 排除表总数 */
    private Integer excludedTableCount;

    /** 本地还在的全量份数 */
    private Integer localFullCopies;

    /** 已按策略清理本地份数 */
    private Integer prunedFullCopies;

    /** 本地还在的增量天数 */
    private Integer localBinlogDays;

    /** 最近一次 OSS 上传时间（空 = 从未上传 / 未启用） */
    private String ossLatestAt;
}
