import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Java 17 独立设计示例：只验证配置映射，不连接 Canal、数据库或项目服务。 */
public class CanalAuditMappingDemo {
    private static final Set<String> TECHNICAL_FIELDS = Set.of(
        "create_time", "update_time", "create_by", "update_by"
    );
    private static final Map<String, String> ACTION_NAMES = Map.of(
        "TENANT_DISABLE", "药企停用",
        "TENANT_CORRECTION", "药企信息更正",
        "REPORT_DATA_UPDATE", "报告数据修改"
    );
    private static final Map<String, ResourceRule> RESOURCES = Map.of(
        "sys_tenant", new ResourceRule("TENANT", "药企", "id", "TENANT_UPDATE", "药企信息修改"),
        "analysis_report", new ResourceRule("REPORT", "报告", "report_id", "REPORT_DATA_UPDATE", "报告数据修改")
    );
    // 演示配置表加载后的内存对象；报告状态文案与当前 ReportStatus 已核对。
    private static final Map<String, FieldRule> FIELDS = Map.of(
        "sys_tenant.tenant_name", new FieldRule("药企名称", "INCLUDE", Map.of()),
        "sys_tenant.status", new FieldRule("启用状态", "INCLUDE", Map.of("0", "正常", "1", "停用")),
        "sys_tenant.remark", new FieldRule("备注", "INCLUDE", Map.of()),
        "analysis_report.report_status", new FieldRule("报告状态", "INCLUDE",
            Map.of("32", "报告生成成功", "33", "报告生成失败")),
        "analysis_report.access_token", new FieldRule("凭证", "SECRET", Map.of())
    );
    private static final List<ActionRule> ACTION_RULES = List.of(
        new ActionRule("TENANT", "status", "1", "TENANT_DISABLE", "药企停用")
    );

    record ResourceRule(String code, String label, String primaryKey, String updateCode, String updateLabel) { }
    record FieldRule(String label, String captureMode, Map<String, String> labels) { }
    record ActionRule(String resource, String field, Object target, String code, String label) { }
    record OperationContext(String actionCode) { }
    record RowChange(String table, Map<String, Object> before, Map<String, Object> after,
                     OperationContext context) { }
    record FieldChange(String field, String label, Object before, Object after,
                       String beforeDisplay, String afterDisplay) { }
    record Action(String code, String label, String source) { }
    record AuditView(String objectName, Map<String, Object> objectKey, Action action, List<FieldChange> fields) { }

    /** 输入是已解码、已关联上下文的 UPDATE；生产版还需实现 CREATE/DELETE/事务组装等。 */
    static AuditView convert(RowChange change) {
        ResourceRule resource = RESOURCES.get(change.table());
        if (resource == null) {
            throw new IllegalArgumentException("未配置审计对象：" + change.table());
        }
        Set<String> names = new LinkedHashSet<>(change.before().keySet());
        names.addAll(change.after().keySet());
        List<FieldChange> fields = new ArrayList<>();
        for (String name : names) {
            FieldRule rule = FIELDS.get(change.table() + "." + name);
            if (excluded(name, rule)) {
                continue;
            }
            Object before = change.before().get(name);
            Object after = change.after().get(name);
            if (equivalent(before, after)) {
                continue;
            }
            String label = name;
            if (rule != null) {
                label = rule.label();
            }
            fields.add(new FieldChange(name, label, before, after, display(before, rule), display(after, rule)));
        }
        Map<String, Object> key = new LinkedHashMap<>();
        key.put(resource.primaryKey(), change.after().get(resource.primaryKey()));
        return new AuditView(resource.label(), key, resolveAction(change, resource, fields), List.copyOf(fields));
    }

    private static boolean excluded(String name, FieldRule rule) {
        if (TECHNICAL_FIELDS.contains(name)) {
            return true;
        }
        if (rule == null) {
            return false; // 未配置的普通字段保留原名；生产版应同时发出配置缺失提示。
        }
        return "IGNORE".equals(rule.captureMode()) || "SECRET".equals(rule.captureMode());
    }

    private static boolean equivalent(Object before, Object after) {
        if (before instanceof BigDecimal left && after instanceof BigDecimal right) {
            return left.compareTo(right) == 0;
        }
        return Objects.equals(before, after);
    }

    private static String display(Object value, FieldRule rule) {
        if (value == null) {
            return "未填写";
        }
        if ("".equals(value)) {
            return "空字符串";
        }
        String raw = String.valueOf(value);
        if (rule == null || rule.labels().isEmpty()) {
            return raw;
        }
        String label = rule.labels().get(raw);
        if (label == null) {
            return "未知编码（" + raw + "）";
        }
        return label + "（" + raw + "）";
    }

    private static Action resolveAction(RowChange change, ResourceRule resource, List<FieldChange> fields) {
        OperationContext context = change.context();
        String actionCode = null;
        if (context != null) {
            actionCode = context.actionCode();
        }
        if (actionCode != null && ACTION_NAMES.containsKey(actionCode)) {
            return new Action(actionCode, ACTION_NAMES.get(actionCode), "CONTEXT");
        }
        for (ActionRule rule : ACTION_RULES) {
            if (matches(rule, resource, fields)) {
                return new Action(rule.code(), rule.label(), "RULE");
            }
        }
        return new Action(resource.updateCode(), resource.updateLabel(), "DEFAULT");
    }

