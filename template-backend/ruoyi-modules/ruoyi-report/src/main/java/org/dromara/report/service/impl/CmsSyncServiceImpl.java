package org.dromara.report.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.TenantContext;
import org.dromara.report.config.CmsApiProperties;
import org.dromara.report.domain.CmsSample;
import org.dromara.report.domain.vo.CmsFieldChangeVo;
import org.dromara.report.domain.vo.CmsSampleRefreshVo;
import org.dromara.report.domain.vo.CmsSampleVo;
import org.dromara.report.mapper.CmsSampleMapper;
import org.dromara.report.service.ICmsSyncService;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * CMS 样本同步实现。
 * <p>
 * 口径（2026-11-03 与用户确认）：
 * <ol>
 *   <li>只接 CMS 订单查询（LIMS 接口不处理）；</li>
 *   <li>接口返回空串/null 的字段不覆盖库值（避免上游清空导致数据丢失）；</li>
 *   <li>45 个设计字段全比，只更新有变化的字段；</li>
 *   <li>变更明细存 cms_sample.sync_log，结构是 JSON 数组
 *       {@code [{"field","label","oldValue","newValue","by","at"}, ...]}，只留最近一次；</li>
 *   <li>写库一律在 {@link TenantContext#withoutTenant} 里做——定时任务没有登录租户上下文，
 *       样本也可能属于别的药企。</li>
 * </ol>
 *
 * @author <你的名字>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CmsSyncServiceImpl implements ICmsSyncService {

    /** 单次区间最多翻多少页（防御接口 hasMore 永远为 true） */
    private static final int MAX_PAGES = 200;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CmsApiClient client;
    private final CmsApiProperties props;
    private final CmsSampleMapper sampleMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public CmsSampleRefreshVo syncBySubbarcode(String subbarcode, String operator) {
        if (subbarcode == null || subbarcode.isBlank()) {
            throw new ServiceException("样本编号不能为空");
        }
        String at = LocalDateTime.now().format(TS);
        CmsSampleRefreshVo primary = applyRemote(queryBySampleId(subbarcode), operator, at);
        primary.setSiblings(syncSiblings(primary, subbarcode, operator, at));
        return primary;
    }

    /**
     * 取一个可用于时间窗的参考日期：送检日期 → 提交时间 → 收样确认时间 → 到达时间 → 采集日期。
     * 都是字符串列，取前 10 位按 yyyy-MM-dd 解析；都解析不了返回 null（此时放弃同订单联动，只刷当前样本）。
     */
    private Optional<LocalDate> referenceDate(CmsSampleVo sample) {
        if (sample == null) {
            return Optional.empty();
        }
        String[] candidates = {
            sample.getSendDate(), sample.getOrderSubmitTime(), sample.getReceiveConfirmTime(),
            sample.getArrivalTime(), sample.getCollectionDate()
        };
        for (String raw : candidates) {
            Optional<LocalDate> parsed = parseDate(raw);
            if (parsed.isPresent()) {
                return parsed;
            }
        }
        return Optional.empty();
    }

    private Optional<LocalDate> parseDate(String raw) {
        if (raw == null || raw.length() < 10) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(raw.substring(0, 10)));
        } catch (Exception e) {
            log.debug("日期解析失败，按没有日期处理：{}（{}）", raw, e.getMessage());
            return Optional.empty();
        }
    }

    /** 按编号取 CMS 的一行（sampleID 与日期二选一，这里用编号） */
    private JsonNode queryBySampleId(String subbarcode) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("sampleID", subbarcode);
        req.put("currPage", 1);
        req.put("numPerPage", 10);
        JsonNode rows = client.invokePharmaQuery(req).path("orderSamples");
        if (!rows.isArray() || rows.isEmpty()) {
            throw new ServiceException("CMS 侧没有查到样本 " + subbarcode);
        }
        return rows.get(0);
    }

    /**
     * 双样本联动：按订单编号把同订单下的其他样本也刷一遍。
     * <p>
     * 双样本 = 同一订单两条记录、编号不同（如组织 -1A / 血液 -2A）。单样本订单这里返回空列表。
     * 单条失败只记日志，不能让整次刷新回滚。
     *
     * @param primary   本次点击的那条的结果（用来取订单编号）
     * @param subbarcode 本次点击的编号（跳过自身）
     * @param operator  操作人
     * @param at        本次操作时间
     * @return 同订单其他样本的刷新结果（含 changed / changes）
     */
    private List<CmsSampleRefreshVo> syncSiblings(CmsSampleRefreshVo primary, String subbarcode,
                                                  String operator, String at) {
        List<CmsSampleRefreshVo> siblings = new ArrayList<>();
        String orderNo = primary.getSample() == null ? null : primary.getSample().getOrderNo();
        if (orderNo == null || orderNo.isBlank()) {
            return siblings;
        }
        // 接口要求「sampleID 或 startTime/endTime 必须给一组」：按订单编号查时用送检日期 ± 窗口（跨度 <=14 天）
        Optional<LocalDate> refOpt = referenceDate(primary.getSample());
        if (refOpt.isEmpty()) {
            log.warn("同订单样本刷新跳过：样本 {} 没有可用的日期字段", subbarcode);
            return siblings;
        }
        LocalDate ref = refOpt.get();
        Set<String> seen = new HashSet<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            JsonNode responseInfo = fetchOrderPage(orderNo, ref, page);
            collectSiblings(responseInfo.path("orderSamples"),
                new SiblingContext(orderNo, subbarcode, operator, at), siblings, seen);
            if (!responseInfo.path("hasMore").asBoolean(false)) {
                break;
            }
        }
        return siblings;
    }

    /** 按订单编号 + 日期窗口取一页（接口要求 sampleID 或时间窗二选一，这里必须带窗口） */
    private JsonNode fetchOrderPage(String orderNo, LocalDate ref, int page) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("orderCode", orderNo);
        req.put("startTime", ref.minusDays(6) + " 00:00:00");
        req.put("endTime", ref.plusDays(7) + " 23:59:59");
        req.put("currPage", page);
        req.put("numPerPage", props.getPageSize());
        return client.invokePharmaQuery(req);
    }

    /** 同订单联动刷新的上下文（避免方法参数过多） */
    private record SiblingContext(String orderNo, String subbarcode, String operator, String at) {
    }

    /** 从一页结果里挑出同订单、非自身的样本逐条刷新；单条失败只记日志 */
    private void collectSiblings(JsonNode rows, SiblingContext ctx,
                                 List<CmsSampleRefreshVo> siblings, Set<String> seen) {
        if (!rows.isArray()) {
            return;
        }
        for (JsonNode row : rows) {
            String code = text(row, "BARCODE");
            if (!ctx.orderNo().equals(text(row, "ORDERCODE")) || code.isBlank()
                || code.equals(ctx.subbarcode()) || !seen.add(code)) {
                continue;
            }
            try {
                siblings.add(applyRemote(row, ctx.operator(), ctx.at()));
            } catch (Exception e) {
                log.warn("同订单样本刷新失败 orderNo={} subbarcode={} err={}", ctx.orderNo(), code, e.getMessage());
            }
        }
    }

    @Override
    public Map<String, Object> syncRange(LocalDate start, LocalDate end, String operator) {
        assertRange(start, end);
        Tally tally = new Tally();
        for (int page = 1; page <= MAX_PAGES; page++) {
            JsonNode responseInfo = fetchPage(start, end, page);
            tallyOnePage(responseInfo.path("orderSamples"), operator, tally);
            if (!responseInfo.path("hasMore").asBoolean(false)) {
                break;
            }
        }
        Map<String, Object> summary = tally.toMap(start, end);
        log.info("CMS 同步完成 {}~{} {}", start, end, summary);
        return summary;
    }

    /** 区间校验：接口单次最多 14 天，超了直接抛，不静默截断 */
    private void assertRange(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)) {
            throw new ServiceException("日期区间不合法");
        }
        if (ChronoUnit.DAYS.between(start, end) > 13) {
            throw new ServiceException("单次同步区间不能超过 14 天（接口限制），请拆成多段");
        }
    }

    /** 取某一页（每页 page-size 条，接口上限 500） */
    private JsonNode fetchPage(LocalDate start, LocalDate end, int page) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("startTime", start + " 00:00:00");
        req.put("endTime", end + " 23:59:59");
        req.put("currPage", page);
        req.put("numPerPage", props.getPageSize());
        return client.invokePharmaQuery(req);
    }

    /** 逐条 applyRemote 并计数；单条失败只记 failed，不中断整页 */
    private void tallyOnePage(JsonNode rows, String operator, Tally tally) {
        if (!rows.isArray()) {
            return;
        }
        String at = LocalDateTime.now().format(TS);
        for (JsonNode row : rows) {
            tally.scanned++;
            try {
                CmsSampleRefreshVo one = applyRemote(row, operator, at);
                tally.count(one);
            } catch (Exception e) {
                tally.failed++;
                log.warn("同步样本失败 subbarcode={} err={}", row.path("BARCODE").asText(), e.getMessage());
            }
        }
    }

    /** 区间同步计数（避免 summary 方法参数过多） */
    private static final class Tally {
        private int scanned;
        private int inserted;
        private int updated;
        private int unchanged;
        private int failed;

        private void count(CmsSampleRefreshVo one) {
            boolean isInsert = one.getChanges() != null
                && one.getChanges().stream().anyMatch(c -> CmsFieldMapping.INSERT_MARK.equals(c.getField()));
            if (isInsert) {
                inserted++;
            } else if (one.isChanged()) {
                updated++;
            } else {
                unchanged++;
            }
        }

        private Map<String, Object> toMap(LocalDate start, LocalDate end) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("start", start.toString());
            m.put("end", end.toString());
            m.put("scanned", scanned);
            m.put("inserted", inserted);
            m.put("updated", updated);
            m.put("unchanged", unchanged);
            m.put("failed", failed);
            return m;
        }
    }

    /**
     * 唯一写库口：库中无则插入，有则按字段差异更新并把变更写进 sync_log。
     *
     * @param row      CMS 返回的一条 orderSamples
     * @param operator 操作人
     * @param at       本次操作时间
     * @return 变更结果
     */
    private CmsSampleRefreshVo applyRemote(JsonNode row, String operator, String at) {
        String subbarcode = text(row, "BARCODE");
        if (subbarcode.isBlank()) {
            throw new ServiceException("CMS 返回样本缺少 BARCODE");
        }
        CmsSample exist = TenantContext.withoutTenant(() -> selectBySubbarcode(subbarcode));
        if (exist == null) {
            exist = TenantContext.withoutTenant(() -> restoreIfSoftDeleted(subbarcode)).orElse(null);
        }
        if (exist == null) {
            CmsSample entity = new CmsSample();
            List<CmsFieldChangeVo> insertChanges = new ArrayList<>();
            copyRemote(row, entity, null, insertChanges, operator, at);
            insertChanges.add(new CmsFieldChangeVo(
                CmsFieldMapping.INSERT_MARK, "新增样本", null, subbarcode, operator, at));
            entity.setSyncLog(toSyncLog(insertChanges));
            TenantContext.withoutTenant(() -> {
                sampleMapper.insert(entity);
            });
            return result(true, insertChanges, operator, at, entity.getId());
        }

        final CmsSample target = exist;
        List<CmsFieldChangeVo> changes = new ArrayList<>();
        copyRemote(row, target, target, changes, operator, at);
        if (changes.isEmpty()) {
            return result(false, changes, operator, at, target.getId());
        }
        target.setSyncLog(toSyncLog(changes));
        target.setUpdateTime(new java.util.Date());
        TenantContext.withoutTenant(() -> {
            sampleMapper.updateById(target);
        });
        return result(true, changes, operator, at, target.getId());
    }

    /**
     * 把接口值写进实体：空串/null 跳过；old != null 时按字段比对并收集变更。
     *
     * @param row     接口一行
     * @param target  目标实体
     * @param old     原实体（新增时为 null）
     * @param changes 变更收集器（新增时为空列表，只写值）
     * @param operator 操作人
     * @param at      操作时间
     */
    private void copyRemote(JsonNode row, CmsSample target, CmsSample old,
                            List<CmsFieldChangeVo> changes, String operator, String at) {
        BeanWrapper t = new BeanWrapperImpl(target);
        BeanWrapper o = old == null ? null : new BeanWrapperImpl(old);
        CmsFieldMapping.API_TO_PROP.forEach((apiKey, prop) -> {
            String remote = text(row, apiKey);
            if (remote.isBlank()) {
                return;
            }
            if (o != null) {
                String oldVal = asString(o.getPropertyValue(prop));
                if (remote.equals(norm(oldVal))) {
                    return;
                }
                changes.add(new CmsFieldChangeVo(
                    prop, CmsFieldMapping.PROP_LABEL.get(prop), oldVal, remote, operator, at));
            }
            t.setPropertyValue(prop, remote);
        });
    }

    private CmsSample selectBySubbarcode(String subbarcode) {
        return sampleMapper.selectOne(Wrappers.<CmsSample>lambdaQuery()
            .eq(CmsSample::getSubbarcode, subbarcode)
            .orderByDesc(CmsSample::getId)
            .last("limit 1"));
    }

    /** 号码被软删除行占着唯一键时先恢复，再按普通更新处理；空 = 库里从来没有这个编号 */
    private Optional<CmsSample> restoreIfSoftDeleted(String subbarcode) {
        Long id = sampleMapper.selectIdBySubbarcodeIncludeDeleted(subbarcode);
        if (id == null) {
            return Optional.empty();
        }
        sampleMapper.restoreById(id);
        return Optional.ofNullable(sampleMapper.selectOne(Wrappers.<CmsSample>lambdaQuery()
            .eq(CmsSample::getId, id)
            .last("limit 1")));
    }

    private String toSyncLog(List<CmsFieldChangeVo> changes) {
        try {
            return objectMapper.writeValueAsString(changes);
        } catch (Exception e) {
            throw new ServiceException("生成 sync_log 失败：" + e.getMessage());
        }
    }

    private CmsSampleRefreshVo result(boolean changed, List<CmsFieldChangeVo> changes,
                                      String operator, String at, Long id) {
        CmsSampleRefreshVo vo = new CmsSampleRefreshVo();
        vo.setChanged(changed);
        vo.setChanges(changes);
        vo.setBy(operator);
        vo.setAt(at);
        if (id != null) {
            vo.setSample(TenantContext.withoutTenant(() -> sampleMapper.selectVoById(id)));
        }
        return vo;
    }

    private static String text(JsonNode row, String key) {
        JsonNode v = row.get(key);
        if (v == null || v.isNull()) {
            return "";
        }
        return v.asText().trim();
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static String norm(String v) {
        return v == null ? "" : v.trim();
    }
}
