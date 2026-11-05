package org.dromara.report.service.impl.preview;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.report.domain.vo.PreviewDrugVo;
import org.dromara.report.domain.vo.PreviewVariantVo;
import org.dromara.report.mapper.InterpretationMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.dromara.report.service.impl.PreviewSupport.asInt;
import static org.dromara.report.service.impl.PreviewSupport.asLong;
import static org.dromara.report.service.impl.PreviewSupport.asString;
import static org.dromara.report.service.impl.PreviewSupport.mutationTypeCode;
import static org.dromara.report.service.impl.PreviewSupport.nz;
import static org.dromara.report.service.impl.PreviewSupport.parseMatchResult;
import static org.dromara.report.service.impl.PreviewSupport.sha256;
import static org.dromara.report.service.impl.PreviewSupport.toJson;
import org.dromara.report.domain.dto.PreviewContext;
import org.dromara.report.service.impl.NkbDrugMatcher;
import org.dromara.report.service.impl.PreviewSupport;

/**
 * 位点匹配：把「报出」的体细胞/胚系位点匹配到 NKB 药物证据，并维护匹配历史。
 * <p>
 * 与设计书对齐的关键口径：
 * <ul>
 *   <li>只处理「报出（is_reported=1）」的位点；体细胞 = SNP/Indel + CNV + Fusion，胚系 = CR_ALL。</li>
 *   <li><b>落库时机</b>：只有报告审核通过才把结果冻结进 history_*（{@code persistHistory=true}）；
 *       页面预览与生成都是只读模式 —— 命中历史照常复用，但不写新行、不登记复用次数。
 *       所以「未审核的报告不占用匹配历史」，审核那一刻才冻结。</li>
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

    /**
     * 五级临床意义的中文标签（供报告级覆盖表复用，避免两处各存一份）。
     *
     * @param value 1~5
     * @return 中文标签；未知返回 null
     */
    public static String significanceLabel(Integer value) {
        return value == null ? null : SIGNIFICANCE_LABEL.get(value);
    }

    /** 复用键版本：v2 = 按「产品项目 + 位点 + 癌种 + 性别 + 人工改靶父级」冻结 */
    private static final String MATCH_KEY_VERSION = "v4";

    /** 落库的 key_version 数字（由 MATCH_KEY_VERSION 推出，别再手写常量） */
    private static final int MATCH_KEY_VERSION_NO = Integer.parseInt(MATCH_KEY_VERSION.substring(1));

    private final InterpretationMapper interpretationMapper;

    private final NkbDrugMatcher drugMatcher;

    private final VariantDescriptionEnricher descriptionEnricher;

    /**
     * 匹配体细胞位点（CNV / Fusion / 小变异，按 en7 口径分级）。
     *
     * @param pc             预览上下文
     * @param persistHistory 是否把本次匹配结果冻结进 history_somatic（只有审核通过 freezeHistory 才落库；预览/生成/提交传 false）
     * @return 位点列表（已按展示顺序排序）
     */
    public List<PreviewVariantVo> matchSomatic(PreviewContext pc, boolean persistHistory) {
        List<PreviewVariantVo> items = new ArrayList<>();
        for (Map<String, Object> row : interpretationMapper.selectReportedSomaticVariants(pc.getAnalysisId())) {
            PreviewVariantVo item = baseItem(row);
            item.setMutationType(mutationTypeCode(item.getSourceType(), asString(row.get("mutationTypeRaw"))));
            item.setFrequency(asString(row.get("frequency")));
            item.setDepth(asString(row.get("depth")));
            item.setParentMutationIds(ParentMutationIds.parse(row.get("parentMutationId")));
            applyEditTarget(pc, item);
            matchWithHistory(pc, item, null, persistHistory);
            PreviewSupport.decorate(row, item, false);
            descriptionEnricher.enrich(item, row.get("freqRaw"));
            items.add(item);
        }
        return PreviewSupport.sortByOrderNum(items);
    }

    /**
     * 匹配胚系位点（CR_ALL），带五级临床意义。
     *
     * @param pc             预览上下文
     * @param persistHistory 是否把本次匹配结果冻结进 history_germline（只有审核通过 freezeHistory 才落库）
     * @return 位点列表（已按展示顺序排序）
     */
    public List<PreviewVariantVo> matchGermline(PreviewContext pc, boolean persistHistory) {
        List<PreviewVariantVo> items = new ArrayList<>();
        for (Map<String, Object> row : interpretationMapper.selectReportedGermlineVariants(pc.getAnalysisId())) {
            PreviewVariantVo item = baseItem(row);
            item.setMutationType("G");
            item.setZygosity(asString(row.get("zygosity")));
            item.setDepth(asString(row.get("depth")));
            item.setSourceClnsig(asString(row.get("clnsig")));
            item.setParentMutationIds(ParentMutationIds.parse(row.get("parentMutationId")));
            applyEditTarget(pc, item);
            item.setClassificationLovd(null);
            Integer significance = resolveSignificance(pc, item);
            // 报告级人工修改（未入库位点才允许）：优先于继承/默认值；审核通过冻结时按这个值写 history_germline
            ReportVariantEdits.Edit edit = ReportVariantEdits.find(pc.getVariantEdits(), item);
            if (edit != null && edit.getClinicalSignificance() != null) {
                significance = edit.getClinicalSignificance();
            }
            item.setClinicalSignificance(significance);
            item.setClinicalSignificanceLabel(significanceLabel(significance));
            matchWithHistory(pc, item, significance, persistHistory);
            PreviewSupport.decorate(row, item, true);
            descriptionEnricher.enrich(item, row.get("freqRaw"));
            items.add(item);
        }
        return PreviewSupport.sortByOrderNum(items);
    }

    /** 只用于 B1 比对（把 frozenResult 与历史 JSON 归一成 Map） */
    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * 把报告级人工修改里的父靶点套到位点（只影响本次匹配；不写源位点表）。
     */
    private void applyEditTarget(PreviewContext pc, PreviewVariantVo item) {
        ReportVariantEdits.Edit edit = ReportVariantEdits.find(pc.getVariantEdits(), item);
        if (edit != null && edit.getParentMutationIds() != null) {
            item.setParentMutationIds(edit.getParentMutationIds());
        }
    }

    /**
     * 从「已生成的报告 JSON」冻结历史：审核通过时唯一入口。
     * <p>
     * 不再查 NKB / 不再用实时样本；match_key 的上下文由调用方用报告快照构建。
     * <ul>
     *   <li>history 未命中 → INSERT IGNORE 首条（match_result 从 JSON item 还原）</li>
     *   <li>history 命中 → B1 比对：结果一致才登记复用；不一致直接拒绝审核通过</li>
     *   <li>同一份报告内相同 match_key 只处理一次（避免自己复用自己）</li>
     * </ul>
     *
     * @param pc       冻结上下文（报告快照构建）
     * @param report   报告行（sourceReportId/analysisId 等审计字段）
     * @param items    制品 JSON 里的位点
     * @param germline 是否胚系
     */
    public void freezeFromItems(PreviewContext pc, Map<String, Object> report,
                                List<PreviewVariantVo> items, boolean germline) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (PreviewVariantVo item : items) {
            String matchKey = matchKey(pc, item);
            if (!seen.add(matchKey)) {
                log.warn("同一报告内重复 match_key，跳过后续冻结：reportId={}，key={}，gene={}，variant={}",
                    pc.getReportId(), matchKey, item.getGene(), item.getVariant());
                continue;
            }
            Map<String, Object> history = germline
                ? interpretationMapper.selectGermlineHistory(matchKey)
                : interpretationMapper.selectSomaticHistory(matchKey);
            if (history == null) {
                insertHistory(pc, item, matchKey, germline ? item.getClinicalSignificance() : null);
                continue;
            }
            requireSameFrozen(history, item, germline, matchKey);
            registerHistoryReuse(history, pc, item, germline, matchKey);
        }
    }

    /**
     * B1：历史已存在时，制品里的匹配结果必须与历史一致，否则阻止审核通过。
     */
    private void requireSameFrozen(Map<String, Object> history, PreviewVariantVo item,
                                   boolean germline, String matchKey) {
        Map<?, ?> expected = JSON.convertValue(frozenResult(item), Map.class);
        Map<?, ?> actual = parseMap(asString(history.get("match_result")));
        StoredFrozen stored = new StoredFrozen(history);
        if (!Objects.equals(expected, actual)
            || !Objects.equals(nz(item.getMatchStatus()), nz(stored.matchStatus))
            || !Objects.equals(nz(item.getVariationClass()), nz(stored.variationClass))
            || (germline && !Objects.equals(clinicalValue(item.getClinicalSignificance()),
                clinicalValue(asInt(stored.clinicalSignificance))))) {
            throw new ServiceException("位点「" + item.getGene() + "_" + item.getVariant()
                + "」已被其他报告冻结且结果不一致（历史=" + stored.summary() + "，本报告="
                + summary(item) + "），请驳回后重新生成再审核");
        }
        log.info("B1 校验通过：match_key={}，reportId={}", matchKey, item.getSourceId());
    }

    private Map<?, ?> parseMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return JSON.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static Integer clinicalValue(Integer value) {
        return value == null ? 3 : value;
    }

    private static String summary(PreviewVariantVo item) {
        return nz(item.getMatchStatus()) + "/" + nz(item.getVariationClass())
            + "/sig=" + clinicalValue(item.getClinicalSignificance());
    }

    /** 历史行里 B1 需要比对的列 */
    private static final class StoredFrozen {

        private final String matchStatus;

        private final String variationClass;

        private final Object clinicalSignificance;

        private StoredFrozen(Map<String, Object> history) {
            this.matchStatus = asString(history.get("match_status"));
            this.variationClass = asString(history.get("variation_class"));
            this.clinicalSignificance = history.get("clinical_significance");
        }

        private String summary() {
            return nz(matchStatus) + "/" + nz(variationClass) + "/sig=" + clinicalValue(asInt(clinicalSignificance));
        }
    }


    /**
     * 历史复用 or 首次知识库匹配（预览/生成用；审核通过不再走这里，见 {@link #freezeFromItems}）。
     * <p>
     * 命中历史 → <b>严格只读复用</b>（知识库更新不刷新），并登记一次复用（时间 + 计数 + 留痕）。
     * 未命中 → 按 en7 口径查最新知识库，{@code INSERT IGNORE} 冻结首条；此后不再被任何路径覆盖。
     *
     * @param item           位点（已填好 gene/variant/oriVariant/mutationType）
     * @param significance   胚系临床意义（体细胞传 null）
     * @param persistHistory 是否落库：false = 只读（命中历史照常复用，但不写新行、不登记复用）
     */
    private void matchWithHistory(PreviewContext pc, PreviewVariantVo item, Integer significance,
                                  boolean persistHistory) {
        String matchKey = matchKey(pc, item);
        boolean germline = "CR_ALL".equals(item.getSourceType());
        Map<String, Object> history = germline
            ? interpretationMapper.selectGermlineHistory(matchKey)
            : interpretationMapper.selectSomaticHistory(matchKey);
        NkbDrugMatcher.MatchResult frozen = history == null ? null
            : parseMatchResult(asString(history.get("match_result")));
        if (history != null && frozen != null) {
            reuseFrozenHistory(history, frozen, pc, item, persistHistory);
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
        if (persistHistory) {
            // 只有审核通过（freezeHistory）才冻结首条历史；预览/生成/提交审核都不写，避免未通过审核的报告占用匹配历史
            insertHistory(pc, item, matchKey, significance);
        }
    }

    /**
     * 命中历史：把冻结结果套到位点行（知识库更新不刷新历史），并按模式决定要不要登记复用。
     *
     * @param history        命中的历史行（含 match_status / variation_class / 胚系临床意义）
     * @param frozen         解析出来的冻结匹配结果
     * @param persistHistory 是否登记复用：false = 只读模式（预览/生成），复用次数与留痕都不写
     */
    private void reuseFrozenHistory(Map<String, Object> history, NkbDrugMatcher.MatchResult frozen,
                                    PreviewContext pc, PreviewVariantVo item, boolean persistHistory) {
        boolean germline = "CR_ALL".equals(item.getSourceType());
        item.setFromHistory(true);
        applyMatchResult(item, frozen);
        item.setMatchStatus(asString(history.get("match_status")));
        item.setVariationClass(asString(history.get("variation_class")));
        if (germline && history.get("clinical_significance") != null) {
            Integer clinical = asInt(history.get("clinical_significance"));
            item.setClinicalSignificance(clinical);
            item.setClinicalSignificanceLabel(SIGNIFICANCE_LABEL.get(clinical));
        }
        if (!persistHistory) {
            // 只读模式（预览/生成）：结果照常复用，但不登记复用次数与留痕 —— 统计口径只认审核后的使用
            return;
        }
        // 匹配键由「上下文 + 位点」确定性推出，与调用方算出来的同值，这里重算以免多传一个参数
        registerHistoryReuse(history, pc, item, germline, matchKey(pc, item));
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
     * 条件与匹配键对齐（项目编号 + 产品 + 癌种 + 性别 + 位点）——<b>故意忽略 parent_mutation_id</b>
     * （改靶继承，设计书 §4.8 第 4 条），也忽略 customer / 合子（它们已不在匹配键里，
     * 带上会导致跨医院继承不到）。
     */
    private Integer resolveSignificance(PreviewContext pc, PreviewVariantVo item) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("gene", item.getGene());
        query.put("variant", item.getVariant());
        query.put("disease", pc.getDisease());
        query.put("gender", pc.getGender());
        query.put("projectNo", pc.getProjectNo());
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
        history.put("keyVersion", MATCH_KEY_VERSION_NO);
        history.put("gene", nz(item.getGene()));
        history.put("variant", nz(item.getVariant()));
        history.put("oriVariant", nz(item.getOriVariant()));
        history.put("diseaseId", pc.getDiseaseId());
        history.put("disease", pc.getDisease());
        history.put("gender", pc.getGender());
        history.put("customer", pc.getCustomer());
        history.put("projectNo", pc.getProjectNo());
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
     * 匹配键：v4 口径 = {@code sha256(v4|项目编号|产品项目|癌种|性别|基因|位点|原始位点)}。
     * <p>
     * v3（2026-10-22）：在 v2「产品项目」基础上加 project_no —— 匹配到历史位点必须
     * {@code gene + variant + disease + gender + project_no + product_bio} 全一致，避免跨项目复用。
     * <p>
     * v4（2026-10-22 用户定，去掉改靶父级）：
     * 历史本身就是「位点 → 已匹配结果」的缓存 —— 自动匹配知识库的位点、人工改靶过的位点都会留下历史。
     * 匹配顺序永远是 <b>先查历史，命中即复用；命中不到才走知识库匹配与用药逻辑</b>。
     * 所以复用键只回答「是不是同一个位点」，而改靶父级属于「匹配结果 / 人工干预」这一侧
     * （随历史一起冻结、也参与首次匹配的用药计算），<b>不是位点身份</b>，因此不进键；
     * 否则同一位置仅因父级不同就命中不上历史，缓存形同虚设。
     * <p>
     * 冻结语义：同一键只写一次（INSERT IGNORE），此后一律复用，NKB 更新不刷新。
     * <p>
     * 不进键的维度（<b>列仍保留并继续填充，仅供查看/审计</b>）：
     * {@code customer}（医院）—— 复用边界是产品项目，不按医院；
     * {@code source_type} / {@code mutation_type} / {@code zygosity} —— 位点短名的派生或次要维度；
     * {@code parent_mutation_id}（人工改靶）—— 属匹配结果侧，不是位点身份，v4 起不进复用键；
     * 列仍填充、仍参与首次实时匹配的用药计算；
     * {@code source_analysis_id} / {@code source_report_id} / {@code source_variant_id} 只做审计。
     * <p>
     * 癌种用 NKB {@code disease_id}（抗改名）；{@code disease_id} 为空时才退回癌种中文名。
     */
    String matchKey(PreviewContext pc, PreviewVariantVo item) {
        String diseaseKey = pc.getDiseaseId() == null
            ? "name:" + nz(pc.getDisease())
            : "id:" + pc.getDiseaseId();
        String raw = String.join("|",
            MATCH_KEY_VERSION, nz(pc.getProjectNo()), nz(pc.getProjectCode()), diseaseKey, nz(pc.getGender()),
            nz(item.getGene()), nz(item.getVariant()), nz(item.getOriVariant()));
        return sha256(raw);
    }

    private PreviewVariantVo baseItem(Map<String, Object> row) {
        PreviewVariantVo item = new PreviewVariantVo();
        item.setSourceId(asLong(row.get("sourceId")));
        item.setSourceType(asString(row.get("sourceType")));
        item.setGene(asString(row.get("gene")));
        item.setVariant(asString(row.get("variant")));
        item.setOriVariant(asString(row.get("oriVariant")));
        item.setFromHistory(false);
        item.setDrugMatch(List.of());
        return item;
    }
}
