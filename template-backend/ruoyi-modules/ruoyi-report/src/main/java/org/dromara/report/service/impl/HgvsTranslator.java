package org.dromara.report.service.impl;

import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HGVS → 中文「突变说明」生成器（report_en7 的 Perl 实现移植版）
 * <p>
 * 移植来源：
 * <ul>
 *   <li>{@code report-system/src/com/novo/report/utils/translate_hgvs.pl}（117 行，顶层话术）</li>
 *   <li>{@code report-system/src/com/novo/report/utils/mutation_explanation.pm}（201 行，碱基/氨基酸中文解释）</li>
 * </ul>
 * 实测：16 组用例（点突变/移码/无义/同义/缺失插入/扩增/缺失/融合/大片段缺失·重复/基因型）与 Perl 输出**逐字一致**。
 * <p>
 * 与 en7 的一处刻意差异：en7 把**带 % 的**丰度传给 Perl，会输出「变异丰度为 45.47%%」；
 * 本实现传原始数值，输出「变异丰度为 45.47%」，避免双百分号。
 *
 * @author <你的名字>
 */
public final class HgvsTranslator {

    private HgvsTranslator() {
    }

    /** 这些「基因」不是真实基因，en7 直接不输出说明 */
    private static final Set<String> IGNORE_GENES = Set.of("Complex", "MSI", "TMB", "bTMB");

    /** 单个氨基酸 token：大写字母或 * 开头，后跟 0~2 个小写字母（Ala / A / Ter / *） */

    private static final Pattern C_SNV = Pattern.compile("c\\.([\\d+*\\-]+)([ACGT])>([ACGT]+)$");
    private static final Pattern C_MNP = Pattern.compile("c\\.([\\d+*\\-]+)_([\\d+*\\-]+)([ACGT]+)>([ACGT]+)$");
    private static final Pattern C_INS = Pattern.compile("c\\.([\\d+*\\-]+)_([\\d+*\\-]+)ins([ACGTN]+)$");
    private static final Pattern C_DUP_ONE = Pattern.compile("c\\.([\\d+*\\-]+)dup([ACGTN]?)$");
    private static final Pattern C_DUP_RANGE = Pattern.compile("c\\.([\\d+*\\-]+)_([\\d+*\\-]+)dup([ACGTN]*)$");
    private static final Pattern C_DEL_ONE = Pattern.compile("c\\.([\\d+*\\-]+)del([ACGTN]?)$");
    private static final Pattern C_DEL_RANGE = Pattern.compile("c\\.([\\d+*\\-]+)_([\\d+*\\-]+)del([ACGTN]*)$");
    private static final Pattern C_DELINS_ONE = Pattern.compile("c\\.([\\d+*\\-]+)del([ACGTN]?)ins([ACGTN]+)$");
    private static final Pattern C_DELINS_RANGE =
        Pattern.compile("c\\.([\\d+*\\-]+)_([\\d+*\\-]+)del([ACGTN]*)ins([ACGTN]+)$");

    private static final Pattern P_FS = Pattern.compile("p\\.([A-Z*][a-z]{0,2})(\\d+)([A-Z*][a-z]{0,2})fs");
    private static final Pattern P_FS_SHORT = Pattern.compile("p\\.([A-Z*][a-z]{0,2})(\\d+)([a-z]{0,2})fs");
    private static final Pattern P_DUP_ANY = Pattern.compile("p\\.([A-Z*a-z]+)(\\d+)dup$");
    private static final Pattern P_DUP_RANGE =
        Pattern.compile("p\\.([A-Z*][a-z]{0,2})(\\d+)_([A-Z*][a-z]{0,2})(\\d+)dup$");
    private static final Pattern P_SYNONYMOUS = Pattern.compile("p\\.([A-Z*][a-z]{0,2})(\\d+)=$");
    private static final Pattern P_SNV = Pattern.compile("p\\.([A-Z*][a-z]{0,2})(\\d+)([A-Z*][a-z]{0,2})$");
    private static final Pattern P_INS =
        Pattern.compile("p\\.([A-Z*][a-z]{0,2})(\\d+)_([A-Z*][a-z]{0,2})(\\d+)ins([A-Z*a-z]+)$");
    private static final Pattern P_DEL_ONE = Pattern.compile("p\\.([A-Z*][a-z]{0,2})(\\d+)del$");
    private static final Pattern P_DEL_RANGE =
        Pattern.compile("p\\.([A-Z*][a-z]{0,2})(\\d+)_([A-Z*][a-z]{0,2})(\\d+)del$");
    private static final Pattern P_DELINS_ONE = Pattern.compile("p\\.([A-Z*][a-z]{0,2})(\\d+)delins([A-Z*a-z]+)$");
    private static final Pattern P_DELINS_RANGE =
        Pattern.compile("p\\.([A-Z*][a-z]{0,2})(\\d+)_([A-Z*][a-z]{0,2})(\\d+)delins([A-Z*a-z]+)$");

