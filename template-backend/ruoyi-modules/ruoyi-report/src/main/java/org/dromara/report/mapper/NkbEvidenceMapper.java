package org.dromara.report.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.report.domain.vo.PreviewDrugVo;

import java.util.List;
import java.util.Map;

/**
 * NKB 知识库只读查询（跨库 nkb schema）
 * <p>
 * 所有查询都必须放在 {@code TenantContext.withoutTenant(...)} 里调用：
 * nkb 表没有 tenant_id，租户拦截器一旦给它加上租户条件就会报 Unknown column。
 *
 * @author <你的名字>
 */
public interface NkbEvidenceMapper {

    /**
     * 按癌种中文名解析 NKB 疾病
     *
     * @param diseaseName 癌种（如 结直肠癌）
     * @return do_id / diseaseName；无则 null
     */
    Map<String, Object> selectDiseaseByName(@Param("diseaseName") String diseaseName);

    /**
     * 按ID取疾病中文名（圣域规则要判断「当前癌种或任一父级」是否属于乳腺/卵巢/前列腺癌）
     *
     * @param diseaseIds 疾病ID列表
     * @return 疾病中文名列表
     */
    List<String> selectDiseaseNames(@Param("diseaseIds") List<Long> diseaseIds);

    /**
     * 取一批疾病的父级（disease_hierarchy.is_a），用于逐级向上收敛癌种范围
     *
     * @param diseaseIds 当前层级疾病ID
     * @return 父级疾病ID列表
     */
    List<Long> selectParentDiseaseIds(@Param("diseaseIds") List<Long> diseaseIds);

    /**
     * 按基因符号解析 NKB 基因
     *
     * @param geneSymbol 基因符号
     * @return gene_id / geneSymbol；无则 null
     */
    Map<String, Object> selectGeneBySymbol(@Param("geneSymbol") String geneSymbol);

    /**
     * 精确位点匹配：gene_variant 等于规范化位点，或 ori_variant 与报告的原始位点一致
     *
     * @param geneId     NKB 基因ID
     * @param variant    规范化位点（如 V559D）
     * @param oriVariant 原始位点（如 NM_000222.3 exon11 c.1676T>A p.V559D）
     * @return gene_variant_id 列表
     */
    List<Long> selectGeneVariantIdsByVariant(@Param("geneId") Long geneId,
                                            @Param("variant") String variant,
                                            @Param("oriVariant") String oriVariant);

    /**
     * 胚系按临床分级匹配（NKB 用 gene_variant = 'Class5(致病)' 这类口径表示胚系致病性）
     *
     * @param geneId      NKB 基因ID
     * @param classPrefix 分级前缀（如 Class5 / Class4）
     * @return gene_variant_id 列表
     */
    List<Long> selectGeneVariantIdsByClass(@Param("geneId") Long geneId,
                                          @Param("classPrefix") String classPrefix);

    /**
     * 取药物证据（按基因位点 + 癌种范围过滤）
     *
     * @param geneVariantIds NKB gene_variant_id 列表
     * @param diseaseIds     癌种ID范围（本癌种 + 祖先 + 子孙）
     * @param mutationType   突变类型：S 体细胞 / G 胚系（SQL 里按 IN (#{mutationType}, 'S/G') 取）
     * @param limit          最多返回条数
     * @return 证据列表（未分级）
     */
    List<PreviewDrugVo> selectDrugAnnotations(@Param("geneVariantIds") List<Long> geneVariantIds,
                                             @Param("diseaseIds") List<Long> diseaseIds,
                                             @Param("mutationType") String mutationType,
                                             @Param("limit") int limit);

    /**
     * 位点节点（含 effect 与父级名称）——en7 的 gene_variant_evw 等价查询
     * （本机 sql_mode 含 only_full_group_by，视图 gene_variant_evw 报 1055，故用等价自建查询）
     *
     * @param geneSymbol  基因符号
     * @param geneVariant 位点（短名，如 V559D / Inactive Mutation）
     * @return mutationId / variantName / effectText / parentVariants；无则 null
     */
    Map<String, Object> selectVariantNode(@Param("geneSymbol") String geneSymbol,
                                          @Param("geneVariant") String geneVariant);

    /**
     * 一个节点的父级ID列表（一层）——en7 的 getParentMutationId
     *
     * @param mutationId 节点ID
     * @return 父级节点ID列表
     */
    List<Long> selectParentMutationIds(@Param("mutationId") Long mutationId);

    /**
     * 子孙癌种展开：取这些癌种的直接子级（en7 getSonDiseaseList，Java 里递归）
     *
     * @param diseaseIds 当前层癌种ID
     * @return 子级癌种ID列表
     */
    List<Long> selectChildDiseaseIds(@Param("diseaseIds") List<Long> diseaseIds);

