package org.dromara.audit.service.impl;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 保留清空语义；凭证与不影响业务含义的技术更新时间不进入证据。 */
@Component
public class AuditDiff {
    private static final Set<String> TECHNICAL = Set.of("create_time", "update_time", "create_by", "update_by");

    public Map<String, Object> snapshot(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> field : row.entrySet()) {
            String name = field.getKey().toLowerCase(Locale.ROOT);
            if (!excluded(name)) {
                result.put(name, cleanValue(field.getValue()));
            }
        }
        return result;
    }

    private boolean excluded(String name) {
        String compact = name.replace("_", "").replace("-", "");
        return name.startsWith("_audit_") || TECHNICAL.contains(name) || compact.contains("password") || compact.contains("token")
            || compact.contains("secret") || compact.contains("authorization") || compact.contains("cookie")
            || compact.contains("privatekey") || compact.contains("apikey") || compact.equals("salt");
    }

    private Object cleanValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> fields = new LinkedHashMap<>();
            map.forEach((key, item) -> fields.put(String.valueOf(key), item));
            return snapshot(fields);
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(this::cleanValue).toList();
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros();
        }
        return value;
    }

    public List<Map<String, Object>> changedFields(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> oldValues = snapshot(before);
        Map<String, Object> newValues = snapshot(after);
        Set<String> names = new LinkedHashSet<>(oldValues.keySet());
        names.addAll(newValues.keySet());
        List<Map<String, Object>> result = new ArrayList<>();
        for (String name : names) {
            if (!Objects.equals(oldValues.get(name), newValues.get(name))) {
                Map<String, Object> change = new LinkedHashMap<>();
                change.put("field", name);
                change.put("before", oldValues.get(name));
                change.put("after", newValues.get(name));
                result.add(change);
            }
        }
        return result;
    }
}
