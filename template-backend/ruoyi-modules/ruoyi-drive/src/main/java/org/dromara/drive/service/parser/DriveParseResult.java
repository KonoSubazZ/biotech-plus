package org.dromara.drive.service.parser;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 单个文件的解析结果。
 * <p>
 * {@code rows} 的 key 是目标明细表的列名（驼峰），由 Mapper XML 逐列取值 ——
 * 解析器不认识 SQL，落库只走 XML，符合本仓"SQL 不写在 Java 里"的约定。
 *
 * @author <你的名字>
 */
@Getter
@RequiredArgsConstructor
public class DriveParseResult {

    /** 写入明细表的数据行（QC/chemical 这类整文件一条的，列表长度为 1） */
    private final List<Map<String, Object>> rows;

    /** 写入 analysis_file_status.file_text 的文本内容 */
    private final String fileText;

    /** 明细行数（= analysis_file_status.mut_num） */
    public int rowCount() {
        return rows == null ? 0 : rows.size();
    }
}
