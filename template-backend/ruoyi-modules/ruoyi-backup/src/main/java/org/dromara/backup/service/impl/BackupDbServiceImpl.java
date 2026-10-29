package org.dromara.backup.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.backup.domain.BackupDbBinlog;
import org.dromara.backup.domain.BackupDbFull;
import org.dromara.backup.domain.bo.BackupDbBinlogBo;
import org.dromara.backup.domain.bo.BackupDbFullBo;
import org.dromara.backup.domain.bo.BackupDbPruneBo;
import org.dromara.backup.domain.bo.BackupDbQueryBo;
import org.dromara.backup.domain.vo.BackupDbBinlogVo;
import org.dromara.backup.domain.vo.BackupDbFullVo;
import org.dromara.backup.domain.vo.BackupDbSummaryVo;
import org.dromara.backup.mapper.BackupDbBinlogMapper;
import org.dromara.backup.mapper.BackupDbFullMapper;
import org.dromara.backup.service.IBackupDbService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 数据库备份 业务层实现
 * <p>
 * 口径：库里只存「成功的备份事实」。脚本失败只写自己的日志，不上报，所以这里没有任何失败态分支。
 *
 * @author liushangzhi
 */
@RequiredArgsConstructor
@Service
public class BackupDbServiceImpl implements IBackupDbService {

    /** 默认实例标识（与 backup.yml 的 mysql.container 对齐） */
    private static final String DEFAULT_INSTANCE = "biotech-plus-mysql";

    /** 成功状态（本期固定写 2） */
    private static final int STATUS_SUCCESS = 2;

    private static final int LOCAL_KEEP = 0;

    private static final int LOCAL_PRUNED = 1;

    private final BackupDbFullMapper fullMapper;

