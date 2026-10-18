package org.dromara.audit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.dromara.audit.domain.AuditChange;
import org.dromara.audit.domain.DataAudit;
import org.dromara.audit.domain.dto.AuditCapture;
import org.dromara.audit.domain.dto.AuditIdentity;
import org.dromara.audit.domain.dto.AuditRow;
import org.dromara.audit.domain.dto.AuditSource;
import org.dromara.audit.domain.dto.AuditTarget;
import org.dromara.audit.mapper.AuditChangeMapper;
import org.dromara.audit.mapper.AuditSourceMapper;
import org.dromara.audit.mapper.DataAuditMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.TenantContext;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 事务提交前写入最终持久化值；不提供审计修改、删除或清空。 */
@Component
public class AuditWriter {
    private final AuditSourceMapper sourceMapper;
    private final DataAuditMapper eventMapper;
    private final AuditChangeMapper changeMapper;
    private final AuditDiff diff;
    private final ObjectMapper objectMapper;

    public AuditWriter(AuditSourceMapper sourceMapper, DataAuditMapper eventMapper,
                       AuditChangeMapper changeMapper, AuditDiff diff, ObjectMapper objectMapper) {
        this.sourceMapper = sourceMapper;
        this.eventMapper = eventMapper;
        this.changeMapper = changeMapper;
        this.diff = diff;
        this.objectMapper = objectMapper.copy().setSerializationInclusion(JsonInclude.Include.ALWAYS);
    }

    public void write(AuditCapture capture) {
        List<AuditRow> rows = compare(capture);
        Map<AuditIdentity, List<AuditChange>> groups = new LinkedHashMap<>();
        Map<String, Object> anchor = anchor(rows);
        for (AuditRow row : rows) {
            Map<String, Object> actual = row.after().isEmpty() ? row.before() : row.after();
            AuditIdentity identity = identity(actual, anchor, rows);
            AuditChange change = change(row);
            if (change != null) {
                groups.computeIfAbsent(identity, key -> new ArrayList<>()).add(change);
            } else if (row.source() == AuditSource.WET_QC || row.source() == AuditSource.BIO_QC) {
                // 批量导入中每个样本都保留导入事实，包括同值重导；不生成虚构字段差异。
                groups.computeIfAbsent(identity, key -> new ArrayList<>());
            }
        }
        if (groups.isEmpty() && !anchor.isEmpty()) {
            groups.put(identity(anchor, anchor, rows), new ArrayList<>());
        }
        for (Map.Entry<AuditIdentity, List<AuditChange>> group : groups.entrySet()) {
            persist(capture, group.getKey(), group.getValue());
        }
    }

    private List<AuditRow> compare(AuditCapture capture) {
        Map<String, AuditRow> result = new LinkedHashMap<>();
        for (Map.Entry<AuditTarget, List<Map<String, Object>>> entry : capture.getBefore().entrySet()) {
            AuditTarget target = entry.getKey();
            Map<String, Map<String, Object>> before = indexed(target.getSource(), entry.getValue());
            List<Map<String, Object>> actual = sourceMapper.snapshot(target,
                capture.getTenantId(), !capture.isUnrestricted());
            Map<String, Map<String, Object>> after = indexed(target.getSource(), actual);
            Set<String> keys = new LinkedHashSet<>(before.keySet());
            keys.addAll(after.keySet());
            for (String key : keys) {
                result.putIfAbsent(target.getTableName() + ":" + key, new AuditRow(target.getSource(),
                    before.getOrDefault(key, Map.of()), after.getOrDefault(key, Map.of())));
            }
        }
        return new ArrayList<>(result.values());
    }

