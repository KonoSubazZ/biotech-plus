package org.dromara.drive.service.parser.impl;

import org.dromara.drive.service.parser.DriveFileParser;
import org.dromara.drive.service.parser.DriveParseResult;
import org.dromara.drive.utils.DriveFileType;
import org.dromara.drive.utils.DriveTexts;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MSI 解析器（移植 {@code processors/msi_processor.py}）。
 * <p>
 * 每行至少 4 列（score / threshold / status / checkResult），任一为空即整行失败 ——
 * 与旧脚本完全一致（实测生产里有 3 列的坏文件，驱动会如实报失败而不是静默跳过）。
 *
 * @author <你的名字>
 */
@Component
public class MsiParser implements DriveFileParser {

    /** 每行最少列数 */
    private static final int REQUIRED_FIELD_COUNT = 4;

    @Override
    public List<DriveFileType> fileTypes() {
        return List.of(DriveFileType.MSI);
    }

    @Override
    public DriveParseResult parse(Path file, long fileId) throws IOException {
        List<String> lines = readNonBlankLines(file);
        if (lines.size() <= 1) {
            throw new IllegalArgumentException("MSI 文件无数据行，实际行数=" + lines.size());
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        boolean headerSkipped = false;
        for (String line : lines) {
            if (!headerSkipped) {
                headerSkipped = true;
                continue;
            }
            String[] columns = DriveTexts.splitTabs(line);
            if (columns.length < REQUIRED_FIELD_COUNT) {
                throw new IllegalArgumentException("MSI 数据行字段数不足（需≥" + REQUIRED_FIELD_COUNT
                    + "，实际" + columns.length + "），行内容：" + head(line));
            }
            for (int index = 0; index < REQUIRED_FIELD_COUNT; index++) {
                if (columns[index].strip().isEmpty()) {
                    throw new IllegalArgumentException("MSI 第 " + (index + 1)
                        + " 个字段为空（score/threshold/status/checkResult 不能为空），行内容：" + head(line));
                }
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("fileId", fileId);
            row.put("score", columns[0].strip());
            row.put("threshold", columns[1].strip());
            row.put("status", columns[2].strip());
            row.put("checkResult", columns[3].strip());
            rows.add(row);
        }
        return new DriveParseResult(rows, String.join("\n", lines));
    }

    private List<String> readNonBlankLines(Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (!line.strip().isEmpty()) {
                lines.add(line.strip());
            }
        }
        return lines;
    }

    private String head(String line) {
        return line.length() <= 100 ? line : line.substring(0, 100) + "...";
    }
}
