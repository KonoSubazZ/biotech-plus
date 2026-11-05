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
 * 化疗位点解析器（移植 {@code processors/chem_processor.py}）。
 * <p>
 * 注意：{@code .Chem.xls} 实际是制表符文本（不是 Excel 二进制），所以按行解析，不需要 POI。
 * 每行至少 3 列（chr / position / allele1），三列都不允许为空。
 *
 * @author <你的名字>
 */
@Component
public class ChemParser implements DriveFileParser {

    /** 每行最少列数 */
    private static final int REQUIRED_FIELD_COUNT = 3;

    @Override
    public List<DriveFileType> fileTypes() {
        return List.of(DriveFileType.CHEM);
    }

    @Override
    public DriveParseResult parse(Path file, long fileId) throws IOException {
        List<String> lines = readNonBlankLines(file);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            rows.add(parseLine(lines.get(index), fileId));
        }
        return new DriveParseResult(rows, String.join("\n", lines));
    }

    private Map<String, Object> parseLine(String line, long fileId) {
        String[] columns = DriveTexts.splitTabs(line);
        if (columns.length < REQUIRED_FIELD_COUNT) {
            throw new IllegalArgumentException("Chem 字段数不足（需≥" + REQUIRED_FIELD_COUNT
                + "，实际" + columns.length + "），行内容：" + head(line));
        }
        String chr = columns[0].strip();
        String position = columns[1].strip();
        String allele1 = columns[2].strip();
        if (chr.isEmpty() || position.isEmpty() || allele1.isEmpty()) {
            throw new IllegalArgumentException("Chem 核心字段为空（chr/position/allele1），行内容：" + head(line));
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("fileId", fileId);
        row.put("chr", chr);
        row.put("position", position);
        row.put("allele1", allele1);
        return row;
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
