package org.dromara.drive.service.parser.impl;

import org.dromara.drive.service.parser.DriveFileParser;
import org.dromara.drive.service.parser.DriveParseResult;
import org.dromara.drive.utils.DriveFileType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 化疗用药原始 JSON 解析器（移植 {@code processors/chemical_all_processor.py}）。
 * <p>
 * 整个文件原样存进 {@code file_chemical.raw_json}（一条记录），不做结构解析 ——
 * 下游按 raw_json 自行取字段，这里保证"文本与原文件逐字节一致（去掉首尾空白）"。
 *
 * @author <你的名字>
 */
@Component
public class ChemicalParser implements DriveFileParser {

    @Override
    public List<DriveFileType> fileTypes() {
        return List.of(DriveFileType.CHEMICAL_ALL);
    }

    @Override
    public DriveParseResult parse(Path file, long fileId) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8).strip();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("fileId", fileId);
        row.put("rawJson", content);
        return new DriveParseResult(List.of(row), content);
    }
}
