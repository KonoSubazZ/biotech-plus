package org.dromara.drive.utils;

/**
 * 驱动支持的文件类型（第一版只做 1238 产品的 10 个固定文件）。
 * <p>
 * 枚举顺序即匹配顺序：文件名按「样本编号+T+后缀」精确匹配，长后缀必须排在短后缀之前
 * （{@code .G.qc.txt} 在 {@code .qc.txt} 前面，否则 G.qc 会被 qc 抢走）。
 * 字段含义沿用旧 Python 驱动 {@code config/file_config.json}：
 * <ul>
 *   <li>{@code code} → {@code analysis_file_status.file_type}（= 旧配置的 ori_file_type）；</li>
 *   <li>{@code dataType} → {@code analysis_file_status.data_type}；</li>
 *   <li>{@code detailTable} → 明细落库表；</li>
 *   <li>{@code variantCategory} → 参与 {@code total_variant_count} 的哪一段（null = 不统计）。</li>
 * </ul>
 *
 * @author <你的名字>
 */
public enum DriveFileType {

    /** 体细胞 SNV（明细与 Indel 共用 file_Somatic_SNV_Indel） */
    SNP("SNP", ".SomaticSNV.tsv", "variant", "file_Somatic_SNV_Indel", "snp_del"),

    /** 体细胞 Indel */
    INDEL("Indel", ".SomaticInDel.tsv", "variant", "file_Somatic_SNV_Indel", "snp_del"),

    /** 拷贝数变异 */
    CNV("CNV", ".CNV.tsv", "variant", "file_CNV", "cnv"),

    /** 融合 */
    FUSION("Fusion", ".Fusion.tsv", "variant", "file_Fusion", "fusion"),

    /** 胚系全量（CR_ALL） */
    CR_ALL("CR_ALL", ".CR_ALL.tsv", "variant", "file_CR_ALL", "cr_all"),

    /** 微卫星不稳定 */
    MSI("MSI", ".MSI.tsv", "variant", "file_MSI", null),

    /** 对照样本质控（G.qc）—— 必须排在 QC 之前 */
    QC_CONTROL("qc_g", ".G.qc.txt", "variant", "file_qc_control", null),

    /** 样本质控 */
    QC("qc", ".qc.txt", "variant", "file_qc", null),

    /** 化疗位点（文本 TSV，扩展名是 .xls） */
    CHEM("Chem", ".Chem.xls", "variant", "file_Chem", null),

    /** 化疗用药原始 JSON */
    CHEMICAL_ALL("Chemical_all", ".chemical.json", "druginfo", "file_chemical", null);

    private final String code;
    private final String suffix;
    private final String dataType;
    private final String detailTable;
    private final String variantCategory;

    DriveFileType(String code, String suffix, String dataType, String detailTable, String variantCategory) {
        this.code = code;
        this.suffix = suffix;
        this.dataType = dataType;
        this.detailTable = detailTable;
        this.variantCategory = variantCategory;
    }

    public String code() {
        return code;
    }

    public String suffix() {
        return suffix;
    }

    public String dataType() {
        return dataType;
    }

    public String detailTable() {
        return detailTable;
    }

    public String variantCategory() {
        return variantCategory;
    }

    /**
     * 该样本目录下一个文件的期望文件名。
     *
     * @param subbarcode 样本编号（目录名，不含结尾 T）
     * @return 形如 {@code TKHS260138569-1AT.SomaticSNV.tsv}
     */
    public String fileNameOf(String subbarcode) {
        return subbarcode + "T" + suffix;
    }

    /**
     * 按 analysis_file_status.file_type 反查类型。
     *
     * @param code 文件类型码
     * @return 类型，未实现返回 null
     */
    public static DriveFileType byCode(String code) {
        if (code == null) {
            return null;
        }
        for (DriveFileType type : values()) {
            if (type.code.equalsIgnoreCase(code.strip())) {
                return type;
            }
        }
        return null;
    }
}
