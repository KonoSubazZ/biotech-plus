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
 * Fusion 解析器（移植 {@code processors/fusion_processor.py}）。
 * <p>
 * 断点（bp1/bp2）从两侧 softclip 描述里推：{@code intronic_e1_e2} 取第一个外显子号，
 * {@code exonN} 取 N，前缀用基因名首字母，例如 ALK exon20 → A20。
 * variant = {@code gene2-gene1 Fusion bp2:bp1}（基因名做了 HER2→ERBB2、MEK1→MAP2K1 标准化）。
 *
 * @author <你的名字>
 */
@Component
public class FusionParser implements DriveFileParser {

    /** 至少 11 列（0~10）是必填区 */
    private static final int MIN_FIELDS = 11;

    @Override
    public List<DriveFileType> fileTypes() {
        return List.of(DriveFileType.FUSION);
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
        int fieldCount = columns.length;
        if (fieldCount < MIN_FIELDS) {
            throw new IllegalArgumentException("Fusion 字段数不足（需≥" + MIN_FIELDS + "，实际" + fieldCount
                + "），行内容：" + head(line));
        }
        Map<String, Object> row = baseRow(columns, fileId, fieldCount);
        fillFusionFields(row, columns[2], columns[5]);
        return row;
    }

    /** 原始列 → 明细列（第 11 列之后为可选列） */
    private Map<String, Object> baseRow(String[] columns, long fileId, int fieldCount) {
        String sclip1Info = columns[2];
        String sclip2Info = columns[5];
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("fileId", fileId);
        row.put("chromosome1", columns[0]);
        row.put("softclip1", columns[1]);
        row.put("sclip1Info", sclip1Info);
        row.put("chromosome2", columns[3]);
        row.put("softclip2", columns[4]);
        row.put("sclip2Info", sclip2Info);
        row.put("driverGene", columns[6]);
        row.put("cosmicInfo", columns[7]);
        row.put("dbInfo", columns[8]);
        row.put("stream", columns[9]);
        row.put("supReadsHq", columns[10]);
        row.put("supReadsUniq", fieldCount > 11 ? columns[11] : null);
        row.put("depth", fieldCount > 12 ? columns[12] : null);
        row.put("freq", fieldCount > 13 ? DriveTexts.formatFrequency(columns[13]) : null);
        row.put("checkResult", fieldCount > 14 ? columns[14] : null);
        row.put("tag", fieldCount > 15 ? columns[15] : null);
        row.put("fusionReads", fieldCount > 16 ? columns[16] : null);
        row.put("sarcomaSubtypes", fieldCount > 17 ? columns[17] : null);
        row.put("evidenceLevel", fieldCount > 18 ? columns[18] : null);
        row.put("gene", standardizeGene(columns[6]));
        // 转录本列：sclip2 的 transcript / sclip1 的 transcript（列数不足 15 时该列无意义）
        if (fieldCount > 14) {
            row.put("transcript", firstSegment(sclip2Info) + "/" + firstSegment(sclip1Info));
        } else {
            row.put("transcript", null);
        }
        return row;
    }

    /** 由两侧 softclip 描述推 gene1/bp1、gene2/bp2，并拼出 variant */
    private void fillFusionFields(Map<String, Object> row, String sclip1Info, String sclip2Info) {
        String gene1 = standardizeGene(secondSegment(sclip1Info));
        String gene2 = standardizeGene(secondSegment(sclip2Info));
        String bp1 = breakpointOf(sclip1Info, gene1);
        String bp2 = breakpointOf(sclip2Info, gene2);
        row.put("gene1", gene1);
        row.put("bp1", bp1);
        row.put("gene2", gene2);
        row.put("bp2", bp2);

        String variant = null;
        if (gene1 != null && gene2 != null && bp1 != null && bp2 != null) {
            variant = gene2 + "-" + gene1 + " Fusion " + bp2 + ":" + bp1;
        }
        row.put("variant", variant);
        row.put("oriVariant", variant);
    }

    private String standardizeGene(String geneName) {
        if (geneName == null) {
            return null;
        }
        String gene = geneName.strip();
        if (gene.isEmpty()) {
            return gene;
        }
        if (gene.contains("HER2") || gene.contains("ERBB2")) {
            return "ERBB2";
        }
        if (gene.contains("MEK1") || gene.contains("MAP2K1")) {
            return "MAP2K1";
        }
        return gene;
    }

    /**
     * 断点：优先 intronic_eN_eM（取 N），其次 exonN（取 N），前缀是基因名首字母。
     */
    private String breakpointOf(String info, String gene) {
        if (info == null || info.isEmpty() || gene == null || gene.isEmpty()) {
            return null;
        }
        char prefix = gene.charAt(0);
        if (info.contains("intronic_e")) {
            String[] parts = info.split("_e", -1);
            if (parts.length >= 3) {
                String first = parts[1].strip();
                if ("intergenic".equals(first)) {
                    return prefix + "(" + first + ")";
                }
                return prefix + first;
            }
            if (parts.length >= 2) {
                return prefix + parts[1].strip().replace(";", "");
            }
        }
        if (info.contains("exon")) {
            String[] parts = info.split("exon", -1);
            if (parts.length >= 2) {
                return prefix + parts[1].strip().replace(";", "");
            }
        }
        return null;
    }

    private String firstSegment(String info) {
        if (info == null) {
            return "";
        }
        int index = info.indexOf(':');
        return index < 0 ? info.strip() : info.substring(0, index).strip();
    }

    private String secondSegment(String info) {
        if (info == null) {
            return null;
        }
        String[] parts = info.split(":", -1);
        return parts.length > 1 ? parts[1].strip() : null;
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
