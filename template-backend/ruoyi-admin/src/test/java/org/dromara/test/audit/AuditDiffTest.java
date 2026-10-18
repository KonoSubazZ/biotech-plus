package org.dromara.test.audit;

import org.dromara.audit.service.impl.AuditDiff;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("local")
class AuditDiffTest {
    private final AuditDiff diff = new AuditDiff();

    @Test
    void keepsExplicitNullAndDistinguishesEmptyValues() {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("name", "before");
        before.put("count", 0);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("name", null);
        after.put("count", "");
        assertEquals(2, diff.changedFields(before, after).size());
        assertTrue(diff.snapshot(after).containsKey("name"));
    }

    @Test
    void excludesCredentialsAndTechnicalTimestamps() {
        Map<String, Object> row = Map.of("user_id", 7L, "password", "<PASSWORD>",
            "access_token", "<TOKEN>", "update_time", "old", "nested", Map.of("secret", "<SECRET>"));
        Map<String, Object> snapshot = diff.snapshot(row);
        assertFalse(snapshot.containsKey("password"));
        assertFalse(snapshot.containsKey("access_token"));
        assertFalse(snapshot.containsKey("update_time"));
        assertEquals(Map.of(), snapshot.get("nested"));
    }

    @Test
    void unchangedBusinessValuesHaveNoFieldChanges() {
        assertEquals(0, diff.changedFields(Map.of("name", "same", "update_time", "one"),
            Map.of("name", "same", "update_time", "two")).size());
    }
}
