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
     * @param diseaseIds     癌种ID范围（自身 + 全部父级）
     * @param limit          最多返回条数
     * @return 证据列表
     */
    List<PreviewDrugVo> selectDrugAnnotations(@Param("geneVariantIds") List<Long> geneVariantIds,
                                             @Param("diseaseIds") List<Long> diseaseIds,
                                             @Param("limit") int limit);
}