    private static boolean matches(ActionRule rule, ResourceRule resource, List<FieldChange> fields) {
        if (!rule.resource().equals(resource.code())) {
            return false;
        }
        for (FieldChange field : fields) {
            if (field.field().equals(rule.field()) && equivalent(field.after(), rule.target())) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Object> row(Object... pairs) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            values.put((String) pairs[index], pairs[index + 1]);
        }
        return values;
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            System.out.println("FAIL " + name);
        }
        assert condition : name;
        System.out.println("PASS " + name);
    }

    private static AuditView testTenantRules() {
        AuditView view = convert(new RowChange("sys_tenant",
            row("id", 7L, "status", "0", "update_time", "BEFORE"),
            row("id", 7L, "status", "1", "update_time", "AFTER"), null));
        List<FieldChange> changed = view.fields();
        check(changed.size() == 1 && "status".equals(changed.get(0).field()), "technical_fields_excluded");
        Action action = view.action();
        check("TENANT_DISABLE".equals(action.code()) && "RULE".equals(action.source()),
            "derived_action_name");
        check("停用（1）".equals(view.fields().get(0).afterDisplay()), "dictionary_display");
        check(Long.valueOf(7L).equals(view.objectKey().get("id")), "real_primary_key_preserved");
        AuditView overridden = convert(new RowChange("sys_tenant",
            row("id", 7L, "status", "0"), row("id", 7L, "status", "1"),
            new OperationContext("TENANT_CORRECTION")));
        check("CONTEXT".equals(overridden.action().source())
            && "TENANT_CORRECTION".equals(overridden.action().code()), "context_action_has_priority");
        AuditView unchangedStatus = convert(new RowChange("sys_tenant",
            row("id", 7L, "status", "1", "remark", "A"),
            row("id", 7L, "status", "1", "remark", "B"), null));
        check("DEFAULT".equals(unchangedStatus.action().source()), "unchanged_status_is_not_disable_action");
        return view;
    }

    private static void testDisplayAndFiltering() {
        AuditView report = convert(new RowChange("analysis_report",
            row("report_id", 9L, "report_status", 32, "access_token", "<BEFORE>"),
            row("report_id", 9L, "report_status", 33, "access_token", "<AFTER>"), null));
        check(report.fields().size() == 1, "secret_field_excluded");
        check("报告生成失败（33）".equals(report.fields().get(0).afterDisplay()), "enum_display");
        AuditView cleared = convert(new RowChange("sys_tenant",
            row("id", 7L, "remark", "待更正"), row("id", 7L, "remark", null), null));
        check(cleared.fields().get(0).after() == null
            && "未填写".equals(cleared.fields().get(0).afterDisplay()), "null_clear_preserved");
        AuditView emptyText = convert(new RowChange("sys_tenant",
            row("id", 7L, "remark", null), row("id", 7L, "remark", ""), null));
        check("空字符串".equals(emptyText.fields().get(0).afterDisplay()), "empty_string_differs_from_null");
        AuditView unknown = convert(new RowChange("analysis_report",
            row("report_id", 9L, "report_status", 32), row("report_id", 9L, "report_status", 999), null));
        check("未知编码（999）".equals(unknown.fields().get(0).afterDisplay()), "unknown_code_fallback");
    }

    private static void testOtherBoundaries() {
        AuditView technical = convert(new RowChange("sys_tenant",
            row("id", 7L, "update_time", "BEFORE"), row("id", 7L, "update_time", "AFTER"), null));
        check(technical.fields().isEmpty(), "technical_only_has_no_business_change");
        AuditView unknownField = convert(new RowChange("sys_tenant",
            row("id", 7L, "new_metric", "A"), row("id", 7L, "new_metric", "B"), null));
        check("new_metric".equals(unknownField.fields().get(0).label()), "unknown_field_keeps_raw_name");
        AuditView decimal = convert(new RowChange("sys_tenant",
            row("id", 7L, "metric", new BigDecimal("1.0")),
            row("id", 7L, "metric", new BigDecimal("1.00")), null));
        check(decimal.fields().isEmpty(), "decimal_scale_is_not_business_change");
        boolean rejected = false;
        try {
            convert(new RowChange("unconfigured_table", Map.of(), Map.of(), null));
        } catch (IllegalArgumentException exception) {
            rejected = exception.getMessage().contains("unconfigured_table");
        }
        check(rejected, "unconfigured_resource_rejected");
    }

    private static void testEmptyActionCode() {
        OperationContext context = new OperationContext(null);
        AuditView derived = convert(new RowChange("sys_tenant",
            row("id", 7L, "status", "0"), row("id", 7L, "status", "1"), context));
        check("TENANT_DISABLE".equals(derived.action().code()), "null_action_code_uses_field_rule");
        AuditView fallback = convert(new RowChange("sys_tenant",
            row("id", 7L, "tenant_name", "A"), row("id", 7L, "tenant_name", "B"), context));
        check("DEFAULT".equals(fallback.action().source()), "null_action_code_uses_default");
    }

    public static void main(String[] args) {
        if (!CanalAuditMappingDemo.class.desiredAssertionStatus()) {
            throw new IllegalStateException("请使用 java -ea 启用断言");
        }
        AuditView example = testTenantRules();
        testDisplayAndFiltering();
        testOtherBoundaries();
        testEmptyActionCode();
        FieldChange first = example.fields().get(0);
        String display = "EXAMPLE " + example.action().label() + " | " + first.label()
            + " | " + first.beforeDisplay() + " -> " + first.afterDisplay();
        System.out.println(display);
        System.out.println("CANAL_AUDIT_MAPPING_DEMO: PASS (17 assertions)");
    }
}
