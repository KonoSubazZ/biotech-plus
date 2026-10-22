package org.dromara.audit.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一条待输出的变更日志（一行 = 一次行变更）。
 * <p>
 * 只负责拼成单行 JSON，不落库、不依赖 Spring。
 *
 * @author liushangzhi
 */
@Data
public class CdcChangeLine {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** binlog 提交时间 */
    private Date commitTime;

    /** 库名 */
    private String db;

    /** 表名 */
    private String table;

    /** INSERT / UPDATE / DELETE */
    private String op;

    /** 主键列与值 */
    private Map<String, String> pk;

    /** UPDATE：变化列 → [前值, 后值] */
    private Map<String, String[]> changes;

    /** INSERT / DELETE：整行 */
    private Map<String, String> data;

    /** 操作者用户ID */
    private Long actorId;

    /** 操作者姓名（由调用方查 sys_user 补） */
    private String actorName;

    /** USER / SERVICE */
    private String actorType;

    /** binlog 文件 */
    private String binlogFile;

    /** binlog 偏移 */
    private Long binlogOffset;

    /** 同一 binlog 事件内的行号 */
    private Integer rowIndex;

    /**
     * 输出单行 JSON；字段顺序固定，便于 grep 和肉眼比对
     *
     * @return 一行 JSON
     */
    public String toLogLine() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("time", formatTime(commitTime));
        out.put("db", db);
        out.put("table", table);
        out.put("op", op);
        out.put("pk", pk);
        if (changes != null && !changes.isEmpty()) {
            out.put("changes", changes);
        }
        if (data != null && !data.isEmpty()) {
            out.put("data", data);
        }
        out.put("actorId", actorId);
        out.put("actorName", actorName);
        out.put("actorType", actorType);
        out.put("binlog", binlogFile + ":" + binlogOffset + "#" + rowIndex);
        try {
            return MAPPER.writeValueAsString(out);
        } catch (JsonProcessingException e) {
            // 日志序列化失败不能让消费线程死掉：退化成可读文本
            return "{\"time\":\"" + formatTime(commitTime) + "\",\"table\":\"" + table
                + "\",\"op\":\"" + op + "\",\"error\":\"json-serialize-failed\"}";
        }
    }

    private String formatTime(Date date) {
        if (date == null) {
            // 日志字段约定：没有提交时间就写 null，不编造当前时间
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(date);
    }
}
