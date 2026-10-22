package org.dromara.audit.service.impl;

import com.alibaba.otter.canal.protocol.CanalEntry;
import org.dromara.audit.domain.CdcChangeLine;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 行解码器单测：变化列、整行、脱敏、截断、操作者。
 *
 * @author liushangzhi
 */
@Tag("dev")
class CdcRowDecoderTest {

    @Test
    void updateShouldOnlyKeepChangedColumns() {
        CanalEntry.RowData rowData = CanalEntry.RowData.newBuilder()
            .addBeforeColumns(column("id", "7", true, false))
            .addBeforeColumns(column("qc_status", "pending", false, false))
            .addBeforeColumns(column("update_by", "3", false, false))
            .addBeforeColumns(column("update_time", "2026-10-21 16:40:12", false, false))
            .addAfterColumns(column("id", "7", true, false))
            .addAfterColumns(column("qc_status", "passed", false, true))
            .addAfterColumns(column("update_by", "3", false, false))
            .addAfterColumns(column("update_time", "2026-10-21 17:02:11", false, true))
            .build();
        CanalEntry.RowChange rowChange = CanalEntry.RowChange.newBuilder()
            .setEventType(CanalEntry.EventType.UPDATE)
            .addRowDatas(rowData)
            .build();

        List<CdcChangeLine> lines = CdcRowDecoder.decode(entry(rowChange), 4000);

        assertEquals(1, lines.size());
        CdcChangeLine line = lines.get(0);
        assertEquals(2, line.getChanges().size());
        assertTrue(line.getChanges().containsKey("qc_status"));
        assertFalse(line.getChanges().containsKey("update_by"));
        assertEquals(3L, line.getActorId());
        assertTrue(line.toLogLine().contains("\"op\":\"UPDATE\""));
    }

    @Test
    void updateWithoutAuditColumnsShouldNotBlamePreviousActor() {
        // 直接 SQL 只改业务列：update_time 没动 → 不能把行里旧的 update_by 当成本次操作者
        CanalEntry.RowData rowData = CanalEntry.RowData.newBuilder()
            .addBeforeColumns(column("id", "1", true, false))
            .addBeforeColumns(column("clinical_remark", null, false, false))
            .addBeforeColumns(column("update_by", "1", false, false))
            .addBeforeColumns(column("update_time", "2026-10-21 16:00:00", false, false))
            .addAfterColumns(column("id", "1", true, false))
            .addAfterColumns(column("clinical_remark", "cdc-e2e", false, true))
            .addAfterColumns(column("update_by", "1", false, false))
            .addAfterColumns(column("update_time", "2026-10-21 16:00:00", false, false))
            .build();
        CanalEntry.RowChange rowChange = CanalEntry.RowChange.newBuilder()
            .setEventType(CanalEntry.EventType.UPDATE)
            .addRowDatas(rowData)
            .build();

        CdcChangeLine line = CdcRowDecoder.decode(entry(rowChange), 4000).get(0);

        assertNull(line.getActorId());
        assertEquals("SERVICE", line.getActorType());
    }

    @Test
    void insertShouldKeepWholeRowAndMaskSensitiveColumns() {
        CanalEntry.RowData rowData = CanalEntry.RowData.newBuilder()
            .addAfterColumns(column("id", "1", true, false))
            .addAfterColumns(column("password", "p@ss", false, false))
            .addAfterColumns(column("file_text", "x".repeat(50), false, false))
            .build();
        CanalEntry.RowChange rowChange = CanalEntry.RowChange.newBuilder()
            .setEventType(CanalEntry.EventType.INSERT)
            .addRowDatas(rowData)
            .build();

        CdcChangeLine line = CdcRowDecoder.decode(entry(rowChange), 10).get(0);
        String json = line.toLogLine();

        assertTrue(json.contains("\"password\":\"***\""));
        assertTrue(json.contains("...[truncated]"));
        assertTrue(json.contains("\"op\":\"INSERT\""));
    }

    private static CanalEntry.Entry entry(CanalEntry.RowChange rowChange) {
        return CanalEntry.Entry.newBuilder()
            .setEntryType(CanalEntry.EntryType.ROWDATA)
            .setStoreValue(rowChange.toByteString())
            .setHeader(CanalEntry.Header.newBuilder()
                .setSchemaName("ry-vue")
                .setTableName("sample_file")
                .setLogfileName("mysql-bin.000001")
                .setLogfileOffset(1024L)
                .setExecuteTime(System.currentTimeMillis()))
            .build();
    }

    private static CanalEntry.Column column(String name, String value, boolean key, boolean updated) {
        CanalEntry.Column.Builder builder = CanalEntry.Column.newBuilder()
            .setName(name)
            .setIsKey(key)
            .setUpdated(updated);
        if (value == null) {
            // protobuf 不允许 setValue(null)：null 值要用 isNull 表达
            builder.setIsNull(true);
        } else {
            builder.setValue(value);
        }
        return builder.build();
    }
}
