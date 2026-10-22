package org.dromara.drive.service.parser.impl;

import lombok.extern.slf4j.Slf4j;
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
 * 体细胞 SNV / Indel 解析器（移植 {@code processors/snp_processor.py}）。
 * <p>
 * 两个文件共用一张明细表 {@code file_Somatic_SNV_Indel}，第 13 列（AAChange）的冒号段数决定
 * variant / ori_variant / exon / chgvs / phgvs 的取法：
 * <ul>
 *   <li>5 段（Gene:Transcript:Exon:NtChange:AAChange）→ variant 取 AAChange 去掉 "p."；</li>
 *   <li>4 段（Gene:Transcript:Intron:NtChange）→ variant = "Intron NtChange"；</li>
 *   <li>其它段数 → 整行失败（与旧脚本一致，不静默跳过）。</li>
 * </ul>
 *
 * @author <你的名字>
 */
@Slf4j
@Component
public class SomaticSnvIndelParser implements DriveFileParser {

    /** 至少 13 列才能取到 AAChange */
    private static final int MIN_FIELDS = 13;

    /** ≥24 列时是 Indel 版式（后 4 列语义不同） */
    private static final int INDEL_LAYOUT_FIELDS = 24;

    @Override
    public List<DriveFileType> fileTypes() {
        return List.of(DriveFileType.SNP, DriveFileType.INDEL);
    }

    @Override
    public DriveParseResult parse(Path file, long fileId) throws IOException {
        List<String> lines = readNonBlankLines(file);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            rows.add(parseLine(lines.get(index), fileId, index + 1));
        }
        return new DriveParseResult(rows, String.join("\n", lines));
    }

    private Map<String, Object> parseLine(String line, long fileId, int lineNo) {
        String[] columns = DriveTexts.splitTabs(line);
        int fieldCount = columns.length;
        if (fieldCount < MIN_FIELDS) {
            throw new IllegalArgumentException("SNPInDel 字段数不足（需≥" + MIN_FIELDS + "，实际" + fieldCount
                + "），行内容：" + head(line));
        }
        Map<String, Object> row = baseRow(columns, fileId, fieldCount);
        fillVariantFields(row, columns[12], lineNo);
        return row;
    }

    /** 原始列 → 明细列（含两种版式的尾部列差异） */
    private Map<String, Object> baseRow(String[] columns, long fileId, int fieldCount) {
        boolean indelLayout = fieldCount >= INDEL_LAYOUT_FIELDS;
        String aaChange = columns[12];
        String[] segments = aaChange.split(":", -1);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("fileId", fileId);
        row.put("chr", columns[0]);
        row.put("start", columns[1]);
        row.put("end", columns[2]);
        row.put("ref", columns[3]);
        row.put("alt", columns[4]);
        row.put("homHet", columns[5]);
        row.put("mutDepth", columns[6]);
        row.put("totalDepth", columns[7]);
        row.put("mutFreq", DriveTexts.formatFrequency(columns[8]));
        row.put("funcKnownGene", columns[9]);
        row.put("geneKnownGene", columns[10].strip());
        row.put("exonicFuncKnownGene", columns[11]);
        row.put("aaChangeKnownGene", aaChange);
        row.put("esp6500siAll", fieldCount > 13 ? columns[13] : null);
        row.put("c1000g2012aprAll", fieldCount > 14 ? columns[14] : null);
        row.put("dbsnpRs", fieldCount > 15 ? columns[15] : null);
        row.put("cosmic91", fieldCount > 16 ? columns[16] : null);
        row.put("clinvar", fieldCount > 17 ? columns[17] : null);
        row.put("checkDepth", fieldCount > 18 ? columns[18] : null);
        row.put("checkResult", fieldCount > 19 ? columns[19] : null);
        row.put("clinvarResult", indelLayout ? columns[20] : null);
        row.put("spTag", indelLayout ? columns[21] : null);
        row.put("finalCheck", optionalColumn(columns, indelLayout ? 22 : 20));
        row.put("databaseInfo", optionalColumn(columns, indelLayout ? 23 : 21));
        row.put("gene", geneOf(segments, columns[10]));
        row.put("transcript", segments.length > 1 ? segments[1].strip() : null);
        return row;
    }

    /** AAChange 冒号段数决定 variant / oriVariant / exon / chgvs / phgvs */
    private void fillVariantFields(Map<String, Object> row, String aaChange, int lineNo) {
        String[] segments = aaChange.split(":", -1);
        if (segments.length == 5) {
            String aa = segments[4].strip();
            row.put("variant", aa.startsWith("p.") ? aa.substring(2) : aa);
            row.put("oriVariant", segments[1].strip() + " " + segments[2].strip() + " "
                + DriveTexts.removeInterzone(segments[3]) + " " + segments[4]);
            row.put("exon", segments[2].strip());
            row.put("chgvs", DriveTexts.removeInterzone(segments[3]));
            row.put("phgvs", segments[4].strip());
            return;
        }
        if (segments.length == 4) {
            row.put("variant", segments[2].strip() + " " + DriveTexts.removeInterzone(segments[3]));
            row.put("oriVariant", segments[1].strip() + " " + segments[2].strip() + " "
                + DriveTexts.removeInterzone(segments[3]));
            row.put("exon", segments[2].strip());
            row.put("chgvs", DriveTexts.removeInterzone(segments[3]));
            row.put("phgvs", null);
            return;
        }
        throw new IllegalArgumentException("SNPInDel 变异信息格式错误（AAChange 应为 4 或 5 段，实际 "
            + segments.length + " 段）：" + aaChange + "，文件第 " + lineNo + " 行");
    }

    private String geneOf(String[] segments, String fallback) {
        if (segments.length > 0 && !segments[0].strip().isEmpty()) {
            return segments[0].strip();
        }
        return fallback.strip();
    }

    /** 取第 index 列，超出列数返回 null（不同版式的尾部列数不一致） */
    private String optionalColumn(String[] columns, int index) {
        return index < columns.length ? columns[index] : null;
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
