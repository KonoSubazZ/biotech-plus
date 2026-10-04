package org.dromara.report.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

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

    /** 人工改靶：源位点表 parent_mutation_id（逗号分隔的 NKB 节点ID，可多个；参与匹配键，用于区分改靶前后） */
    private List<Long> parentMutationIds;

    /**
     * 人工改靶父级的节点名，**与 {@link #parentMutationIds} 同序**（取不到时该位为 null）。
     * 只给界面回显用（候选列表按关键词截断，选中的节点可能不在里面）。
     */
    private List<String> parentMutationNames;

    /** 文件里给出的原始临床判定（CR_ALL.clnsig，如 Pathogenic），仅供人工参考 */
    private String sourceClnsig;

    /** 文件里给出的分类（CR_ALL.classification_lovd，如 Class5(致病)） */
    private String classificationLovd;

    /** 药物证据（全部保留：每个「药物 × 等级 × 证据癌种」一行；无证据时为空列表） */
    private List<PreviewDrugVo> drugMatch;

    /**
     * 按等级分组的药物名串（en7 的 drugsA/drugsB/drugsC/drugsD + resistant_drugsA..D）。
     * <p>
     * 去重键 = 药名 + 证据癌种（与 en7 {@code getDrugNameStr} 一致）；
     * 明细仍在 {@link #drugMatch} 里**全量保留**，不因为名字去重而丢证据。
     * 需要其他检测的药物名后缀 `#`（en7 的 other_test_required 标记）。
     */
    private Map<String, String> drugGroups;

    /** 审核列表（en7 buildDrugAuditList）：{drug, relation, level, evidenceDiseaseId, evidenceDiseaseName} */
    private List<Map<String, Object>> drugAuditList;

    /** 是否在知识库中查到节点 */
    private Boolean inNkb;

    /** 知识库命中节点名（自身或父级，如 V559D / Active Mutation / Inactive Mutation） */
    private String matchedNode;

    /**
     * 关联突变节点名列表：命中节点自身 + 一层父级（en7 的 mutationIdList 口径）。
     * 知识库未收录时为空列表；`基因 位点` 那一条由前端追加。
     */
    private List<String> relatedMutations;

    /** 知识库功能判定（effect 字典：激活/失活/未知/无影响） */
    private String effectText;

    /** 位点用药说明（en7 四段模板：失活/扩增/缺失/未明） */
    private String description;

    /** 命中的知识库节点ID（自身或父级；位点说明按它取） */
    private Long matchedMutationId;

    /** 基因说明（NKB gene_description.gene_description_chinese，Approved） */
    private String geneDescription;

    /** 信号通路说明（NKB gene_description.pathway_description_chinese，Approved；与基因说明同一行） */
    private String pathwayDescription;

    /** 位点说明（NKB gene_variant_description.description_chinese，按命中节点） */
    private String variantDescription;

    /** 突变说明（HGVS → 中文，移植 en7 的 translate_hgvs.pl + mutation_explanation.pm） */
    private String mutationExplanation;

    /** 来源文件类型（data_file_status.file_type：SNP / Indel / CNV / Fusion / CR_ALL） */
    private String fileType;

    /** 核酸类型：DNA / RNA（只有融合行才有值，取自 file_Fusion.fusion_quality） */
    private String nucleicAcid;

    /** 类型列展示值（体系|变异类别|核酸类型，见 NkbDrugMatcher#typeText） */
    private String typeText;

    /** 变异类型展示值（VEP ExonicFunc → 中文：错义突变/移码突变…，见 PreviewSupport#variantTypeText） */
    private String variantTypeText;

    /** 丰度/reads 展示值：DNA → "45.47%"；RNA → reads 数（无单位）；扩增/缺失 → 拷贝数 */
    private String abundanceText;

    /** 默认排序权重（en7 setOrderNum，越大越靠前；不展示给前端） */
    private Float orderNum;

    /** 位点类别的排序辅助（en7 用：Cosmic 有无 + 药物最高等级） */
    private Integer topLevelWeight;
}