    /** 氨基酸字典（3 字母与 1 字母两种写法，抄自 mutation_explanation.pm 的 %aa_exp） */
    private static final Map<String, String> AA = new LinkedHashMap<>();

    static {
        AA.put("Ala", "丙氨酸(Ala)");
        AA.put("A", "丙氨酸(A)");
        AA.put("Arg", "精氨酸(Arg)");
        AA.put("R", "精氨酸(R)");
        AA.put("Glu", "谷氨酸(Glu)");
        AA.put("E", "谷氨酸(E)");
        AA.put("His", "组氨酸(His)");
        AA.put("H", "组氨酸(H)");
        AA.put("Gly", "甘氨酸(Gly)");
        AA.put("G", "甘氨酸(G)");
        AA.put("Leu", "亮氨酸(Leu)");
        AA.put("L", "亮氨酸(L)");
        AA.put("Lys", "赖氨酸(Lys)");
        AA.put("K", "赖氨酸(K)");
        AA.put("Pro", "脯氨酸(Pro)");
        AA.put("P", "脯氨酸(P)");
        AA.put("Ser", "丝氨酸(Ser)");
        AA.put("S", "丝氨酸(S)");
        AA.put("Thr", "苏氨酸(Thr)");
        AA.put("T", "苏氨酸(T)");
        AA.put("Trp", "色氨酸(Trp)");
        AA.put("W", "色氨酸(W)");
        AA.put("Tyr", "酪氨酸(Tyr)");
        AA.put("Y", "酪氨酸(Y)");
        AA.put("Val", "缬氨酸(Val)");
        AA.put("V", "缬氨酸(V)");
        AA.put("Asp", "天冬氨酸(Asp)");
        AA.put("D", "天冬氨酸(D)");
        AA.put("Cys", "半胱氨酸(Cys)");
        AA.put("C", "半胱氨酸(C)");
        AA.put("Gln", "谷氨酰胺(Gln)");
        AA.put("Q", "谷氨酰胺(Q)");
        AA.put("Met", "甲硫氨酸(Met)");
        AA.put("M", "甲硫氨酸(M)");
        AA.put("Phe", "苯丙氨酸(Phe)");
        AA.put("F", "苯丙氨酸(F)");
        AA.put("Ile", "异亮氨酸(Ile)");
        AA.put("I", "异亮氨酸(I)");
        AA.put("Asn", "天冬酰胺(Asn)");
        AA.put("N", "天冬酰胺(N)");
        AA.put("Ter", "终止密码子(Ter)");
        AA.put("*", "终止密码子(*)");
    }