    private Map<String, Map<String, Object>> indexed(AuditSource source, List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(key(source, row), row);
        }
        return result;
    }

    private String key(AuditSource source, Map<String, Object> row) {
        Map<String, Object> key = new LinkedHashMap<>();
        for (String column : source.getPrimaryKeys()) {
            key.put(column, row.get(column));
        }
        return json(key);
    }

    private Map<String, Object> anchor(List<AuditRow> rows) {
        for (AuditRow row : rows) {
            if (row.source() == AuditSource.REPORT && change(row) != null) {
                return row.after().isEmpty() ? row.before() : row.after();
            }
        }
        for (AuditRow row : rows) {
            if (row.source() == AuditSource.REPORT || row.source() == AuditSource.SAMPLE
                || row.source() == AuditSource.ANALYSIS || row.source() == AuditSource.USER) {
                return row.after().isEmpty() ? row.before() : row.after();
            }
        }
        for (AuditRow row : rows) {
            Map<String, Object> actual = row.after().isEmpty() ? row.before() : row.after();
            if (!actual.isEmpty()) {
                return actual;
            }
        }
        return Map.of();
    }

    private AuditIdentity identity(Map<String, Object> row, Map<String, Object> fallback, List<AuditRow> rows) {
        Map<String, Object> owner = owner(row, fallback, rows);
        String tenantId = text(row, "tenant_id");
        if (tenantId == null) {
            tenantId = text(owner, "tenant_id");
        }
        if (tenantId == null) {
            tenantId = TenantContext.PLATFORM_LOG_TENANT_ID;
        }
        String subbarcode = text(row, "subbarcode");
        if (subbarcode == null) {
            subbarcode = text(row, "_audit_subbarcode");
        }
        if (subbarcode == null) {
            subbarcode = text(owner, "subbarcode");
        }
        return new AuditIdentity(tenantId, subbarcode, number(owner, "analysis_id"), number(owner, "report_id"));
    }

    private Map<String, Object> owner(Map<String, Object> row, Map<String, Object> fallback, List<AuditRow> rows) {
        for (AuditRow candidate : rows) {
            Map<String, Object> value = candidate.after().isEmpty() ? candidate.before() : candidate.after();
            if (candidate.source() == AuditSource.USER && equalKey(row, value, "user_id")) {
                return value;
            }
            if (candidate.source() == AuditSource.REPORT && equalKey(row, value, "report_id")) {
                return value;
            }
        }
        boolean sameReportAnalysis = equalKey(row, fallback, "analysis_id");
        boolean sampleOnlyObject = row.get("analysis_id") == null && equalKey(row, fallback, "subbarcode");
        if (fallback.get("report_id") != null && (sameReportAnalysis || sampleOnlyObject)) {
            return fallback;
        }
        if (row.containsKey("subbarcode") || row.containsKey("analysis_id")) {
            return row;
        }
        return fallback;
    }

    private boolean equalKey(Map<String, Object> left, Map<String, Object> right, String key) {
        return left.get(key) != null && String.valueOf(left.get(key)).equals(String.valueOf(right.get(key)));
    }

    private AuditChange change(AuditRow row) {
        List<Map<String, Object>> fields = diff.changedFields(row.before(), row.after());
        if (!row.before().isEmpty() && !row.after().isEmpty() && fields.isEmpty()) {
            return null; // 真正无变化的行不伪造修改，主事件仍记录 NO_CHANGE。
        }
        AuditChange change = new AuditChange();
        change.setTableName(row.source().getTableName());
        Map<String, Object> actual = row.after().isEmpty() ? row.before() : row.after();
        change.setObjectKey(key(row.source(), actual));
        change.setChangeType(changeType(row));
        Map<String, Object> before = diff.snapshot(row.before());
        Map<String, Object> after = diff.snapshot(row.after());
        Set<String> names = new LinkedHashSet<>(before.keySet());
        names.addAll(after.keySet());
        for (String name : names) {
            if (!row.before().isEmpty()) {
                before.putIfAbsent(name, null);
            }
            if (!row.after().isEmpty()) {
                after.putIfAbsent(name, null);
            }
        }
        change.setBeforeJson(row.before().isEmpty() ? null : json(before));
        change.setAfterJson(row.after().isEmpty() ? null : json(after));
        change.setChangedFieldsJson(json(fields));
        return change;
    }

    private String changeType(AuditRow row) {
        if (row.before().isEmpty()) {
            return "CREATE";
        }
        if (row.after().isEmpty()) {
            return "DELETE";
        }
        String before = text(row.before(), "del_flag");
        String after = text(row.after(), "del_flag");
        if ("0".equals(before) && after != null && !"0".equals(after)) {
            return "SOFT_DELETE";
        }
        if ("0".equals(after) && before != null && !"0".equals(before)) {
            return "RESTORE";
        }
        return "UPDATE";
    }

    private void persist(AuditCapture capture, AuditIdentity identity, List<AuditChange> changes) {
        DataAudit event = new DataAudit();
        event.setTenantId(identity.tenantId());
        event.setOperationId(capture.getOperationId());
        event.setAuditScope(identity.subbarcode() == null ? "NON_SAMPLE" : "SAMPLE");
        event.setAction(capture.getAction());
        event.setSubbarcode(identity.subbarcode());
        event.setAnalysisId(identity.analysisId());
        event.setReportId(identity.reportId());
        event.setActorId(capture.getActorId());
        event.setActorName(capture.getActorName());
        event.setSource(capture.getSource());
        event.setOccurredAt(capture.getOccurredAt());
        event.setOutcome(changes.isEmpty() ? "NO_CHANGE" : "SUCCESS");
        if (capture.getOutcome() != null) {
            event.setOutcome(capture.getOutcome());
        }
        if (capture.getAction().contains("密码")) {
            event.setOutcome("SUCCESS");
        }
        event.setChangeCount(changes.size());
        TenantContext.withoutTenant(() -> eventMapper.insert(event));
        for (AuditChange change : changes) {
            change.setEventId(event.getId());
            change.setTenantId(identity.tenantId());
            TenantContext.withoutTenant(() -> changeMapper.insert(change));
        }
    }

    private String text(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Long number(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ServiceException("审计快照序列化失败，业务操作已取消");
        }
    }
}
