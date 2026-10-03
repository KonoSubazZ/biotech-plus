package org.dromara.report.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 报告预览：单个位点（体细胞或胚系）
 *
 * @author <你的名字>
 */
@Data
public class PreviewVariantVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 位点主键（明细表 id） */
    private Long sourceId;

    /** SNP_INDEL / CNV / FUSION / CR_ALL */
    private String sourceType;

    /** 基因 */
    private String gene;

    /** 规范化位点（gene_variant） */
    private String variant;

    /** 原始位点描述 */
    private String oriVariant;

    /** 突变类型（D 缺失/I 插入/S 替换/CNV/Fusion 等） */
    private String mutationType;

    /** 突变丰度 */
    private String frequency;

    /** 深度 */
    private String depth;

    /** 合子状态（胚系用） */
    private String zygosity;

    /** 胚系五级临床意义：1~5；体细胞为 null */
    private Integer clinicalSignificance;

    /** 临床意义中文标签：致病/可能致病/未知临床意义/可能良性/良性 */
    private String clinicalSignificanceLabel;

    /** MATCHED（知识库命中）/ NOT_MATCHED（无证据） */
    private String matchStatus;

    /** 变异分类（NKB 的 Class 口径，如 Class5(致病)） */
    private String variationClass;

    /**
     * 是否复用冻结的历史匹配结果。
     * <p>
     * **不序列化**：首次预览会写历史、第二次就变成复用，若把它放进 JSON 会破坏
     * 「同输入产生相同 JSON」这条契约。需要看的话在服务端日志里查。
     */
    @JsonIgnore
    private Boolean fromHistory;

    /** 人工改靶：file_CR_ALL.parent_mutation_id（参与匹配键，用于区分改靶前后） */
    private Long parentMutationId;

    /** 文件里给出的原始临床判定（CR_ALL.clnsig，如 Pathogenic），仅供人工参考 */
    private String sourceClnsig;

    /** 文件里给出的分类（CR_ALL.classification_lovd，如 Class5(致病)） */
    private String classificationLovd;

    /** 药物证据；无证据时为空列表 */
    private List<PreviewDrugVo> drugMatch;
}
