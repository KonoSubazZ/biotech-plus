package org.dromara.report.domain.dto;

import lombok.Data;
import org.dromara.report.domain.vo.InterpretationLimsVo;
import org.dromara.report.domain.vo.PreviewVariantVo;
import org.dromara.report.domain.vo.ReportTemplateVo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 报告模块处理器上下文：公共查询做一次，Handler 之间共享，不重复查库。
 * <p>
 * 用 {@code @Data} 类而不是 record：字段有十几个，record 的构造参数会被可读性规则判为「参数过多」
 * （与 {@code PreviewContext} 同一取舍）。
 * <p>
 * 与 {@code PreviewContext} 的分工：后者是「Service 内部的一次性查询缓存」，
 * 本类是对外传给 Handler 的上下文 —— 两者共存，不合并，避免大改既有匹配链路。
 *
 * @author <你的名字>
 */
@Data
public class ReportModuleContext {

    /** 分析批次ID */
    private Long analysisId;

    /** 报告ID */
    private Long reportId;

    /** 报告 + 分析行（selectReportRow） */
    private Map<String, Object> report;

    /** LIMS 信息（sample_file 映射） */
    private InterpretationLimsVo lims;

    /** 已算好的体细胞报出位点（含匹配结果） */
    private List<PreviewVariantVo> somaticVariants;

    /** 已算好的胚系报出位点（含匹配结果与五级临床意义） */
    private List<PreviewVariantVo> germlineVariants;

    /** 本次使用的模板（可能为空：报告未绑定模板时只输出公共字段） */
    private ReportTemplateVo template;

    /** 产品启用的基因（有序；Handler 声明 requiresProductGenes 时才查） */
    private List<String> productGenes;

    /** 解读癌种 */
    private String disease;

    /** 解读癌种在 NKB 的 ID */
    private Long diseaseId;

    /** 癌种范围（本癌种 + 祖先 + 子孙，已按性别/瘤种剔除） */
    private List<Long> diseaseIds;

    /** 若干「白名单癌种名 → NKB disease id」，供 {@link #diseaseBelongsToAny} 判定用 */
    private Map<String, Long> namedDiseaseIds = new LinkedHashMap<>();

    /** 性别（MALE / FEMALE / UNKNOWN） */
    private String gender;

    /** 标本类型（组织 / 血液；判不出为 null） */
    private String specimenType;

    /** 告警（公共组装与 Handler 都会往里追加，最终写进 JSON 的 warnings） */
    private List<String> warnings;

    /**
     * 当前癌种（或它的任一父级/子级）是否属于给定癌种集合。
     * <p>
     * 判定口径：NKB 癌种范围 {@link #diseaseIds} 里含有任一目标的 disease id 即命中
     * （范围已含本癌种 + 全部祖先，所以「子类癌种」也能命中父级「乳腺癌」这类配置）。
     *
     * @param names 目标癌种中文名集合
     * @return true 表示命中
     */
    public boolean diseaseBelongsToAny(List<String> names) {
        if (names == null || names.isEmpty()) {
            return false;
        }
        for (String name : names) {
            if (name.equals(disease)) {
                return true;
            }
            Long namedId = namedDiseaseIds.get(name);
            if (namedId != null && diseaseIds != null && diseaseIds.contains(namedId)) {
                return true;
            }
        }
        return false;
    }
}
