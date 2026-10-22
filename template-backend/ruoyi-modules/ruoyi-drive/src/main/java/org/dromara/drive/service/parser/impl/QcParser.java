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
 * 质控解析器：样本 qc.txt 与对照 G.qc.txt 共用一个实现，落不同表。
 * <p>
 * 文件固定 10 行（每行 "指标名 TAB 值"），整文件一条记录；行数不足/超出直接失败（旧脚本同样口径）。
 * 核心字段（肿瘤细胞含量、DNA 总量、降解程度、平均测序深度、目标区域覆盖度、Q30）不允许空值。
 *
 * @author <你的名字>
 */
@Component
public class QcParser implements DriveFileParser {

    /** 文件固定行数 */
    private static final int REQUIRED_LINE_COUNT = 10;

    /** 行号 → 列名（顺序即 qc 文件顺序） */
    private static final String[] COLUMNS = {
        "tumorCellContent", "dnaTotalNg", "dnaDegradation", "preLibraryTotalNg", "sequencingDataVolume",
        "meanSequencingDepth", "coverageUniformity", "targetRegionCoverage", "genomeAlignmentRate",
        "baseQualityQ30Rate"
    };

    /** 不允许为空的核心字段 */
    private static final List<String> REQUIRED_COLUMNS = List.of(
        "tumorCellContent", "dnaTotalNg", "dnaDegradation",
        "meanSequencingDepth", "targetRegionCoverage", "baseQualityQ30Rate");

    @Override
    public List<DriveFileType> fileTypes() {
        return List.of(DriveFileType.QC, DriveFileType.QC_CONTROL);
    }

    @Override
    public DriveParseResult parse(Path file, long fileId) throws IOException {
        List<String> lines = readNonBlankLines(file);
        if (lines.size() != REQUIRED_LINE_COUNT) {
            throw new IllegalArgumentException("QC 文件行数不符合要求（需" + REQUIRED_LINE_COUNT
                + " 行，实际" + lines.size() + " 行）");
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("fileId", fileId);
        for (int index = 0; index < REQUIRED_LINE_COUNT; index++) {
            row.put(COLUMNS[index], valueOf(lines.get(index)));
        }
        for (String column : REQUIRED_COLUMNS) {
            Object value = row.get(column);
            if (value == null || String.valueOf(value).isEmpty()) {
                throw new IllegalArgumentException("QC 核心字段 '" + column + "' 为空");
            }
        }
        return new DriveParseResult(List.of(row), String.join("\n", lines));
    }

    private String valueOf(String line) {
        String[] parts = DriveTexts.splitTabs(line);
        if (parts.length < 2) {
            throw new IllegalArgumentException("QC 行格式错误，缺少制表符分割的第二个元素：" + head(line));
        }
        return parts[1].strip();
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
