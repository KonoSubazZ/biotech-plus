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
 * CNV 解析器（移植 {@code processors/cnv_processor.py}）。
 * <p>
 * 第 3 列可能自带 "start end" 区间（含空格），这时 copy_number 落在第 4 列，否则 start/end/cn 占 3/4/5 列。
 * 拷贝数 ≥ 2.6 → Amplification，≤ 1.5 → Loss，其余 variant 留空（与旧脚本一致）。
 *
 * @author <你的名字>
 */
@Component
public class CnvParser implements DriveFileParser {

    /** 拷贝数达到该值算扩增 */
    private static final double AMPLIFICATION_THRESHOLD = 2.6;

    /** 拷贝数低于该值算缺失 */
    private static final double LOSS_THRESHOLD = 1.5;

    @Override
    public List<DriveFileType> fileTypes() {
        return List.of(DriveFileType.CNV);
    }

    @Override
    public DriveParseResult parse(Path file, long fileId) throws IOException {
        List<String> lines = readNonBlankLines(file);
        List<Map<String, Object>> rows = new ArrayList<>();
        // 仅表头/空文件算成功（旧脚本同样口径：0 行，不报错）
        for (int index = 1; index < lines.size(); index++) {
            rows.add(parseLine(lines.get(index), fileId));
        }
        return new DriveParseResult(rows, String.join("\n", lines));
    }

    private Map<String, Object> parseLine(String line, long fileId) {
        String[] columns = DriveTexts.splitTabs(line);
        if (columns.length < 4) {
            throw new IllegalArgumentException("CNV 字段数不足（需≥4，实际" + columns.length + "），行内容：" + head(line));
        }
        String gene = standardizeGene(columns[0].strip());
        if (gene.isEmpty()) {
            throw new IllegalArgumentException("CNV 基因名称为空，行内容：" + head(line));
        }

        String[] range = resolveRange(columns, line);
        String start = range[0];
        String end = range[1];
        String copyNum = range[2];
        double copy;
        try {
            copy = Double.parseDouble(copyNum.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("CNV 拷贝数解析失败: " + copyNum + "，行内容：" + head(line), e);
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("fileId", fileId);
        row.put("gene", gene);
        row.put("chr", columns[1]);
        row.put("start", start);
        row.put("end", end);
        row.put("copyNum", copyNum);
        row.put("variant", variantOf(copy));
        row.put("oriVariant", "CN=" + copyNum);
        return row;
    }

    /**
     * 解析 start / end / copy_number：第 3 列可能自带 "start end" 区间，这时 copy_number 落在第 4 列。
     *
     * @return [start, end, copyNum]
     */
    private String[] resolveRange(String[] columns, String line) {
        String start;
        String end;
        String copyNum;
        if (columns[2].strip().contains(" ")) {
            String[] parts = columns[2].strip().split(" ");
            if (parts.length < 2) {
                throw new IllegalArgumentException("CNV start/end 解析失败，行内容：" + head(line));
            }
            start = parts[0].strip();
            end = parts[parts.length - 1].strip();
            copyNum = columns[3];
        } else {
            if (columns.length < 5) {
                throw new IllegalArgumentException("CNV 字段数不足（需≥5，实际" + columns.length
                    + "），行内容：" + head(line));
            }
            start = columns[2];
            end = columns[3];
            copyNum = columns[4];
        }
        if (start.isEmpty() || end.isEmpty() || copyNum.isEmpty()) {
            throw new IllegalArgumentException("CNV 缺少必要字段（start/end/copy_number），行内容：" + head(line));
        }
        return new String[]{start, end, copyNum};
    }

    private String variantOf(double copy) {
        if (copy >= AMPLIFICATION_THRESHOLD) {
            return "Amplification";
        }
        if (copy <= LOSS_THRESHOLD) {
            return "Loss";
        }
        return null;
    }

    private String standardizeGene(String gene) {
        if (gene.contains("HER2") || gene.contains("ERBB2")) {
            return "ERBB2";
        }
        if (gene.contains("MEK1") || gene.contains("MAP2K1")) {
            return "MAP2K1";
        }
        return gene;
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
