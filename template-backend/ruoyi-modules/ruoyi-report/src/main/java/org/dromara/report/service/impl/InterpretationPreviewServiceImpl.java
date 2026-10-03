package org.dromara.report.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.TenantContext;
import org.dromara.report.domain.bo.InterpretationGermlineSignificanceBo;
import org.dromara.report.domain.bo.InterpretationPreviewBo;
import org.dromara.report.domain.bo.InterpretationTargetBo;
import org.dromara.report.domain.vo.InterpretationContextVo;
import org.dromara.report.domain.vo.InterpretationPreviewVo;
import org.dromara.report.domain.vo.InterpretationLimsVo;
import org.dromara.report.domain.vo.PreviewDrugVo;
import org.dromara.report.domain.vo.PreviewSectionVo;
import org.dromara.report.domain.vo.PreviewVariantVo;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.mapper.NkbEvidenceMapper;
import org.dromara.report.service.IInterpretationPreviewService;
import org.dromara.report.service.IInterpretationService;

import static org.dromara.report.service.impl.PreviewSupport.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 报告预览组装（设计书 §7.6 JSON 契约 + §8 预览接口）
 * <p>
 * 与设计书对齐的关键点：
 * <ul>
 *   <li>只查「报出（is_reported=1）」的位点；体细胞 = SNP/Indel + CNV + Fusion，胚系 = CR_ALL。</li>
 *   <li>匹配历史：先按 match_key（规范化条件 SHA-256）查 history_somatic/history_germline，
 *       命中就复用冻结的 match_result（只替换来源位点ID）；未命中才查 NKB 并用 INSERT IGNORE 写首条。</li>
 *   <li>胚系五级临床意义 1~5（默认 3）；改靶产生新键时从同客户/产品/癌种/性别/位点/合子状态
 *       <b>最近一条</b>继承（故意忽略 parent_mutation_id）。</li>
 *   <li>确定性：同输入产生相同 JSON，不写时间戳/随机值；缺失业务值保持 null，不替换成 / 或 -。</li>
 *   <li>NKB 是跨库只读表（没有 tenant_id），全部查询包在 {@code TenantContext.withoutTenant} 里。</li>
 * </ul>
 *
 * @author <你的名字>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterpretationPreviewServiceImpl implements IInterpretationPreviewService {

    private final InterpretationMapper interpretationMapper;
    private final NkbEvidenceMapper nkbEvidenceMapper;
    private final IInterpretationService interpretationService;

    /** 癌种向上收敛的最大层数（MySQL 5.7 无递归 CTE，逐层查父级） */
    private static final int MAX_DISEASE_DEPTH = 6;

    /** 药物证据单次最多返回条数 */
    private static final int MAX_EVIDENCE = 30;

    private static final Map<Integer, String> SIGNIFICANCE_LABEL = Map.of(
        1, "致病", 2, "可能致病", 3, "未知临床意义", 4, "可能良性", 5, "良性");

    /** 胚系临床意义 → NKB 的 Class 口径（NKB 用 gene_variant='Class5(致病)' 表示胚系致病性） */
    private static final Map<Integer, String> SIGNIFICANCE_CLASS = Map.of(1, "Class5", 2, "Class4");

    /** 圣域个性化模块编码（对齐设计书 7.7） */
    private static final String MODULE_SHENGYU_SOMATIC = "SHENGYU_SOMATIC_VARIANTS_V1";
    private static final String MODULE_SHENGYU_GERMLINE = "SHENGYU_GERMLINE_VARIANTS_V1";

    /** 圣域体细胞输出条件：当前癌种或任一父级属于这三类 */
    private static final Set<String> SHENGYU_REQUIRED_DISEASES = Set.of("乳腺癌", "卵巢癌", "前列腺癌", "乳腺癌症");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterpretationPreviewVo buildPreview(InterpretationPreviewBo bo) {
        Map<String, Object> report = loadReport(bo.getReportId(), bo.getAnalysisId());
        InterpretationContextVo context = interpretationService.loadContext(bo.getReportId(), bo.getAnalysisId());
        PreviewContext pc = buildContext(bo.getAnalysisId(), bo.getReportId(), bo.getTemplateCode(), report, context);

        InterpretationPreviewVo vo = new InterpretationPreviewVo();
        vo.setAnalysisId(bo.getAnalysisId());
        vo.setReportId(bo.getReportId());
        vo.setTemplateCode(pc.getTemplateCode());
        vo.setTemplateVersion(pc.getTemplateVersion());
        vo.setReportInfo(buildReportInfo(pc));
        vo.setSampleInfo(buildSampleInfo(context.getLims()));

        List<PreviewVariantVo> somatic = matchSomaticVariants(pc);
        List<PreviewVariantVo> germline = matchGermlineVariants(pc);
        vo.setSomaticVariants(toSection(somatic));
        vo.setGermlineVariants(toSection(germline));
        vo.setShengyuSomaticVariants(shengyuSomatic(pc, somatic));
        vo.setShengyuGermlineVariants(shengyuGermline(pc, germline));

        List<String> warnings = new ArrayList<>(context.getWarnings());
        if (pc.getDiseaseIds().isEmpty()) {
            warnings.add("NKB 未识别到癌种「" + pc.getDisease() + "」，药物证据未按癌种过滤");
        }
        if (somatic.isEmpty() && germline.isEmpty()) {
            warnings.add("该分析批次没有「报出」的位点，请先在『筛选位点』页设置报出");
        }
        vo.setWarnings(warnings);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGermlineSignificance(InterpretationGermlineSignificanceBo bo) {
        Integer significance = bo.getClinicalSignificance();
        if (significance == null || significance < 1 || significance > 5) {
            throw new ServiceException("临床意义只能是 1~5：1致病/2可能致病/3未知临床意义/4可能良性/5良性");
        }
        Map<String, Object> report = loadReport(bo.getReportId(), bo.getAnalysisId());
        Map<String, Object> row = interpretationMapper.selectGermlineVariant(bo.getSourceId(), bo.getAnalysisId());
        if (row == null) {
            throw new ServiceException("胚系位点不存在或不属于该分析批次：sourceId=" + bo.getSourceId());
        }
        InterpretationContextVo context = interpretationService.loadContext(bo.getReportId(), bo.getAnalysisId());
        PreviewContext pc = buildContext(bo.getAnalysisId(), bo.getReportId(), null, report, context);
        PreviewVariantVo item = new PreviewVariantVo();
        item.setSourceType("CR_ALL");
        item.setGene(asString(row.get("gene")));
        item.setVariant(asString(row.get("variant")));
        item.setOriVariant(asString(row.get("oriVariant")));
        item.setZygosity(asString(row.get("zygosity")));
        // 必须带上人工父级：它参与匹配键，漏了会把改靶后的确认写到旧键上（实测踩到）
        item.setParentMutationId(asLong(row.get("parentMutationId")));
        item.setMutationType("G");
        // 人工确认会改变致病性档位 → 按新档位的 Class 重新匹配证据，并冻结回同一条历史
        String classPrefix = SIGNIFICANCE_CLASS.get(significance);
        List<PreviewDrugVo> evidence = queryEvidence(pc, item, classPrefix);
        String status = evidence.isEmpty() ? "NOT_MATCHED" : "MATCHED";
        int affected = interpretationMapper.updateGermlineSignificance(matchKey(pc, item), significance,
            toJson(evidence), status, clsPrefixLabel(classPrefix));
        if (affected == 0) {
            throw new ServiceException("该位点还没有匹配历史，请先预览一次再确认临床意义");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGermlineTarget(InterpretationTargetBo bo) {
        int affected = interpretationMapper.updateGermlineParentMutation(bo.getSourceId(), bo.getAnalysisId(),
            bo.getParentMutationId());
        if (affected == 0) {
            throw new ServiceException("胚系位点不存在或不属于该分析批次：sourceId=" + bo.getSourceId());
        }
    }

    // ---------------------------------------------------------------- 上下文

    /** 报告必须存在且与入参 analysisId 一致（避免拿别的批次预览） */
    private Map<String, Object> loadReport(Long reportId, Long analysisId) {
        Map<String, Object> report = interpretationMapper.selectReportRow(reportId);
        if (report == null) {
            throw new ServiceException("报告不存在或已删除：reportId=" + reportId);
        }
        if (!analysisId.equals(asLong(report.get("analysisId")))) {
            throw new ServiceException("报告与分析批次不匹配：reportId=" + reportId + "，analysisId=" + analysisId);
        }
        return report;
    }

    private PreviewContext buildContext(Long analysisId, Long reportId, String templateCode,
                                        Map<String, Object> report, InterpretationContextVo context) {
        InterpretationLimsVo lims = context.getLims();
        String disease = firstNonBlank(asString(report.get("disease")), asString(report.get("cancerType")),
            lims.getCancerType(), "未知癌种");
        Long productId = asLong(report.get("productId"));
        Map<String, Long> diseaseIdByName = new LinkedHashMap<>();
        List<Long> diseaseIds = resolveDiseaseScope(disease, diseaseIdByName);
        Long diseaseId = diseaseIdByName.get(disease);

        // 客户：LIMS 医院名优先；缺失时用分析粒度占位（避免未知客户跨样本串用同一条历史）
        String customer = firstNonBlank(lims.getHospitalName(),
            "UNKNOWN_CUSTOMER@ANALYSIS:" + analysisId);

        PreviewContext pc = new PreviewContext();
        pc.setAnalysisId(analysisId);
        pc.setReportId(reportId);
        pc.setTemplateCode(firstNonBlank(templateCode, asString(report.get("templateCode")),
            asString(report.get("template"))));
        pc.setTemplateVersion(asString(report.get("templateVersion")));
        pc.setModuleCode(asString(report.get("moduleCode")));
        pc.setDisease(disease);
        pc.setDiseaseId(diseaseId);
        pc.setDiseaseIds(diseaseIds);
        pc.setGender(normalizeGender(lims.getGender()));
        pc.setCustomer(customer);
        pc.setProjectCode(firstNonBlank(asString(report.get("product")), ""));
        pc.setProductGenes(productId == null ? List.of() : interpretationMapper.selectProductGeneSymbols(productId));
        pc.setSpecimenType(lims.getSpecimenType());
        return pc;
    }

    /** 癌种 + 全部父级 ID（逐层向上，直到没有新父级） */
    private List<Long> resolveDiseaseScope(String disease, Map<String, Long> diseaseIdByName) {
        Map<String, Object> hit = nkb(() -> nkbEvidenceMapper.selectDiseaseByName(disease));
        if (hit == null) {
            return List.of();
        }
        Long rootId = asLong(hit.get("diseaseId"));
        diseaseIdByName.put(disease, rootId);
        List<Long> scope = new ArrayList<>(List.of(rootId));
        List<Long> current = List.of(rootId);
        for (int depth = 0; depth < MAX_DISEASE_DEPTH; depth++) {
            if (current.isEmpty()) {
                break;
            }
            // lambda 捕获的必须是 final：把当前层拷一份
            List<Long> layer = current;
            List<Long> parents = nkb(() -> nkbEvidenceMapper.selectParentDiseaseIds(layer));
            List<Long> fresh = parents == null ? List.of()
                : parents.stream().filter(id -> id != null && !scope.contains(id)).toList();
            scope.addAll(fresh);
            current = fresh;
        }
        return scope;
    }

    // ---------------------------------------------------------------- 匹配

    private List<PreviewVariantVo> matchSomaticVariants(PreviewContext pc) {
        List<PreviewVariantVo> items = new ArrayList<>();
        for (Map<String, Object> row : interpretationMapper.selectReportedSomaticVariants(pc.getAnalysisId())) {
            PreviewVariantVo item = baseItem(row);
            item.setMutationType(mutationTypeCode(item.getSourceType(), asString(row.get("mutationTypeRaw"))));
            item.setFrequency(asString(row.get("frequency")));
            item.setDepth(asString(row.get("depth")));
            matchWithHistory(pc, item, null, null);
            items.add(item);
        }
        return items;
    }

    private List<PreviewVariantVo> matchGermlineVariants(PreviewContext pc) {
        List<PreviewVariantVo> items = new ArrayList<>();
        for (Map<String, Object> row : interpretationMapper.selectReportedGermlineVariants(pc.getAnalysisId())) {
            PreviewVariantVo item = baseItem(row);
            item.setMutationType("G");
            item.setZygosity(asString(row.get("zygosity")));
            item.setDepth(asString(row.get("depth")));
            item.setSourceClnsig(asString(row.get("clnsig")));
            item.setParentMutationId(asLong(row.get("parentMutationId")));
            item.setClassificationLovd(null);
            Integer significance = resolveSignificance(pc, item);
            item.setClinicalSignificance(significance);
            item.setClinicalSignificanceLabel(SIGNIFICANCE_LABEL.get(significance));
            matchWithHistory(pc, item, significance, SIGNIFICANCE_CLASS.get(significance));
            items.add(item);
        }
        return items;
    }

    /**
     * 历史复用 or 首次知识库匹配。
     *
     * @param item             位点（已填好 gene/variant/oriVariant/mutationType）
     * @param significance     胚系临床意义（体细胞传 null）
     * @param classPrefix      胚系按 Class 匹配的前缀（体细胞传 null）
     */
    private void matchWithHistory(PreviewContext pc, PreviewVariantVo item, Integer significance, String classPrefix) {
        String matchKey = matchKey(pc, item);
        boolean germline = "CR_ALL".equals(item.getSourceType());
        Map<String, Object> history = germline
            ? interpretationMapper.selectGermlineHistory(matchKey)
            : interpretationMapper.selectSomaticHistory(matchKey);
        if (history != null) {
            item.setFromHistory(true);
            item.setMatchStatus(asString(history.get("match_status")));
            item.setVariationClass(asString(history.get("variation_class")));
            item.setDrugMatch(parseDrugMatch(asString(history.get("match_result"))));
            if (germline && history.get("clinical_significance") != null) {
                Integer frozen = asInt(history.get("clinical_significance"));
                item.setClinicalSignificance(frozen);
                item.setClinicalSignificanceLabel(SIGNIFICANCE_LABEL.get(frozen));
            }
            return;
        }

        List<PreviewDrugVo> evidence = queryEvidence(pc, item, classPrefix);
        item.setFromHistory(false);
        item.setDrugMatch(evidence);
        item.setMatchStatus(evidence.isEmpty() ? "NOT_MATCHED" : "MATCHED");
        item.setVariationClass(clsPrefixLabel(classPrefix));
        insertHistory(pc, item, matchKey, significance);
    }

    /** 查 NKB：基因 → gene_variant（精确位点或 Class 口径）→ 药物证据（癌种范围过滤） */
    private List<PreviewDrugVo> queryEvidence(PreviewContext pc, PreviewVariantVo item, String classPrefix) {
        if (!StringUtils.hasText(item.getGene())) {
            return List.of();
        }
        Map<String, Object> gene = nkb(() -> nkbEvidenceMapper.selectGeneBySymbol(item.getGene()));
        if (gene == null) {
            return List.of();
        }
        Long geneId = asLong(gene.get("geneId"));
        List<Long> geneVariantIds = StringUtils.hasText(classPrefix)
            ? nkb(() -> nkbEvidenceMapper.selectGeneVariantIdsByClass(geneId, classPrefix))
            : nkb(() -> nkbEvidenceMapper.selectGeneVariantIdsByVariant(geneId, item.getVariant(),
                item.getOriVariant()));
        if (geneVariantIds == null || geneVariantIds.isEmpty()) {
            return List.of();
        }
        List<PreviewDrugVo> evidence = nkb(() -> nkbEvidenceMapper.selectDrugAnnotations(
            geneVariantIds, pc.getDiseaseIds(), MAX_EVIDENCE));
        return evidence == null ? List.of() : evidence;
    }

    /** 胚系临床意义：历史已有 → 用它；否则从同条件最近一条继承；再否则默认 3（未知临床意义） */
    private Integer resolveSignificance(PreviewContext pc, PreviewVariantVo item) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("gene", item.getGene());
        query.put("variant", item.getVariant());
        query.put("disease", pc.getDisease());
        query.put("gender", pc.getGender());
        query.put("customer", pc.getCustomer());
        query.put("projectCode", pc.getProjectCode());
        query.put("zygosity", item.getZygosity());
        Integer inherited = interpretationMapper.selectInheritedGermlineSignificance(query);
        return inherited == null ? 3 : inherited;
    }

    private void insertHistory(PreviewContext pc, PreviewVariantVo item, String matchKey, Integer significance) {
        if (!StringUtils.hasText(pc.getProjectCode())) {
            // 项目编号缺失时不读写历史，直接用最新知识库结果（设计书 4.8）
            return;
        }
        Map<String, Object> history = new LinkedHashMap<>();
        history.put("matchKey", matchKey);
        history.put("gene", nz(item.getGene()));
        history.put("variant", nz(item.getVariant()));
        history.put("oriVariant", nz(item.getOriVariant()));
        history.put("diseaseId", pc.getDiseaseId());
        history.put("disease", pc.getDisease());
        history.put("gender", pc.getGender());
        history.put("customer", pc.getCustomer());
        history.put("projectCode", pc.getProjectCode());
        history.put("sourceType", item.getSourceType());
        history.put("mutationType", nz(item.getMutationType()));
        history.put("measurement", item.getZygosity());
        history.put("parentMutationId", item.getParentMutationId());
        history.put("matchStatus", item.getMatchStatus());
        history.put("variationClass", item.getVariationClass());
        history.put("matchResult", toJson(item.getDrugMatch()));
        history.put("sourceAnalysisId", pc.getAnalysisId());
        history.put("sourceReportId", pc.getReportId());
        history.put("sourceVariantId", item.getSourceId());
        if ("CR_ALL".equals(item.getSourceType())) {
            history.put("clinicalSignificance", significance == null ? 3 : significance);
            interpretationMapper.insertGermlineHistory(history);
        } else {
            interpretationMapper.insertSomaticHistory(history);
        }
    }

    /**
     * 匹配键：全部业务条件规范化后取 SHA-256（设计书 4.8）。
     * <p>
     * 注意：`source_analysis_id`、`source_report_id`、`source_variant_id` 只做审计，不进键。
     */
    private String matchKey(PreviewContext pc, PreviewVariantVo item) {
        String raw = String.join("|",
            nz(item.getGene()), nz(item.getVariant()), nz(item.getOriVariant()),
            pc.getDiseaseId() == null ? "" : pc.getDiseaseId().toString(), pc.getDisease(),
            pc.getGender(), pc.getCustomer(), pc.getProjectCode(),
            nz(item.getSourceType()), nz(item.getMutationType()), nz(item.getZygosity()),
            item.getParentMutationId() == null ? "" : item.getParentMutationId().toString());
        return sha256(raw);
    }

    // ---------------------------------------------------------------- 圣域个性化

    /** 圣域体细胞：癌种（或父级）属于乳腺/卵巢/前列腺癌时，按产品启用基因输出；否则空列表 */
    private List<Map<String, Object>> shengyuSomatic(PreviewContext pc, List<PreviewVariantVo> somatic) {
        if (!pipelineHas(pc.getModuleCode(), MODULE_SHENGYU_SOMATIC)) {
            return List.of();
        }
        // 条件：当前癌种或任一父级属于 乳腺癌/卵巢癌/前列腺癌
        List<String> names = pc.getDiseaseIds().isEmpty()
            ? List.of()
            : nkb(() -> nkbEvidenceMapper.selectDiseaseNames(pc.getDiseaseIds()));
        boolean required = names.stream().anyMatch(SHENGYU_REQUIRED_DISEASES::contains)
            || SHENGYU_REQUIRED_DISEASES.contains(pc.getDisease());
        return required ? filterByProductGenes(somatic, pc.getProductGenes()) : List.of();
    }

    /** 圣域胚系：始终按产品启用基因输出 */
    private List<Map<String, Object>> shengyuGermline(PreviewContext pc, List<PreviewVariantVo> germline) {
        if (!pipelineHas(pc.getModuleCode(), MODULE_SHENGYU_GERMLINE)) {
            return List.of();
        }
        return filterByProductGenes(germline, pc.getProductGenes());
    }



    // ---------------------------------------------------------------- 组装细节

    private PreviewVariantVo baseItem(Map<String, Object> row) {
        PreviewVariantVo item = new PreviewVariantVo();
        item.setSourceId(asLong(row.get("sourceId")));
        item.setSourceType(asString(row.get("sourceType")));
        item.setGene(asString(row.get("gene")));
        item.setVariant(asString(row.get("variant")));
        item.setOriVariant(asString(row.get("oriVariant")));
        item.setDrugMatch(List.of());
        return item;
    }

    private PreviewSectionVo toSection(List<PreviewVariantVo> items) {
        PreviewSectionVo section = new PreviewSectionVo();
        section.setItems(items);
        long matched = items.stream().filter(i -> "MATCHED".equals(i.getMatchStatus())).count();
        section.getSummary().put("reportedCount", items.size());
        section.getSummary().put("matchedCount", matched);
        section.getSummary().put("unmatchedCount", items.size() - matched);
        return section;
    }

    private Map<String, Object> buildReportInfo(PreviewContext pc) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("reportDate", null);
        info.put("specimentType", pc.getSpecimenType());
        info.put("disease", pc.getDisease());
        info.put("templateCode", pc.getTemplateCode());
        return info;
    }

    /** LIMS → JSON 契约的 sampleInfo（字段名对齐设计书 7.6） */
    private Map<String, Object> buildSampleInfo(InterpretationLimsVo lims) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("researchCenterName", lims.getHospitalName());
        info.put("participantNumber", lims.getPatientId());
        info.put("patientName", lims.getPatientName());
        info.put("gender", lims.getGender());
        info.put("birthday", lims.getBirthday());
        info.put("age", lims.getAge());
        info.put("disease", lims.getCancerType());
        info.put("pathologicalType", lims.getPathologicalType());
        info.put("clinicalStage", lims.getClinicalStage());
        info.put("sampleCode", lims.getBarcode());
        info.put("sampleType", lims.getSpecimenType());
        info.put("tissueCollectionDate", lims.getSampleCollectedAt());
        info.put("receivedDate", lims.getSampleReceivedAt());
        info.put("commissionedAt", lims.getCommissionedAt());
        return info;
    }













}
