package org.dromara.report.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 位点人工修改（父靶点 / 胚系临床意义 / 突变说明），报告预览页统一入参。
 * <p>
 * 存 {@code analysis_report.report_variant_edits}（json，键 = gene_variant_sourceType:sourceId）；
 * 只影响本报告生成的 JSON，不写 history_*、不写源位点表。
 * 只有可生成报告的状态（31/32/33/52）允许写；来源=历史的位点只读。
 *
 * @author <你的名字>
 */
@Data
public class InterpretationVariantEditBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分析数据ID */
    @NotNull(message = "分析数据ID不能为空")
    private Long analysisId;

    /** 报告ID */
    @NotNull(message = "报告ID不能为空")
    private Long reportId;

    /** 源位点表（SNP_INDEL / CNV / FUSION / CR_ALL） */
    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    /** 位点ID（对应源位点表主键） */
    @NotNull(message = "位点ID不能为空")
    private Long sourceId;

    /** 父靶点节点ID；null = 不改，空列表 = 取消改靶 */
    private List<Long> parentMutationIds;

    /** 胚系临床意义 1~5；null = 不改（仅 CR_ALL 可传） */
    private Integer clinicalSignificance;

    /** 突变说明整段文本；null = 不改，空串 = 恢复默认 */
    private String mutationDescription;
}
