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
 * CR_ALL（胚系全量）解析器（移植 {@code processors/cr_all_processor.py}）。
 * <p>
 * 前 15 列必填；chgvs 走 {@code remove_interzone}（保留 del/dup 关键词及其前的内容）。
 * variant 优先取 pHGVS 去掉 "p."，为空则退回 cHGVS；ori_variant = 转录本 外显子 cHGVS pHGVS 拼接。
 *
 * @author <你的名字>
 */
@Component
public class CrAllParser implements DriveFileParser {

    /** 前 15 列（0~14）必填 */
    private static final int MIN_FIELDS = 15;

    @Override
    public List<DriveFileType> fileTypes() {
        return List.of(DriveFileType.CR_ALL);
    }

    @Override
    public DriveParseResult parse(Path file, long fileId) throws IOException {
        List<String> lines = readNonBlankLines(file);
        List<Map<String, Object>> rows = new ArrayList<>();
        // 仅表头/空文件算成功（0 行）
        for (int index = 1; index < lines.size(); index++) {
            rows.add(parseLine(lines.get(index), fileId));
        }
        return new DriveParseResult(rows, String.join("\n", lines));
    }

    private Map<String, Object> parseLine(String line, long fileId) {
        String[] columns = DriveTexts.splitTabs(line);
        int fieldCount = columns.length;
        if (fieldCount < MIN_FIELDS) {
            throw new IllegalArgumentException("CR_ALL 字段数不足（需≥" + MIN_FIELDS + "，实际" + fieldCount
                + "），行内容：" + head(line));
        }

        String transcriptId = columns[3].strip();
        String exon = columns[4].strip();
        String chgvs = DriveTexts.removeInterzone(columns[5]);
        String phgvs = columns[6].strip();

        String variant;
        if (!phgvs.isEmpty()) {
            variant = phgvs.startsWith("p.") ? phgvs.substring(2) : phgvs;
        } else {
            variant = chgvs;
        }
        String oriVariant = joinNonEmpty(transcriptId, exon, chgvs, phgvs);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("fileId", fileId);
        row.put("gene", columns[0].strip());
        row.put("chr", columns[1].strip());
        row.put("pos", columns[2].strip());
        row.put("transcriptId", transcriptId);
        row.put("exon", exon);
        row.put("chgvs", chgvs);
        row.put("phgvs", phgvs);
        row.put("zygosity", columns[7].strip());
        row.put("exonicFunc", columns[8].strip());
        row.put("c1000g2015augAll", columns[9].strip());
        row.put("exacEas", columns[10].strip());
        row.put("clinvarId", columns[11].strip());
        row.put("siftPred", columns[12].strip());
        row.put("polyphen2HdivPred", columns[13].strip());
        row.put("mutationTasterPred", columns[14].strip());
        putOptionalFields(row, columns, fieldCount);
        row.put("variant", variant);
        row.put("oriVariant", oriVariant);

        requireNotEmpty(row, line, "gene", "chr", "pos", "transcriptId", "chgvs", "phgvs");
        return row;
    }

    /** 第 15 列之后都是可选注释列，按位补齐（文件短了就给 null） */
    private void putOptionalFields(Map<String, Object> row, String[] columns, int fieldCount) {
        String[] optionalColumns = {"depth", "revel", "gnomadGenomeAll", "interproDomain", "clnsig",
            "omimPhenotypes", "omimId", "hgmdTag", "hgmdDisease", "hgmdPmid", "classificationLovd",
            "clinicalSignificanceClinvar", "source", "clinicalSignificanceEnigma"};
        int firstOptionalIndex = 15;
        for (int offset = 0; offset < optionalColumns.length; offset++) {
            int columnIndex = firstOptionalIndex + offset;
            String value = columnIndex < fieldCount ? columns[columnIndex].strip() : null;
            row.put(optionalColumns[offset], value);
        }
    }

    private void requireNotEmpty(Map<String, Object> row, String line, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            if (value == null || String.valueOf(value).isEmpty()) {
                throw new IllegalArgumentException("CR_ALL 核心字段 '" + key + "' 为空，行内容：" + head(line));
            }
        }
    }

    private String joinNonEmpty(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(value);
            }
        }
        return builder.toString();
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
