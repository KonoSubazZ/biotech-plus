package org.dromara.audit.service.impl;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.google.protobuf.InvalidProtocolBufferException;
import org.dromara.audit.domain.CdcChangeLine;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * binlog 行事件 → CdcChangeLine。
 * <p>
 * 纯函数：不碰数据库、不读写文件，方便单测。
 *
 * @author liushangzhi
 */
public final class CdcRowDecoder {

    /** 凭证类列一律打码，绝不落原值 */
    private static final Set<String> SENSITIVE_COLUMNS = Set.of(
        "password", "salt", "token", "secret", "access_key", "private_key", "client_secret");

    private static final String MASK = "***";

    /** 主键 id 的最大字符数，超过按非数字处理，避免 parseLong 溢出 */
    private static final int MAX_ID_LENGTH = 18;

    private CdcRowDecoder() {
    }

    /**
     * 解析一个 canal entry；非 ROWDATA / DDL 返回空列表
     *
     * @param entry          canal 条目
     * @param maxValueLength 单列值截断长度
     * @return 变更行
     */
    public static List<CdcChangeLine> decode(CanalEntry.Entry entry, int maxValueLength) {
        List<CdcChangeLine> lines = new ArrayList<>();
        if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
            return lines;
        }
        CanalEntry.RowChange change = parseRowChange(entry);
        if (change.getIsDdl()) {
            // 表结构变化用迁移脚本追，日志只记数据行
            return lines;
        }
        CanalEntry.Header header = entry.getHeader();
        String op = change.getEventType().name();
        int rowIndex = 0;
        for (CanalEntry.RowData rowData : change.getRowDatasList()) {
            lines.add(buildLine(header, op, rowIndex, rowData, maxValueLength));
            rowIndex++;
        }
        return lines;
    }

    private static CanalEntry.RowChange parseRowChange(CanalEntry.Entry entry) {
        try {
            return CanalEntry.RowChange.parseFrom(entry.getStoreValue());
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException("解析 canal RowChange 失败", e);
        }
    }

    private static CdcChangeLine buildLine(CanalEntry.Header header, String op, int rowIndex,
                                           CanalEntry.RowData rowData, int maxValueLength) {
        CdcChangeLine line = new CdcChangeLine();
        line.setCommitTime(new Date(header.getExecuteTime()));
        line.setDb(header.getSchemaName());
        line.setTable(header.getTableName());
        line.setOp(op);
        line.setBinlogFile(header.getLogfileName());
        line.setBinlogOffset(header.getLogfileOffset());
        line.setRowIndex(rowIndex);

        Map<String, String> before = toColumnMap(rowData.getBeforeColumnsList(), maxValueLength);
        Map<String, String> after = toColumnMap(rowData.getAfterColumnsList(), maxValueLength);
        line.setPk(buildPk(rowData, maxValueLength));

        if ("UPDATE".equals(op)) {
            line.setChanges(buildChanges(rowData, before, after));
        } else {
            // INSERT 取 after 整行，DELETE 取 before 整行
            line.setData(after.isEmpty() ? before : after);
        }

        Long actorId = resolveActorId(op, line.getChanges(), after.isEmpty() ? before : after);
        line.setActorId(actorId);
        line.setActorType(actorId == null ? "SERVICE" : "USER");
        return line;
    }

    /**
     * 归因口径：INSERT / DELETE 用行内 create_by / update_by；
     * UPDATE 只有本次真的写了 update_time 才认 update_by ——
     * 否则说明是绕过应用的直写，行里的 update_by 只是上一次的旧值，不能冒充本次操作者。
     */
    private static Long resolveActorId(String op, Map<String, String[]> changes, Map<String, String> actorRow) {
        if ("UPDATE".equals(op)) {
            if (changes == null || !changes.containsKey("update_time")) {
                return null;
            }
            return firstPositive(actorRow.get("update_by"), actorRow.get("create_by"));
        }
        if ("INSERT".equals(op)) {
            return firstPositive(actorRow.get("create_by"), actorRow.get("update_by"));
        }
        return firstPositive(actorRow.get("update_by"), actorRow.get("create_by"));
    }

    private static Map<String, String> toColumnMap(List<CanalEntry.Column> columns, int maxValueLength) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (CanalEntry.Column column : columns) {
            String name = column.getName().toLowerCase(Locale.ROOT);
            if (column.getIsNull()) {
                map.put(name, null);
                continue;
            }
            if (SENSITIVE_COLUMNS.contains(name)) {
                map.put(name, MASK);
                continue;
            }
            map.put(name, truncate(column.getValue(), maxValueLength));
        }
        return map;
    }

    private static Map<String, String> buildPk(CanalEntry.RowData rowData, int maxValueLength) {
        List<CanalEntry.Column> source = rowData.getAfterColumnsList().isEmpty()
            ? rowData.getBeforeColumnsList() : rowData.getAfterColumnsList();
        LinkedHashMap<String, String> pk = new LinkedHashMap<>();
        for (CanalEntry.Column column : source) {
            if (column.getIsKey()) {
                pk.put(column.getName().toLowerCase(Locale.ROOT), truncate(column.getValue(), maxValueLength));
            }
        }
        if (pk.isEmpty()) {
            // 无主键关系表（如 sys_user_role）退化成列数，保证 pk 字段不为空
            pk.put("_row", String.valueOf(source.size()));
        }
        return pk;
    }

    /**
     * UPDATE 只输出变化列：优先用 canal 的 updated 标记，标记不可靠时自己比对前后值
     */
    private static Map<String, String[]> buildChanges(CanalEntry.RowData rowData,
                                                      Map<String, String> before,
                                                      Map<String, String> after) {
        LinkedHashMap<String, String[]> changes = new LinkedHashMap<>();
        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
            String name = column.getName().toLowerCase(Locale.ROOT);
            String beforeValue = before.get(name);
            String afterValue = after.get(name);
            if (column.getUpdated() || !Objects.equals(beforeValue, afterValue)) {
                changes.put(name, new String[]{beforeValue, afterValue});
            }
        }
        return changes;
    }

    private static String truncate(String value, int maxValueLength) {
        if (value == null || maxValueLength <= 0 || value.length() <= maxValueLength) {
            return value;
        }
        return value.substring(0, maxValueLength) + "...[truncated]";
    }

    /**
     * 取第一个正整数 ID；没有时返回 null —— 日志字段约定：不知道就写 null，不编造 0。
     */
    private static Long firstPositive(String... values) {
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty() || trimmed.length() > MAX_ID_LENGTH
                || !trimmed.chars().allMatch(Character::isDigit)) {
                continue;
            }
            long parsed = Long.parseLong(trimmed);
            if (parsed > 0) {
                return parsed;
            }
        }
        return null;
    }
}
