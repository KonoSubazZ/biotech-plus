package org.dromara.report.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.dromara.report.domain.dto.ReportModuleContext;
import org.dromara.report.domain.vo.PreviewSectionVo;
import org.dromara.report.domain.vo.PreviewVariantVo;
import org.dromara.report.domain.vo.ReportTemplateData;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 公共字段组装器（设计书 §0.1 / §7.2）。
 * <p>
 * 无条件写入「所有报告都成立」的部分：报告信息、样本信息、体细胞/胚系完整预览、样本类型开关与告警。
 * 模板专属字段一律不在这里出现 —— 它们由 {@code service/template} 下的 Handler 挂到顶层动态字段。
 * <p>
 * 取值口径：缺值保持 null；只有明确要展示的位（如圣域补空行）才由 Handler 补 "/"。
 *
 * @author <你的名字>
 */
@Slf4j
@Component
public class CommonReportModuleAssembler {

    /** 个性化列表里「没有该基因位点」时的占位符 */
    public static final String MISSING_VALUE = "/";

    /** 组织样本产品（如 novopm2_tis_1238） */
    private static final Pattern TISSUE_PRODUCT = Pattern.compile("(?i).*_tis_?.*");

    /** 仅血液样本产品（如 novopm2_blo_1238） */
    private static final Pattern BLOOD_PRODUCT = Pattern.compile("(?i).*_blo_?.*");

    /** 出生日期里取 4 位年份 */
    private static final Pattern FOUR_DIGIT_YEAR = Pattern.compile("(\\d{4})");

    /**
     * 写公共字段。
     *
     * @param context 模块上下文
     * @param target  目标 JSON
     */
    public void apply(ReportModuleContext context, ReportTemplateData target) {
        target.setReportInfo(reportInfo(context));
        target.setSampleInfo(sampleInfo(context));

        Map<String, Object> report = context.getReport();
        target.setShowTissueBloodSample(isTissueSample(productOf(report)));
        target.setShowBloodOnlySample(isBloodOnlySample(productOf(report), false));

        target.setSomaticVariants(section(context, context.getSomaticVariants()));
        target.setGermlineVariants(section(context, context.getGermlineVariants()));
        target.setWarnings(context.getWarnings());
    }

    /** 报告基础信息：报告日期取分析日期（同输入同 JSON，不取当前时间）；标本类型 tissue / blood */
    public ReportTemplateData.ReportInfo reportInfo(ReportModuleContext context) {
        ReportTemplateData.ReportInfo info = new ReportTemplateData.ReportInfo();
        info.setReportDate(reportDate(context.getReport()));
        info.setSpecimentType(englishSpecimenType(specimenType(context)));
        return info;
    }

    /** LIMS → JSON 契约的 sampleInfo（字段名与设计书 §7.6 逐字一致） */
    public ReportTemplateData.SampleInfo sampleInfo(ReportModuleContext context) {
        ReportTemplateData.SampleInfo info = new ReportTemplateData.SampleInfo();
        info.setResearchCenterName(valueOf(context.getLims(), l -> l.getHospitalName()));
        info.setParticipantNumber(valueOf(context.getLims(), l -> l.getPatientId()));
        info.setGender(genderLabel(valueOf(context.getLims(), l -> l.getGender())));
        info.setBirthYear(birthYear(valueOf(context.getLims(), l -> l.getBirthday())));
        info.setDisease(context.getDisease());
        info.setSampleCode(valueOf(context.getLims(), l -> l.getBarcode()));
        info.setSampleType(sampleTypeLabel(context));
        info.setTissueCollectionDate(valueOf(context.getLims(), l -> l.getSampleCollectedAt()));
        info.setReceivedDate(valueOf(context.getLims(), l -> l.getSampleReceivedAt()));
        // visitCycle / sectionDate / bloodCollectionDate：本仓 LIMS 无对应字段，固定 null（不编造）
        return info;
    }

