package org.dromara.report.service.impl.preview;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.report.domain.ReportVariantHistory;
import org.dromara.report.domain.vo.PreviewVariantVo;
import org.dromara.report.domain.vo.ReportTemplateData;
import org.dromara.report.mapper.ReportVariantHistoryMapper;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.dromara.report.service.impl.PreviewSupport.asString;
import static org.dromara.report.service.impl.PreviewSupport.toJson;
import org.dromara.report.domain.dto.PreviewContext;
import org.dromara.report.service.impl.PreviewSupport;

/**
 * 报告位点检出历史写入：**提交审核**时把该报告「报出」的位点落库（供「历史检出」页检索）。
 * <p>
 * 只在 {@link InterpretationPreviewServiceImpl#assemble} 的 {@code persistHistory=true} 分支调用，
 * 与 history_* 的冻结同一事务：冻结失败则检出表也不写、报告状态也不变。
 * <p>
 * 幂等靠表上的 uk(tenant_id, report_id, source_type, source_id) + ON DUPLICATE KEY UPDATE：
 * 驳回后重新提交会覆盖同一批行，不产生重复。
 *
 * @author <你的名字>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportVariantHistoryWriter {

    private final ReportVariantHistoryMapper reportVariantHistoryMapper;

    /**
     * 写入检出历史（体细胞 + 胚系）。
     *
     * @param pc       预览上下文（客户 / 解读癌种）
     * @param report   报告行（reportId / analysisId / subbarcode / product / reportGeneratedAt）
     * @param data     组装结果（取解读人）
     * @param somatic  报出的体细胞位点（含证据）
     * @param germline 报出的胚系位点（含证据）
     */
    public void write(PreviewContext pc, Map<String, Object> report, ReportTemplateData data,
                      List<PreviewVariantVo> somatic, List<PreviewVariantVo> germline) {
        List<ReportVariantHistory> rows = new ArrayList<>();
        addAll(rows, pc, report, data, somatic);
        addAll(rows, pc, report, data, germline);
        if (rows.isEmpty()) {
            log.info("提交审核：该报告没有报出位点，检出历史不写：reportId={}", report.get("reportId"));
            return;
        }
        int affected = reportVariantHistoryMapper.upsertBatch(rows);
        log.info("提交审核：检出历史已写入 {} 行（影响 {} 行）：reportId={}", rows.size(), affected, report.get("reportId"));
    }

    /** 把一组位点转成行并追加 */
    private void addAll(List<ReportVariantHistory> rows, PreviewContext pc, Map<String, Object> report,
                        ReportTemplateData data, List<PreviewVariantVo> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (PreviewVariantVo item : items) {
            rows.add(toRow(pc, report, data, item));
        }
    }

    /** 单个位点 → 一行 */
    private ReportVariantHistory toRow(PreviewContext pc, Map<String, Object> report,
                                       ReportTemplateData data, PreviewVariantVo item) {
        ReportVariantHistory row = new ReportVariantHistory();
        row.setReportId(PreviewSupport.asLong(report.get("reportId")));
        row.setAnalysisId(PreviewSupport.asLong(report.get("analysisId")));
        row.setSubbarcode(asString(report.get("subbarcode")));
        row.setClient(cleanCustomer(pc.getCustomer()));
        row.setProjectNo(asString(report.get("projectNo")));
        row.setProduct(asString(report.get("product")));
        row.setDisease(pc.getDisease());
        row.setSourceType(item.getSourceType());
        row.setSourceId(item.getSourceId());
        row.setGene(item.getGene());
        row.setVariant(item.getVariant());
        row.setAbundance(abundanceOf(item));
        // 突变类型：与预览页「变异类型」列同源（VEP ExonicFunc 中文）
        row.setVariantType(item.getVariantTypeText());
        row.setOriVariant(item.getOriVariant());
        row.setLevel(levelOf(item));
        row.setDrugInfo(drugInfoOf(item.getDrugGroups()));
        row.setAnalyzer(analyzerOf(data));
        row.setReportGeneratedAt(asDate(report.get("reportGeneratedAt")));
        row.setCreateBy(LoginHelper.getUserId());
        return row;
    }

    /**
     * 位点等级：体细胞取 variationClass 去「类」字（I / II / III）；胚系取临床意义标签。
     * 无证据（NOT_MATCHED）时为空 —— 位点照写，等级留空。
     */
    private String levelOf(PreviewVariantVo item) {
        if ("CR_ALL".equals(item.getSourceType())) {
            return item.getClinicalSignificanceLabel();
        }
        String variationClass = item.getVariationClass();
        return variationClass == null ? null : variationClass.replace("类", "");
    }

    /**
     * 丰度：
     * <ul>
     *   <li>体细胞 / CNV / 融合：预览页「丰度/reads」列同源展示值（DNA → 百分比；RNA 融合 → reads 数；扩增缺失 → 拷贝数）</li>
     *   <li>胚系（CR_ALL）：没有百分比丰度，取合子状态 zygosity（het / hom）——
     *       与冻结侧同一口径（{@code VariantMatcher} 把 zygosity 写进 history_germline.measurement），
     *       与检出表 genotype 语义一致</li>
     * </ul>
     */
    private String abundanceOf(PreviewVariantVo item) {
        return "CR_ALL".equals(item.getSourceType()) ? item.getZygosity() : item.getAbundanceText();
    }

    /** 原始用药信息：en7 drugGroups 原样序列化（解析放读取侧，写入不做有损转换）；空则不写 */
    private String drugInfoOf(Map<String, String> drugGroups) {
        return drugGroups == null || drugGroups.isEmpty() ? null : toJson(drugGroups);
    }

    /** 解读人：与生成 JSON 同源（报告生成参数里的解读人） */
    private String analyzerOf(ReportTemplateData data) {
        return data.getReportInfo() == null ? null : data.getReportInfo().getAnalyzer();
    }

    /** 客户：LIMS 医院名；缺失时的「分析粒度占位符」不写进业务表 */
    private String cleanCustomer(String customer) {
        boolean missing = customer == null || customer.isBlank() || customer.startsWith("UNKNOWN_CUSTOMER@");
        return missing ? null : customer;
    }

    /**
     * 报告时间：报告行是 {@code selectReportRow} 出来的 Map，datetime 列经 MySQL Connector/J 的
     * {@code getObject()} 回来是 {@link LocalDateTime}（不是 java.util.Date），所以两种都接。
     */
    private Date asDate(Object value) {
        Date result = null;
        if (value instanceof Date date) {
            result = date;
        } else if (value instanceof LocalDateTime dateTime) {
            result = Timestamp.valueOf(dateTime);
        } else if (value != null) {
            log.warn("报告时间字段类型未预期，检出历史该列写空：{}", value.getClass().getName());
        }
        return result;
    }
}