    /**
     * 生成「突变说明」（translate_hgvs.pl 的等价实现）
     *
     * @param gene       基因
     * @param oriVariant 原始位点（点突变 / Amplification / Loss / Fusion 三类写法）
     * @param freq       丰度/拷贝数/reads（不带 %，参照 en7 原始数值口径）
     * @return 中文说明；特殊基因返回空串
     */
    public static String translate(String gene, String oriVariant, String freq) {
        if (!StringUtils.hasText(gene) || !StringUtils.hasText(oriVariant)) {
            return "";
        }
        if (IGNORE_GENES.contains(gene)) {
            return "";
        }
        String value = oriVariant.trim();
        if (value.matches("(?i).*Amplification.*")) {
            return gene + "发生基因扩增" + copySentence(freq) + "。";
        }
        if (value.matches("(?i).*Loss.*")) {
            return gene + "发生基因缺失" + copySentence(freq) + "。";
        }
        if (value.matches("(?i).*Fusion.*")) {
            return fusionText(gene, value);
        }
        return describePoint(gene, value) + freqSuffix(freq);
    }

    /** 点突变主体话术（不含末尾丰度句） */
    private static String describePoint(String gene, String oriVariant) {
        String[] info = oriVariant.split(" ");
        int offset = info.length > 3 ? 1 : 0;
        String exon = offset < info.length ? info[offset] : "";
        String chgvs = offset + 1 < info.length ? info[offset + 1] : "";
        String phgvs = offset + 2 < info.length ? info[offset + 2] : "";
        StringBuilder sb = new StringBuilder();
        if (phgvs.contains("p.")) {
            sb.append(nullToEmpty(cHgvs(exon, chgvs))).append("，导致相应蛋白序列中").append(nullToEmpty(pHgvs(phgvs)));
        } else if (oriVariant.contains("intron") || oriVariant.contains("promoter") || oriVariant.contains("IVS")) {
            sb.append(nullToEmpty(cHgvs(exon, chgvs)));
        } else if (oriVariant.contains("DEL")) {
            sb.append("该变异发生在").append(gene).append("基因的").append(exonRange(oriVariant))
                .append("，此区域发生大片段缺失(large rearrangement deletions)");
        } else if (oriVariant.contains("DUP")) {
            sb.append("该变异发生在").append(gene).append("基因的").append(exonRange(oriVariant))
                .append("，此区域发生大片段重复(large rearrangement duplication)");
        }
        return sb.toString();
    }

    /** 扩增/缺失的拷贝数/倍数句（freq 带 X → 扩增倍数，否则拷贝数） */
    private static String copySentence(String freq) {
        String empty = "";
        if (!StringUtils.hasText(freq) || ".".equals(freq.trim())) {
            return empty;
        }
        String value = freq.trim();
        return value.contains("X") ? "，此突变在样本中的扩增倍数为" + value : "，此突变在样本中的拷贝数为" + value;
    }

    /** 末尾丰度句（en7：含「合」→ 基因型；否则变异丰度为 X%） */
    private static String freqSuffix(String freq) {
        String empty = "";
        if (!StringUtils.hasText(freq) || ".".equals(freq.trim())) {
            return empty;
        }
        String value = freq.trim();
        if (value.contains("合")) {
            return "，此突变在样本中的基因型为" + value + "。";
        }
        return "，此突变在样本中的变异丰度为" + value + "%。";
    }

    /** 大片段缺失/重复的外显子范围（exon5-8 → 第5号到第8号外显子；exon5 → 第5号外显子） */
    private static String exonRange(String oriVariant) {
        Matcher range = Pattern.compile("exon(\\d+)-(\\d+)").matcher(oriVariant);
        if (range.find()) {
            return "第" + range.group(1) + "号到第" + range.group(2) + "号外显子";
        }
        Matcher single = Pattern.compile("exon(\\d+)").matcher(oriVariant);
        if (single.find()) {
            return "第" + single.group(1) + "号外显子";
        }
        return "第1号到第2号外显子";
    }