    /** 位点分节：summary 统计 + 明细 + 癌种上下文 */
    public PreviewSectionVo section(ReportModuleContext context, List<PreviewVariantVo> items) {
        List<PreviewVariantVo> safeItems = items == null ? List.of() : items;
        PreviewSectionVo section = new PreviewSectionVo();
        section.setItems(safeItems);
        section.setAnalysisId(context.getAnalysisId());
        section.setReportId(context.getReportId());
        section.setDiseaseId(context.getDiseaseId());
        section.setDiseaseName(context.getDisease());
        section.setGender(context.getGender());
        long matched = safeItems.stream().filter(item -> "MATCHED".equals(item.getMatchStatus())).count();
        section.getSummary().put("reportedCount", safeItems.size());
        section.getSummary().put("matchedCount", matched);
        section.getSummary().put("unmatchedCount", safeItems.size() - matched);
        return section;
    }

    /**
     * 体细胞按产品启用基因过滤并补空行。
     *
     * @param items        报出的体细胞位点
     * @param productGenes 产品启用基因（有序）
     * @return 一行一个基因；没有位点的基因补 "/"
     */
    public List<ReportTemplateData.SomaticVariant> filterSomaticVariants(List<PreviewVariantVo> items,
                                                                        List<String> productGenes) {
        Map<String, PreviewVariantVo> byGene = indexByGene(items);
        List<ReportTemplateData.SomaticVariant> rows = new ArrayList<>();
        for (String gene : productGenes(productGenes)) {
            PreviewVariantVo item = byGene.get(gene);
            ReportTemplateData.SomaticVariant row = new ReportTemplateData.SomaticVariant();
            row.setGene(gene);
            if (item == null) {
                row.setMutationType(MISSING_VALUE);
                row.setResult(MISSING_VALUE);
                row.setAbundanceOrCopyNumber(MISSING_VALUE);
                row.setClassification(MISSING_VALUE);
            } else {
                row.setMutationType(somaticMutationType(item.getSourceType()));
                row.setResult(firstNonBlank(item.getOriVariant(), item.getVariant()));
                row.setAbundanceOrCopyNumber(abundance(item));
                row.setClassification(firstNonBlank(item.getVariationClass()));
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * 胚系按产品启用基因过滤并补空行。
     *
     * @param items        报出的胚系位点
     * @param productGenes 产品启用基因（有序）
     * @return 一行一个基因；没有位点的基因补 "/"
     */
    public List<ReportTemplateData.GermlineVariant> filterGermlineVariants(List<PreviewVariantVo> items,
                                                                           List<String> productGenes) {
        Map<String, PreviewVariantVo> byGene = indexByGene(items);
        List<ReportTemplateData.GermlineVariant> rows = new ArrayList<>();
        for (String gene : productGenes(productGenes)) {
            PreviewVariantVo item = byGene.get(gene);
            ReportTemplateData.GermlineVariant row = new ReportTemplateData.GermlineVariant();
            row.setGene(gene);
            if (item == null) {
                row.setMutationType(MISSING_VALUE);
                row.setResult(MISSING_VALUE);
                row.setZygosity(MISSING_VALUE);
                row.setClassification(MISSING_VALUE);
            } else {
                row.setMutationType(firstNonBlank(item.getMutationType(), MISSING_VALUE));
                row.setResult(firstNonBlank(item.getVariant(), item.getOriVariant()));
                row.setZygosity(firstNonBlank(item.getZygosity(), MISSING_VALUE));
                row.setClassification(firstNonBlank(item.getClinicalSignificanceLabel(), MISSING_VALUE));
            }
            rows.add(row);
        }
        return rows;
    }

    /** 产品启用基因：去空、去重、保持顺序 */
    public List<String> productGenes(List<String> genes) {
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        if (genes != null) {
            for (String gene : genes) {
                if (StringUtils.hasText(gene)) {
                    cleaned.add(gene.trim());
                }
            }
        }
        return new ArrayList<>(cleaned);
    }

    /** 标本类型（组织 / 血液）；判不出返回 null，由模板决定怎么显示 */
    public String specimenType(ReportModuleContext context) {
        String product = productOf(context.getReport());
        if (isTissueSample(product)) {
            return "组织";
        }
        if (isBloodOnlySample(product, false)) {
            return "血液";
        }
        return null;
    }

    /** 组织样本产品（novopm2_tis_1238 这类） */
    public boolean isTissueSample(String product) {
        return product != null && TISSUE_PRODUCT.matcher(product).matches();
    }

    /** 仅血液样本产品（novopm2_blo_1238 这类） */
    public boolean isBloodOnlySample(String product, boolean hasBlood) {
        return product != null && BLOOD_PRODUCT.matcher(product).matches();
    }

    // ---------------------------------------------------------------- 内部细节

    /** 英文标本类型（对齐参考工程 reportInfo.specimentType：tissue / blood） */
    private String englishSpecimenType(String specimenType) {
        if ("组织".equals(specimenType)) {
            return "tissue";
        }
        if ("血液".equals(specimenType)) {
            return "blood";
        }
        return null;
    }

    /** 报告日期：analysis_date（yyyyMMdd）→ yyyy-MM-dd；解析不出返回原值 */
    private String reportDate(Map<String, Object> report) {
        String analysisDate = report == null ? null : text(report.get("analysisDate"));
        if (!StringUtils.hasText(analysisDate)) {
            return null;
        }
        String trimmed = analysisDate.trim();
        if (trimmed.length() == 8 && trimmed.chars().allMatch(Character::isDigit)) {
            return trimmed.substring(0, 4) + "-" + trimmed.substring(4, 6) + "-" + trimmed.substring(6, 8);
        }
        return trimmed;
    }

    /** 样本类型展示值：组织+全血 / 组织 / 全血；判不出退回 LIMS 原文 */
    private String sampleTypeLabel(ReportModuleContext context) {
        String product = productOf(context.getReport());
        boolean tissue = isTissueSample(product);
        boolean bloodOnly = isBloodOnlySample(product, false);
        if (tissue && bloodOnly) {
            return "组织+全血";
        }
        if (tissue) {
            return "组织";
        }
        if (bloodOnly) {
            return "全血";
        }
        return valueOf(context.getLims(), l -> l.getSpecimenType());
    }

    /** 性别归一成 女 / 男；无法识别返回原值 */
    private String genderLabel(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        String value = raw.trim();
        if (value.contains("女") || value.toUpperCase().startsWith("F")) {
            return "女";
        }
        if (value.contains("男") || value.toUpperCase().startsWith("M")) {
            return "男";
        }
        return value;
    }

    /** 出生年份：从出生日期里取 4 位数字 */
    private String birthYear(String birthday) {
        if (!StringUtils.hasText(birthday)) {
            return birthday;
        }
        Matcher matcher = FOUR_DIGIT_YEAR.matcher(birthday);
        return matcher.find() ? matcher.group(1) : birthday.trim();
    }

    /** 体细胞突变类型（模板展示口径） */
    private String somaticMutationType(String sourceType) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("SNP_INDEL", "SNV/Indel");
        labels.put("CNV", "CNV");
        labels.put("FUSION", "Fusion");
        String label = sourceType == null ? null : labels.get(sourceType.toUpperCase());
        return label == null ? MISSING_VALUE : label;
    }

    /** 丰度/拷贝数：DNA 带 %，其它原样；空值补 "/" */
    private String abundance(PreviewVariantVo item) {
        String frequency = firstNonBlank(item.getFrequency());
        if (frequency == null) {
            return MISSING_VALUE;
        }
        if (frequency.endsWith("%")) {
            return frequency;
        }
        return frequency + "%";
    }

    /** 按基因索引（同基因多行时保留第一行，避免覆盖成最后一行导致结果不稳定） */
    private Map<String, PreviewVariantVo> indexByGene(List<PreviewVariantVo> items) {
        Map<String, PreviewVariantVo> byGene = new LinkedHashMap<>();
        for (PreviewVariantVo item : items == null ? List.<PreviewVariantVo>of() : items) {
            String gene = item.getGene();
            if (StringUtils.hasText(gene)) {
                byGene.putIfAbsent(gene.trim(), item);
            }
        }
        return byGene;
    }

    /** 报告行里的产品名（novopm2_tis_1238 这类） */
    private String productOf(Map<String, Object> report) {
        return report == null ? null : text(report.get("product"));
    }

    private String valueOf(org.dromara.report.domain.vo.InterpretationLimsVo lims,
                           java.util.function.Function<org.dromara.report.domain.vo.InterpretationLimsVo, String> getter) {
        return lims == null ? null : getter.apply(lims);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
