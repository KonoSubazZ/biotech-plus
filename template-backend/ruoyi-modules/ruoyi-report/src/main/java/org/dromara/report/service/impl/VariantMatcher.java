package org.dromara.report.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.report.domain.vo.PreviewDrugVo;
import org.dromara.report.domain.vo.PreviewVariantVo;
import org.dromara.report.mapper.InterpretationMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.dromara.report.service.impl.PreviewSupport.asInt;
import static org.dromara.report.service.impl.PreviewSupport.asLong;
import static org.dromara.report.service.impl.PreviewSupport.asString;
import static org.dromara.report.service.impl.PreviewSupport.mutationTypeCode;
import static org.dromara.report.service.impl.PreviewSupport.nz;
import static org.dromara.report.service.impl.PreviewSupport.parseMatchResult;
import static org.dromara.report.service.impl.PreviewSupport.sha256;
import static org.dromara.report.service.impl.PreviewSupport.toJson;

/**
 * 位点匹配：把「报出」的体细胞/胚系位点匹配到 NKB 药物证据，并维护匹配历史。
 * <p>
 * 与设计书对齐的关键口径：
 * <ul>
 *   <li>只处理「报出（is_reported=1）」的位点；体细胞 = SNP/Indel + CNV + Fusion，胚系 = CR_ALL。</li>
 *   <li>匹配历史：先按 match_key（v2 = 产品项目 + 癌种 + 性别 + 位点 + 人工改靶父级 的 SHA-256）
 *       查 history_somatic / history_germline。<b>命中即严格只读复用</b>冻结的 match_result
 *       （NKB 更新不刷新），并登记一次复用（last_reused_at / reuse_count + history_reuse_log 留痕）；
 *       未命中才查 NKB 并用 INSERT IGNORE 写首条。</li>
 *   <li>胚系五级临床意义 1~5（默认 3）：只影响该列，不覆盖冻结结果；改靶产生新键时从同产品/癌种/性别/位点
 *       <b>最近一条</b>继承（故意忽略 parent_mutation_id）。</li>
 *   <li>确定性：同输入产生相同结果，不写时间戳/随机值。</li>
 *   <li>项目编号缺失时不读写历史，直接用最新知识库结果（设计书 §4.8）。</li>
 * </ul>
 *
 * @author <你的名字>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VariantMatcher {

    /** 五级临床意义标签（1~5） */
    private static final Map<Integer, String> SIGNIFICANCE_LABEL = Map.of(
        1, "致病", 2, "可能致病", 3, "未知临床意义", 4, "可能良性", 5, "良性");

    /** 复用键版本：v2 = 按「产品项目 + 位点 + 癌种 + 性别 + 人工改靶父级」冻结 */
    private static final String MATCH_KEY_VERSION = "v2";

    private final InterpretationMapper interpretationMapper;

    private final NkbDrugMatcher drugMatcher;

    private final VariantDescriptionEnricher descriptionEnricher;

    /**
     * 匹配体细胞位点（CNV / Fusion / 小变异，按 en7 口径分级）。
     *
     * @param pc 预览上下文
     * @return 位点列表（已按展示顺序排序）
     */
    public List<PreviewVariantVo> matchSomatic(PreviewContext pc) {
        List<PreviewVariantVo> items = new ArrayList<>();
        for (Map<String, Object> row : interpretationMapper.selectReportedSomaticVariants(pc.getAnalysisId())) {
            PreviewVariantVo item = baseItem(row);
            item.setMutationType(mutationTypeCode(item.getSourceType(), asString(row.get("mutationTypeRaw"))));
            item.setFrequency(asString(row.get("frequency")));
            item.setDepth(asString(row.get("depth")));
            item.setParentMutationIds(ParentMutationIds.parse(row.get("parentMutationId")));
            matchWithHistory(pc, item, null);
            PreviewSupport.decorate(row, item, false);
            descriptionEnricher.enrich(item, row.get("freqRaw"));
            items.add(item);
        }
        return PreviewSupport.sortByOrderNum(items);
    }

    /**
     * 匹配胚系位点（CR_ALL），带五级临床意义。
     *
     * @param pc 预览上下文
     * @return 位点列表（已按展示顺序排序）
     */
    public List<PreviewVariantVo> matchGermline(PreviewContext pc) {
        List<PreviewVariantVo> items = new ArrayList<>();
        for (Map<String, Object> row : interpretationMapper.selectReportedGermlineVariants(pc.getAnalysisId())) {
            PreviewVariantVo item = baseItem(row);
            item.setMutationType("G");
            item.setZygosity(asString(row.get("zygosity")));
            item.setDepth(asString(row.get("depth")));
            item.setSourceClnsig(asString(row.get("clnsig")));
            item.setParentMutationIds(ParentMutationIds.parse(row.get("parentMutationId")));
            item.setClassificationLovd(null);
            Integer significance = resolveSignificance(pc, item);
            item.setClinicalSignificance(significance);
            item.setClinicalSignificanceLabel(SIGNIFICANCE_LABEL.get(significance));
            matchWithHistory(pc, item, significance);
            PreviewSupport.decorate(row, item, true);
            descriptionEnricher.enrich(item, row.get("freqRaw"));
            items.add(item);
        }
        return PreviewSupport.sortByOrderNum(items);
    }

    /**
     * 历史复用 or 首次知识库匹配。
     * <p>
     * 命中历史 → <b>严格只读复用</b>（知识库更新不刷新），并登记一次复用（时间 + 计数 + 留痕）。
     * 未命中 → 按 en7 口径查最新知识库，{@code INSERT IGNORE} 冻结首条；此后不再被任何路径覆盖。
     *
     * @param item         位点（已填好 gene/variant/oriVariant/mutationType）
     * @param significance 胚系临床意义（体细胞传 null）
     */
    private void matchWithHistory(PreviewContext pc, PreviewVariantVo item, Integer significance) {
        String matchKey = matchKey(pc, item);
        boolean germline = "CR_ALL".equals(item.getSourceType());
        Map<String, Object> history = germline
            ? interpretationMapper.selectGermlineHistory(matchKey)
            : interpretationMapper.selectSomaticHistory(matchKey);
        NkbDrugMatcher.MatchResult frozen = history == null ? null
            : parseMatchResult(asString(history.get("match_result")));
        if (history != null && frozen != null) {
            // 命中历史：复用冻结结果（知识库更新不刷新历史）
            item.setFromHistory(true);
            applyMatchResult(item, frozen);
            item.setMatchStatus(asString(history.get("match_status")));
            item.setVariationClass(asString(history.get("variation_class")));
            if (germline && history.get("clinical_significance") != null) {
                Integer clinical = asInt(history.get("clinical_significance"));
                item.setClinicalSignificance(clinical);
                item.setClinicalSignificanceLabel(SIGNIFICANCE_LABEL.get(clinical));
            }
            registerHistoryReuse(history, pc, item, germline, matchKey);
            return;
        }
        if (history != null) {
            // v2 行不可能解析失败；真出现也只告警 + 本次按最新知识库输出，
            // **不覆盖**已冻结行（宁可不一致也不改写历史）
            log.warn("匹配历史 match_result 无法解析，本次按最新知识库输出且不覆盖历史：matchKey={}", matchKey);
        }

        // 首次匹配：走 en7 口径（节点四级兜底 + 自身/父级节点 + 人工改靶父级 + 癌种范围 + 证据过滤 + 分级）
        NkbDrugMatcher.MatchResult matched = drugMatcher.match(item.getGene(), item.getVariant(), item.getOriVariant(),
            germline, pc.getDiseaseScope(), item.getParentMutationIds());
        applyMatchResult(item, matched);
        insertHistory(pc, item, matchKey, significance);
    }

    /**
     * 登记一次历史复用：{@code last_reused_at} / {@code reuse_count}（表上）+ 一条 append-only 留痕
     * （能回答「哪份报告 / 谁 / 何时用了这条历史」）。
     *
     * @param history  命中的历史行（含 id）
     * @param matchKey 本次匹配键（与历史行同值）
     * @param germline 是否胚系
     */
    private void registerHistoryReuse(Map<String, Object> history, PreviewContext pc, PreviewVariantVo item,
                                      boolean germline, String matchKey) {
        interpretationMapper.touchHistoryReuse(matchKey, germline);
        Map<String, Object> reuse = new LinkedHashMap<>();
        reuse.put("historyType", germline ? "GERMLINE" : "SOMATIC");
        reuse.put("historyId", asLong(history.get("id")));
        reuse.put("matchKey", matchKey);
        reuse.put("gene", nz(item.getGene()));
        reuse.put("variant", nz(item.getVariant()));
        reuse.put("projectCode", nz(pc.getProjectCode()));
        reuse.put("reportId", pc.getReportId());
        reuse.put("analysisId", pc.getAnalysisId());
        reuse.put("sourceVariantId", item.getSourceId());
        reuse.put("reusedBy", LoginHelper.getUserId());
        interpretationMapper.insertReuseLog(reuse);
    }

    /** 冻结结构（与 {@link #insertHistory} 写入的 match_result 保持一致） */
    private Map<String, Object> frozenResult(PreviewVariantVo item) {
        Map<String, Object> frozen = new LinkedHashMap<>();
        frozen.put("inNkb", item.getInNkb());
        frozen.put("matchedNode", item.getMatchedNode());
        frozen.put("mutationId", item.getMatchedMutationId());
        frozen.put("effectText", item.getEffectText());
        frozen.put("variationClass", item.getVariationClass());
        frozen.put("description", item.getDescription());
        frozen.put("evidence", item.getDrugMatch());
        frozen.put("drugGroups", item.getDrugGroups());
        frozen.put("drugAuditList", item.getDrugAuditList());
        return frozen;
    }

    /** 把匹配结果落到位点行（首次匹配与历史复用共用的字段集合） */
    private void applyMatchResult(PreviewVariantVo item, NkbDrugMatcher.MatchResult matched) {
        item.setInNkb(Boolean.TRUE.equals(matched.getInNkb()));
        item.setMatchedNode(matched.getMatchedNode());
        item.setMatchedMutationId(matched.getMutationId());
        item.setEffectText(matched.getEffectText());
        List<PreviewDrugVo> evidence = matched.getEvidence() == null ? List.of() : matched.getEvidence();
        item.setDrugMatch(evidence);
        item.setDrugGroups(matched.getDrugGroups());
        item.setDrugAuditList(matched.getDrugAuditList());
        item.setMatchStatus(evidence.isEmpty() ? "NOT_MATCHED" : "MATCHED");
        item.setVariationClass(matched.getVariationClass());
        if (StringUtils.hasText(matched.getDescription())) {
            item.setDescription(matched.getDescription());
        }
    }

    /**
     * 胚系临床意义：历史已有 → 用它；否则从同条件最近一条继承；再否则默认 3（未知临床意义）。
     * <p>
     * 条件与匹配键对齐（产品 + 癌种 + 性别 + 位点）——<b>故意忽略 parent_mutation_id</b>
     * （改靶继承，设计书 §4.8 第 4 条），也忽略 customer / 合子（它们已不在匹配键里，
     * 带上会导致跨医院继承不到）。
     */
    private Integer resolveSignificance(PreviewContext pc, PreviewVariantVo item) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("gene", item.getGene());
        query.put("variant", item.getVariant());
        query.put("disease", pc.getDisease());
        query.put("gender", pc.getGender());
        query.put("projectCode", pc.getProjectCode());
        Integer inherited = interpretationMapper.selectInheritedGermlineSignificance(query);
        return inherited == null ? 3 : inherited;
    }

    /**
     * 首次匹配后冻结一条历史（INSERT IGNORE：同键只写一次）。
     *
     * @param matchKey     匹配键
     * @param significance 胚系临床意义（体细胞传 null；胚系为空时按 3 存）
     */
    private void insertHistory(PreviewContext pc, PreviewVariantVo item, String matchKey, Integer significance) {
        if (!StringUtils.hasText(pc.getProjectCode())) {
            // 项目编号缺失时不读写历史，直接用最新知识库结果（设计书 §4.8）
            return;
        }
        Map<String, Object> history = new LinkedHashMap<>();
        history.put("matchKey", matchKey);
        history.put("keyVersion", 2);
        history.put("gene", nz(item.getGene()));
        history.put("variant", nz(item.getVariant()));
        history.put("oriVariant", nz(item.getOriVariant()));
        history.put("diseaseId", pc.getDiseaseId());
        history.put("disease", pc.getDisease());
        history.put("gender", pc.getGender());
        history.put("customer", pc.getCustomer());
        history.put("projectCode", pc.getProjectCode());
        // 以下三列 + customer 已不进匹配键（v2 按产品项目收敛），但列保留并继续填充，供查看/审计
        history.put("sourceType", item.getSourceType());
        history.put("mutationType", nz(item.getMutationType()));
        history.put("measurement", item.getZygosity());
        // 人工父级按「去重升序逗号串」入库（与匹配键同口径），未改靶存 null
        String parentIdsText = ParentMutationIds.toKey(item.getParentMutationIds());
        history.put("parentMutationId", parentIdsText.isEmpty() ? null : parentIdsText);
        history.put("matchStatus", item.getMatchStatus());
        history.put("variationClass", item.getVariationClass());
        history.put("matchResult", toJson(frozenResult(item)));
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
     * 匹配键：v2 口径 = {@code sha256(v2|产品项目|癌种|性别|基因|位点|原始位点|人工改靶父级)}。
     * <p>
     * 冻结语义：同一键只写一次（INSERT IGNORE），此后一律复用，NKB 更新不刷新。
     * <p>
     * 不进键的维度（<b>列仍保留并继续填充，仅供查看/审计</b>）：
     * {@code customer}（医院）—— 复用边界是产品项目，不按医院；
     * {@code source_type} / {@code mutation_type} / {@code zygosity} —— 位点短名的派生或次要维度；
     * {@code source_analysis_id} / {@code source_report_id} / {@code source_variant_id} 只做审计。
     * <p>
     * 癌种用 NKB {@code disease_id}（抗改名）；{@code disease_id} 为空时才退回癌种中文名。
     */
    String matchKey(PreviewContext pc, PreviewVariantVo item) {
        String diseaseKey = pc.getDiseaseId() == null
            ? "name:" + nz(pc.getDisease())
            : "id:" + pc.getDiseaseId();
        String raw = String.join("|",
            MATCH_KEY_VERSION, nz(pc.getProjectCode()), diseaseKey, nz(pc.getGender()),
            nz(item.getGene()), nz(item.getVariant()), nz(item.getOriVariant()),
            ParentMutationIds.toKey(item.getParentMutationIds()));
        return sha256(raw);
    }

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
}