    /** 融合话术（translate_hgvs.pl 的 Fusion 分支，含基因间区分支） */
    private static String fusionText(String gene, String oriVariant) {
        String[] tokens = oriVariant.split(" ");
        String geneStr = tokens.length > 0 ? tokens[0] : "";
        String bpStr = tokens.length > 2 ? tokens[2] : "";
        String gene1;
        String gene2;
        if (geneStr.matches("^" + Pattern.quote(gene) + "-(.+)$")) {
            gene1 = gene;
            gene2 = geneStr.replaceFirst("^" + Pattern.quote(gene) + "-", "");
        } else if (geneStr.matches("^(.+)-" + Pattern.quote(gene) + "$")) {
            gene1 = geneStr.substring(0, geneStr.lastIndexOf('-'));
            gene2 = gene;
        } else {
            int dash = geneStr.indexOf('-');
            gene1 = dash > 0 ? geneStr.substring(0, dash) : geneStr;
            gene2 = dash > 0 ? geneStr.substring(dash + 1) : "";
        }
        String[] bps = bpStr.split(":");
        String str1 = bps.length > 0 && bps[0].length() > 1 ? bps[0].substring(1) : "";
        String str2 = bps.length > 1 && bps[1].length() > 1 ? bps[1].substring(1) : "";
        String bp1 = firstDigits(str1);
        String bp2 = firstDigits(str2);
        if (str1.contains("intergenic") && str2.contains("intergenic")) {
            return "基因间区和基因间区发生融合。";
        }
        if (str1.contains("intergenic")) {
            return "基因间区和基因" + gene2 + "的" + bp2 + "号外显子发生融合。";
        }
        if (str2.contains("intergenic")) {
            return "基因" + gene1 + "的" + bp1 + "号外显子和基因间区发生融合。";
        }
        return "基因" + gene1 + "的" + bp1 + "号外显子和基因" + gene2 + "的" + bp2 + "号外显子发生融合。";
    }

    private static String firstDigits(String value) {
        Matcher matcher = Pattern.compile("(\\d+)").matcher(value);
        return matcher.find() ? matcher.group(1) : "";
    }

    /**
     * 碱基变异描述（mutation_explanation.pm 的 chgvs）
     *
     * @param exon  外显子串（exon11 / intron2 / promoter / exon5_exon8）
     * @param chgvs cHGVS
     * @return 中文描述；不匹配任何形态时返回 null（与 Perl 一致）
     */
    static String cHgvs(String exon, String chgvs) {
        String miss = null;
        if (!StringUtils.hasText(chgvs)) {
            return miss;
        }
        String[] region = exon == null ? new String[0] : exon.split("_");
        String exon1 = region.length > 0 ? exonText(region[0]) : "";
        String exon2 = region.length > 1 ? exonText(region[1]) : "";
        String substitution = substitutionText(exon1, chgvs);
        return substitution == null ? indelText(exon1, exon2, chgvs) : substitution;
    }

    /** 单碱基/多碱基替换（Perl 的前两条分支） */
    private static String substitutionText(String exon1, String chgvs) {
        String miss = null;
        Matcher matcher = C_SNV.matcher(chgvs);
        if (matcher.find()) {
            return "位于" + exon1 + "的" + cPos(matcher.group(1)) + "核苷酸" + matcher.group(2)
                + "被核苷酸" + matcher.group(3) + "替代";
        }
        matcher = C_MNP.matcher(chgvs);
        if (matcher.find()) {
            return "位于" + exon1 + "的" + cPos(matcher.group(1)) + "到" + cPos(matcher.group(2))
                + "核苷酸" + matcher.group(3) + "被核苷酸" + matcher.group(4) + "替代";
        }
        return miss;
    }