    private final BackupDbBinlogMapper binlogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean reportFull(BackupDbFullBo bo) {
        BackupDbFull entity = MapstructUtils.convert(bo, BackupDbFull.class);
        fillDefaults(entity);
        return fullMapper.upsert(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean reportBinlog(BackupDbBinlogBo bo) {
        BackupDbBinlog entity = MapstructUtils.convert(bo, BackupDbBinlog.class);
        Date now = new Date();
        if (StringUtils.isBlank(entity.getInstance())) {
            entity.setInstance(DEFAULT_INSTANCE);
        }
        entity.setFileCount(entity.getFileCount() == null ? 0 : entity.getFileCount());
        entity.setTotalSize(entity.getTotalSize() == null ? 0L : entity.getTotalSize());
        entity.setLocalPruned(entity.getLocalPruned() == null ? LOCAL_KEEP : entity.getLocalPruned());
        entity.setBackupStatus(STATUS_SUCCESS);
        entity.setCreateBy(LoginHelper.getUserId());
        entity.setCreateTime(now);
        entity.setUpdateBy(LoginHelper.getUserId());
        entity.setUpdateTime(now);
        return binlogMapper.upsert(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean markPruned(BackupDbPruneBo bo) {
        LambdaQueryWrapper<BackupDbFull> wrapper = Wrappers.lambdaQuery(BackupDbFull.class)
            .eq(BackupDbFull::getDbName, bo.getDbName())
            .eq(BackupDbFull::getBackupDate, bo.getBackupDate());
        if (StringUtils.isNotBlank(bo.getBackupType())) {
            wrapper.eq(BackupDbFull::getBackupType, bo.getBackupType());
        }
        BackupDbFull patch = new BackupDbFull();
        patch.setLocalPruned(LOCAL_PRUNED);
        patch.setUpdateTime(new Date());
        patch.setUpdateBy(LoginHelper.getUserId());
        return fullMapper.update(patch, wrapper) > 0;
    }

    @Override
    public TableDataInfo<BackupDbFullVo> selectFullPage(BackupDbQueryBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<BackupDbFull> wrapper = Wrappers.lambdaQuery(BackupDbFull.class)
            .eq(StringUtils.isNotBlank(bo.getDbName()), BackupDbFull::getDbName, bo.getDbName())
            .eq(StringUtils.isNotBlank(bo.getInstance()), BackupDbFull::getInstance, bo.getInstance())
            .ge(StringUtils.isNotBlank(bo.getBeginDate()), BackupDbFull::getBackupDate, toDate(bo.getBeginDate()))
            .le(StringUtils.isNotBlank(bo.getEndDate()), BackupDbFull::getBackupDate, toDate(bo.getEndDate()))
            .orderByDesc(BackupDbFull::getBackupDate)
            .orderByAsc(BackupDbFull::getDbName);
        Page<BackupDbFullVo> page = fullMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public TableDataInfo<BackupDbBinlogVo> selectBinlogPage(BackupDbQueryBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<BackupDbBinlog> wrapper = Wrappers.lambdaQuery(BackupDbBinlog.class)
            .eq(StringUtils.isNotBlank(bo.getInstance()), BackupDbBinlog::getInstance, bo.getInstance())
            .ge(StringUtils.isNotBlank(bo.getBeginDate()), BackupDbBinlog::getBackupDate, toDate(bo.getBeginDate()))
            .le(StringUtils.isNotBlank(bo.getEndDate()), BackupDbBinlog::getBackupDate, toDate(bo.getEndDate()))
            .orderByDesc(BackupDbBinlog::getBackupDate);
        Page<BackupDbBinlogVo> page = binlogMapper.selectVoPage(pageQuery.build(), wrapper);
        markChainState(page.getRecords(), bo.getInstance());
        return TableDataInfo.build(page);
    }

    /**
     * 现算位点链：本条 start 是否等于上一条（更早一天）的 end。
     * 页内相邻直接比；页首那条需要多查一行「比本页最旧日期更早的最近一条」。
     */
    private void markChainState(List<BackupDbBinlogVo> rows, String instance) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<BackupDbBinlogVo> asc = new ArrayList<>(rows);
        Collections.reverse(asc);
        BackupDbBinlogVo before = newestBefore(asc.get(0).getBackupDate(), instance);
        BackupDbBinlogVo prev = before;
        for (BackupDbBinlogVo row : asc) {
            row.setChainState(chainOf(prev, row));
            prev = row;
        }
    }

    private String chainOf(BackupDbBinlogVo prev, BackupDbBinlogVo row) {
        if (prev == null) {
            return "origin";
        }
        boolean samePos = prev.getEndPos() != null && prev.getEndPos().equals(row.getStartPos());
        boolean sameFile = StringUtils.equals(prev.getEndFile(), row.getStartFile());
        return sameFile && samePos ? "linked" : "broken";
    }

    private BackupDbBinlogVo newestBefore(Date date, String instance) {
        LambdaQueryWrapper<BackupDbBinlog> wrapper = Wrappers.lambdaQuery(BackupDbBinlog.class)
            .eq(StringUtils.isNotBlank(instance), BackupDbBinlog::getInstance, instance)
            .lt(BackupDbBinlog::getBackupDate, date)
            .orderByDesc(BackupDbBinlog::getBackupDate);
        Page<BackupDbBinlogVo> page = binlogMapper.selectVoPage(new Page<>(1, 1), wrapper);
        return page.getRecords().isEmpty() ? null : page.getRecords().get(0);
    }

    @Override
    public BackupDbSummaryVo summary() {
        BackupDbSummaryVo vo = new BackupDbSummaryVo();
        List<BackupDbFullVo> allFulls = fullMapper.selectVoList();
        fillFullSummary(vo, allFulls);
        fillBinlogSummary(vo);
        fillOssSummary(vo, allFulls);
        return vo;
    }

    private void fillFullSummary(BackupDbSummaryVo vo, List<BackupDbFullVo> allFulls) {
        long local = allFulls.stream().filter(x -> LOCAL_KEEP == intOf(x.getLocalPruned())).count();
        long pruned = allFulls.stream().filter(x -> LOCAL_PRUNED == intOf(x.getLocalPruned())).count();
        vo.setLocalFullCopies((int) local);
        vo.setPrunedFullCopies((int) pruned);
        if (allFulls.isEmpty()) {
            vo.setLatestFullOk(0);
            vo.setLatestFullTotal(0);
            vo.setLatestFullSize(0L);
            vo.setDbCount(0);
            vo.setDbTableTotal(0);
            vo.setExcludedTableCount(0);
            return;
        }
        Date latest = allFulls.stream().map(BackupDbFullVo::getBackupDate)
            .filter(java.util.Objects::nonNull).max(Date::compareTo).orElse(null);
        List<BackupDbFullVo> latestRows = allFulls.stream()
            .filter(x -> latest != null && latest.equals(x.getBackupDate())).toList();
        vo.setLatestFullDate(formatDate(latest));
        vo.setLatestFullOk(latestRows.size());
        vo.setLatestFullTotal(latestRows.size());
        vo.setLatestFullSize(latestRows.stream().mapToLong(x -> longOf(x.getLocalSize())).sum());
        vo.setDbCount((int) allFulls.stream().map(BackupDbFullVo::getDbName).distinct().count());
        fillScopeSummary(vo, latestRows);
    }

    /** 备份范围：库表总数 + 排除数（用最近一次全量的行对账） */
    private void fillScopeSummary(BackupDbSummaryVo vo, List<BackupDbFullVo> latestRows) {
        int total = 0;
        int excluded = 0;
        for (BackupDbFullVo row : latestRows) {
            total += intOf(row.getDbTableTotal());
            excluded += intOf(row.getDbTableTotal()) - intOf(row.getTableCount());
        }
        vo.setDbTableTotal(total);
        vo.setExcludedTableCount(excluded);
    }

    private void fillBinlogSummary(BackupDbSummaryVo vo) {
        LambdaQueryWrapper<BackupDbBinlog> wrapper = Wrappers.lambdaQuery(BackupDbBinlog.class)
            .orderByDesc(BackupDbBinlog::getBackupDate);
        Page<BackupDbBinlogVo> page = binlogMapper.selectVoPage(new Page<>(1, 1), wrapper);
        if (!page.getRecords().isEmpty()) {
            BackupDbBinlogVo last = page.getRecords().get(0);
            vo.setLastBinlogDate(formatDate(last.getBackupDate()));
            vo.setLastBinlogSize(longOf(last.getTotalSize()));
            vo.setLastBinlogFileCount(intOf(last.getFileCount()));
            vo.setLastBinlogAt(formatTime(last.getFinishedAt()));
        }
        Date from = Date.from(LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant());
        vo.setLocalBinlogDays(Math.toIntExact(binlogMapper.selectCount(
            Wrappers.lambdaQuery(BackupDbBinlog.class).ge(BackupDbBinlog::getBackupDate, from))));
    }

    private void fillOssSummary(BackupDbSummaryVo vo, List<BackupDbFullVo> allFulls) {
        vo.setOssLatestAt(allFulls.stream()
            .filter(x -> StringUtils.isNotBlank(x.getOssPath()))
            .map(BackupDbFullVo::getFinishedAt)
            .filter(java.util.Objects::nonNull)
            .max(Date::compareTo)
            .map(BackupDbServiceImpl::formatTime)
            .orElse(null));
    }

    private void fillDefaults(BackupDbFull entity) {
        Date now = new Date();
        if (StringUtils.isBlank(entity.getInstance())) {
            entity.setInstance(DEFAULT_INSTANCE);
        }
        if (StringUtils.isBlank(entity.getBackupType())) {
            entity.setBackupType("weekly");
        }
        entity.setExcludedTables(StringUtils.isBlank(entity.getExcludedTables()) ? "" : entity.getExcludedTables());
        entity.setLocalPruned(entity.getLocalPruned() == null ? LOCAL_KEEP : entity.getLocalPruned());
        entity.setBackupStatus(STATUS_SUCCESS);
        entity.setCreateBy(LoginHelper.getUserId());
        entity.setCreateTime(now);
        entity.setUpdateBy(LoginHelper.getUserId());
        entity.setUpdateTime(now);
    }

    private static int intOf(Integer value) {
        return value == null ? 0 : value;
    }

    private static long longOf(Long value) {
        return value == null ? 0L : value;
    }

    private static Date toDate(String yyyyMmDd) {
        if (StringUtils.isBlank(yyyyMmDd)) {
            return null;
        }
        return Date.from(LocalDate.parse(yyyyMmDd).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static String formatDate(Date date) {
        return date == null ? null : new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private static String formatTime(Date date) {
        return date == null ? null : new SimpleDateFormat("HH:mm:ss").format(date);
    }
}
