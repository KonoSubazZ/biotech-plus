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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    /** 新增样本时 sync_log 里那条变更的 field 取值 */
    private static final String INSERT_MARK = "__insert__";

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 接口键 → 实体属性（顺序 = cms_sample 列顺序，45 个） */
    private static final Map<String, String> API_TO_PROP;

    /** 实体属性 → 中文名（弹窗与 sync_log 共用） */
    private static final Map<String, String> PROP_LABEL;

    static {
        Map<String, String> api = new LinkedHashMap<>();
        api.put("abatchno", "batchNo");
        api.put("corpdesc", "customerName");
        api.put("contractname", "contractName");
        api.put("contractsno", "contractNo");
        api.put("schemeno", "schemeNo");
        api.put("yqprojectno", "projectNo");
        api.put("contractemail", "contractContactEmail");
        api.put("operatemanagerdesc", "operatorName");
        api.put("researchcenter", "researchCenter");
        api.put("REPORTRECEIVER", "reportReceiver");
        api.put("reportemail", "reportEmail");
        api.put("sendpeople", "senderName");
        api.put("sendertel", "senderPhone");
        api.put("SENDDATE", "sendDate");
        api.put("expressname", "expressCompany");
        api.put("expressno", "expressNo");
        api.put("transportcondition", "transportCondition");
        api.put("hascontroller", "hasTemperatureLogger");
        api.put("receivetime", "receiveConfirmTime");
        api.put("submittime", "orderSubmitTime");
        api.put("ORDERCODE", "orderNo");
        api.put("BARCODE", "subbarcode");
        api.put("customersamplingcode", "customerSamplingCode");
        api.put("samplename", "sampleName");
        api.put("subjectid", "subjectNo");
        api.put("SEX", "sex");
        api.put("birthyear", "birthYear");
        api.put("diseasetype", "diseaseType");
        api.put("visitcycle", "visitCycle");
        api.put("PCODE", "productCode");
        api.put("ERPTESTNAME", "productName");
        api.put("SAMPLETYPE", "sampleType");
        api.put("SPECIMENNUM", "sampleQuantity");
        api.put("UNIT", "quantityUnit");
        api.put("SAMPLEATTRIBUTE", "sampleAttribute");
        api.put("collectionsite", "collectionSite");
        api.put("tissuetype", "tissueType");
        api.put("tumortype", "tumorType");
        api.put("primarytumorsite", "primaryTumorSite");
        api.put("SAMPLETIME", "collectionDate");
        api.put("assaymethod", "assayMethod");
        api.put("SAMPLEREMARK", "remark");
        api.put("ABNORMALREMARK", "abnormalRemark");
        api.put("mtacontroltype", "controlType");
        api.put("arrivaltime", "arrivalTime");
        API_TO_PROP = Collections.unmodifiableMap(api);

        Map<String, String> label = new LinkedHashMap<>();
        label.put("batchNo", "订单批次编号");
        label.put("customerName", "客户名称");
        label.put("contractName", "合同名称");
        label.put("contractNo", "合同编号");
        label.put("schemeNo", "研究方案编号");
        label.put("projectNo", "项目编号");
        label.put("contractContactEmail", "联系人邮箱");
        label.put("operatorName", "运营经理");
        label.put("researchCenter", "研究中心名称");
        label.put("reportReceiver", "报告接收人");
        label.put("reportEmail", "报告接收邮箱");
        label.put("senderName", "送检人");
        label.put("senderPhone", "联系电话");
        label.put("sendDate", "送检日期");
        label.put("expressCompany", "快递公司");
        label.put("expressNo", "快递单号");
        label.put("transportCondition", "快递条件");
        label.put("hasTemperatureLogger", "是否带温控计");
        label.put("receiveConfirmTime", "收样确认时间");
        label.put("orderSubmitTime", "提交时间");
        label.put("orderNo", "订单编号");
        label.put("subbarcode", "诺禾编号");
        label.put("customerSamplingCode", "客户采样识别码");
        label.put("sampleName", "样本名称");
        label.put("subjectNo", "试验参与者编号");
        label.put("sex", "性别");
        label.put("birthYear", "出生年份");
        label.put("diseaseType", "临床诊断");
        label.put("visitCycle", "访视周期");
        label.put("productCode", "产品编码");
        label.put("productName", "产品名称");
        label.put("sampleType", "样本类型");
        label.put("sampleQuantity", "样本数量");
        label.put("quantityUnit", "数量单位");
        label.put("sampleAttribute", "样本属性");
        label.put("collectionSite", "取材部位");
        label.put("tissueType", "组织类型");
        label.put("tumorType", "肿瘤类型");
        label.put("primaryTumorSite", "原发部位");
        label.put("collectionDate", "采集日期");
        label.put("assayMethod", "检测方式");
        label.put("remark", "备注");
        label.put("abnormalRemark", "异常备注");
        label.put("controlType", "管控类型");
        label.put("arrivalTime", "到达时间");
        if (api.size() != 45 || label.size() != 45) {
            throw new IllegalStateException(
                "cms_sample 字段映射必须是 45 个，当前 api=" + api.size() + " label=" + label.size());
        }
        PROP_LABEL = Collections.unmodifiableMap(label);
    }

    private final CmsApiClient client;
    private final CmsApiProperties props;
    private final CmsSampleMapper sampleMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public CmsSampleRefreshVo syncBySubbarcode(String subbarcode, String operator) {
        if (subbarcode == null || subbarcode.isBlank()) {
            throw new ServiceException("样本编号不能为空");
        }
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("sampleID", subbarcode);
        req.put("currPage", 1);
        req.put("numPerPage", 10);
        JsonNode responseInfo = client.invokePharmaQuery(req);
        JsonNode rows = responseInfo.path("orderSamples");
        if (!rows.isArray() || rows.isEmpty()) {
            throw new ServiceException("CMS 侧没有查到样本 " + subbarcode);
        }
        return applyRemote(rows.get(0), operator, LocalDateTime.now().format(TS));
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
        private static final String INSERT_MARK_LOCAL = INSERT_MARK;
        private int scanned;
        private int inserted;
        private int updated;
        private int unchanged;
        private int failed;

        private void count(CmsSampleRefreshVo one) {
            boolean isInsert = one.getChanges() != null
                && one.getChanges().stream().anyMatch(c -> INSERT_MARK_LOCAL.equals(c.getField()));
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
            insertChanges.add(new CmsFieldChangeVo(INSERT_MARK, "新增样本", null, subbarcode, operator, at));
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
        API_TO_PROP.forEach((apiKey, prop) -> {
            String remote = text(row, apiKey);
            if (remote.isBlank()) {
                return;
            }
            if (o != null) {
                String oldVal = asString(o.getPropertyValue(prop));
                if (remote.equals(norm(oldVal))) {
                    return;
                }
                changes.add(new CmsFieldChangeVo(prop, PROP_LABEL.get(prop), oldVal, remote, operator, at));
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