    /** 插入/重复/缺失/缺失插入（Perl 的后七条分支） */
    private static String indelText(String exon1, String exon2, String chgvs) {
        String miss = null;
        Matcher matcher = C_INS.matcher(chgvs);
        if (matcher.find()) {
            return "位于" + exon1 + cPos(matcher.group(1)) + "与" + exon2 + cPos(matcher.group(2))
                + "之间插入核苷酸" + matcher.group(3);
        }
        matcher = C_DUP_ONE.matcher(chgvs);
        if (matcher.find()) {
            return "位于" + exon1 + "的" + cPos(matcher.group(1)) + "核苷酸" + nullToEmpty(matcher.group(2)) + "发生重复";
        }
        matcher = C_DUP_RANGE.matcher(chgvs);
        if (matcher.find()) {
            return "位于" + rangePrefix(exon1, exon2, matcher.group(1), matcher.group(2))
                + "核苷酸" + nullToEmpty(matcher.group(3)) + "发生重复";
        }
        matcher = C_DEL_ONE.matcher(chgvs);
        if (matcher.find()) {
            return "位于" + exon1 + "的" + cPos(matcher.group(1)) + "缺失核苷酸" + nullToEmpty(matcher.group(2));
        }
        matcher = C_DEL_RANGE.matcher(chgvs);
        if (matcher.find()) {
            return "位于" + rangePrefix(exon1, exon2, matcher.group(1), matcher.group(2))
                + "缺失核苷酸" + nullToEmpty(matcher.group(3));
        }
        matcher = C_DELINS_ONE.matcher(chgvs);
        if (matcher.find()) {
            return "位于" + exon1 + "的" + cPos(matcher.group(1)) + "缺失核苷酸" + nullToEmpty(matcher.group(2))
                + "并插入核苷酸" + matcher.group(3);
        }
        matcher = C_DELINS_RANGE.matcher(chgvs);
        if (matcher.find()) {
            return "位于" + rangePrefix(exon1, exon2, matcher.group(1), matcher.group(2))
                + "缺失核苷酸" + nullToEmpty(matcher.group(3)) + "并插入核苷酸" + matcher.group(4);
        }
        return miss;
    }

    /** 跨区域位置前缀：跨外显子时 exon2 前缀接在后面，否则只用「的」 */
    private static String rangePrefix(String exon1, String exon2, String pos1, String pos2) {
        return StringUtils.hasText(exon2)
            ? exon1 + cPos(pos1) + "到" + exon2 + cPos(pos2)
            : exon1 + "的" + cPos(pos1) + "到" + cPos(pos2);
    }

    /**
     * 氨基酸变异描述（mutation_explanation.pm 的 phgvs）
     *
     * @param phgvs pHGVS（如 p.V559D）
     * @return 中文描述；不在 Perl 覆盖范围时返回 null
     */
    static String pHgvs(String phgvs) {
        String miss = null;
        if (!StringUtils.hasText(phgvs)) {
            return miss;
        }
        Matcher matcher = P_FS.matcher(phgvs);
        if (matcher.find()) {
            return "第" + matcher.group(2) + "位氨基酸" + aa(matcher.group(1)) + "被氨基酸" + aa(matcher.group(3))
                + "替代并发生移码" + frameStop(phgvs);
        }
        matcher = P_FS_SHORT.matcher(phgvs);
        if (matcher.find()) {
            return "第" + matcher.group(2) + "位氨基酸" + aa(matcher.group(1)) + "被替代并发生移码" + frameStop(phgvs);
        }
        matcher = P_DUP_ANY.matcher(phgvs);
        if (matcher.find()) {
            return "第" + matcher.group(2) + "位氨基酸" + aaList(matcher.group(1)) + "重复";
        }
        matcher = P_DUP_RANGE.matcher(phgvs);
        if (matcher.find()) {
            return "第" + matcher.group(2) + "位氨基酸" + aa(matcher.group(1)) + "到第" + matcher.group(4)
                + "位氨基酸" + aa(matcher.group(3)) + "重复";
        }
        matcher = P_SYNONYMOUS.matcher(phgvs);
        if (matcher.find()) {
            return "第" + matcher.group(2) + "位氨基酸" + aa(matcher.group(1)) + "未发生改变";
        }
        matcher = P_SNV.matcher(phgvs);
        if (matcher.find()) {
            return "第" + matcher.group(2) + "位氨基酸" + aa(matcher.group(1)) + "被氨基酸" + aa(matcher.group(3)) + "替代";
        }
        return aminoIndelText(phgvs);
    }