    /**
     * 其他癌种「获批上市」（evidence_phase_id = 24）的药物证据；en7 会把它们**降格为 C 级**输出
     *
     * @param geneVariantIds 节点ID列表
     * @param diseaseIds     本癌种范围（做 NOT IN）
     * @param excludedIds    需要排除的癌种（性别/实体瘤·血液瘤剔除集合，可空）
     * @param mutationType   突变类型
     * @param limit          最多返回条数
     * @return 证据列表
     */
    List<PreviewDrugVo> selectOtherApprovedDrugs(@Param("geneVariantIds") List<Long> geneVariantIds,
                                                @Param("diseaseIds") List<Long> diseaseIds,
                                                @Param("excludedIds") List<Long> excludedIds,
                                                @Param("mutationType") String mutationType,
                                                @Param("limit") int limit);

    /**
     * 基因说明（NKB gene_description，Approved；en7 用 nkb_onco_gene_description_evw）
     *
     * @param geneSymbol 基因符号
     * @return {geneDescription, relatedPathway, pathwayDescription}；无则 null
     */
    Map<String, Object> selectGeneDescription(@Param("geneSymbol") String geneSymbol);

    /**
     * 位点说明（NKB gene_variant_description，按命中的知识库节点，Approved）
     *
     * @param mutationId 命中的节点ID（自身或父级）
     * @return {variantDescription, simpleDescription, annotationType}；无则 null
     */
    Map<String, Object> selectVariantDescription(@Param("mutationId") Long mutationId);

    /**
     * 一批节点ID → 节点名（关联突变展示用；按传入顺序返回，Approved 之外的不返回）
     *
     * @param mutationIds 节点ID列表（命中节点自身 + 一层父级）
     * @return 节点名列表
     */
    List<String> selectVariantNames(@Param("mutationIds") List<Long> mutationIds);

    /**
     * 获批上市(24)说明：按 (药,癌种) 取批准说明 + 获批机构
     *
     * @param drugIds    药物ID集合
     * @param diseaseIds 癌种ID集合
     * @return {drugId, diseaseId, approvingAgency, approvalDescription}
     */
    List<Map<String, Object>> selectApprovalDescriptions(@Param("drugIds") List<Long> drugIds,
                                                         @Param("diseaseIds") List<Long> diseaseIds);

    /**
     * 指南推荐(23)说明：按 (药,癌种) 取 NCCN/CSCO 指南描述（Java 侧按 type 拼装）
     *
     * @param drugIds    药物ID集合
     * @param diseaseIds 癌种ID集合
     * @return {drugId, diseaseId, guidelineType, guidelineTitle, guidelineDescription}
     */
    List<Map<String, Object>> selectGuidelineDescriptions(@Param("drugIds") List<Long> drugIds,
                                                          @Param("diseaseIds") List<Long> diseaseIds);

    /**
     * 临床试验证据（第二类证据）：按注释ID取招募中的试验
     *
     * @param annotationIds 证据注释ID集合
     * @param diseaseIds    本癌种范围（用于剔除 clinical_trial_exclude_disease）
     * @return {annotationId, drugId, drugName, trialId, title, trialCondition, phase, location, phaseOrder}
     */
    List<Map<String, Object>> selectClinicalTrials(@Param("annotationIds") List<Long> annotationIds,
                                                   @Param("diseaseIds") List<Long> diseaseIds);

    /**
     * 按ID取节点名（人工改靶父级回显用；不排序，调用方按ID自己对齐）
     *
     * @param mutationIds 节点ID列表
     * @return {mutationId, variantName} 列表
     */
    List<Map<String, Object>> selectVariantNamesByIds(@Param("mutationIds") List<Long> mutationIds);

    /**
     * 改靶候选：按关键词查 NKB 位点节点（Approved）
     * <p>
     * 匹配「基因符号前缀」或「节点名包含」，所以传基因符号（KIT）会列出该基因的全部节点，
     * 传具体位点（V559）会做模糊定位。
     *
     * @param keyword 关键词（基因符号 / 节点名片段）
     * @param limit   最多返回条数
     * @return {mutationId, gene, variantName, effectText} 列表
     */
    List<Map<String, Object>> selectVariantCandidates(@Param("keyword") String keyword,
                                                     @Param("limit") int limit);

    /**
     * 某注释在给定癌种范围内「招募中」的临床试验数（en7 getClinicalNumber，用于 give 判定）
     *
     * @param annotationId 注释ID
     * @param diseaseIds   父级癌种范围
     * @return 数量
     */
    Integer selectRecruitingTrialCount(@Param("annotationId") Long annotationId,
                                      @Param("diseaseIds") List<Long> diseaseIds);
}