    /** 氨基酸层面的插入/缺失/缺失插入（pHgvs 的后半段分支） */
    private static String aminoIndelText(String phgvs) {
        String miss = null;
        Matcher matcher = P_INS.matcher(phgvs);
        if (matcher.find()) {
            return "第" + matcher.group(2) + "位氨基酸" + aa(matcher.group(1)) + "与第" + matcher.group(4)
                + "位氨基酸" + aa(matcher.group(3)) + "之间插入氨基酸" + withStop(matcher.group(5));
        }
        matcher = P_DEL_ONE.matcher(phgvs);
        if (matcher.find()) {
            return "第" + matcher.group(2) + "位氨基酸" + aa(matcher.group(1)) + "缺失";
        }
        matcher = P_DEL_RANGE.matcher(phgvs);
        if (matcher.find()) {
            return "第" + matcher.group(2) + "位氨基酸" + aa(matcher.group(1)) + "到第" + matcher.group(4)
                + "位氨基酸" + aa(matcher.group(3)) + "缺失";
        }
        matcher = P_DELINS_ONE.matcher(phgvs);
        if (matcher.find()) {
            return "第" + matcher.group(2) + "位氨基酸" + aa(matcher.group(1)) + "缺失并插入氨基酸" + withStop(matcher.group(3));
        }
        matcher = P_DELINS_RANGE.matcher(phgvs);
        if (matcher.find()) {
            return "第" + matcher.group(2) + "位氨基酸" + aa(matcher.group(1)) + "到第" + matcher.group(4)
                + "位氨基酸" + aa(matcher.group(3)) + "缺失并插入氨基酸" + withStop(matcher.group(5));
        }
        return miss;
    }


    /** 插入/缺失插入的氨基酸串（Perl 会把「终止」替换成「并生成终止」） */
    private static String withStop(String aminoAcids) {
        return aaList(aminoAcids).replace("终止", "并生成终止");
    }

    /** 移码的终止密码子后缀（en7 的 fs\*n / fs\*? 两种写法） */
    private static String frameStop(String phgvs) {
        String empty = "";
        Matcher stop = Pattern.compile("fs\\*(\\d+)").matcher(phgvs);
        if (stop.find()) {
            return "，从此位置开始第" + stop.group(1) + "位为终止密码子(*)";
        }
        return phgvs.contains("fs*?") ? "，终止密码子位置无法确定" : empty;
    }

    /** 外显子/内含子/启动子描述（mutation_explanation.pm 的 exon） */
    static String exonText(String exon) {
        String empty = "";
        if (exon == null) {
            return empty;
        }
        Matcher matcher = Pattern.compile("exon(\\d+)").matcher(exon);
        if (matcher.find()) {
            return matcher.group(1) + "号外显子上";
        }
        matcher = Pattern.compile("intron(\\d+)").matcher(exon);
        if (matcher.find()) {
            return matcher.group(1) + "号内含子上";
        }
        matcher = Pattern.compile("IVS(\\d+)").matcher(exon);
        if (matcher.find()) {
            return matcher.group(1) + "号内含子上";
        }
        return exon.contains("promoter") ? "启动子区" : empty;
    }

    /** 碱基位置描述（mutation_explanation.pm 的 cpos） */
    static String cPos(String pos) {
        String prefix = pos.contains("*") ? "翻译终止密码子下游" : "";
        Matcher matcher = Pattern.compile("(\\d+)-(\\d+)").matcher(pos);
        if (matcher.find()) {
            return prefix + "-" + matcher.group(2) + "位";
        }
        matcher = Pattern.compile("(\\d+)\\+(\\d+)").matcher(pos);
        if (matcher.find()) {
            return prefix + "+" + matcher.group(2) + "位";
        }
        return prefix + "第" + pos + "位";
    }

    /** 多氨基酸串 → 中文（贪心切 token，与 Perl 的 while(s/^token//) 等价） */
    private static String aaList(String value) {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        while (index < value.length()) {
            int length = 1;
            while (index + length < value.length() && length < 3
                && Character.isLowerCase(value.charAt(index + length))) {
                length++;
            }
            sb.append(aa(value.substring(index, index + length)));
            index += length;
        }
        return sb.toString();
    }

    private static String aa(String token) {
        String value = AA.get(token);
        return value == null ? token : value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
