package org.dromara.report.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 人工修改位点「突变说明」（报告预览 › 肿瘤遗传风险 › 位点详情）。
 * <p>
 * 按**报告粒度**存进 {@code analysis_report.report_variant_notes}（JSON，键 = 来源类型:位点ID），
 * 生成报告时由 {@code InterpretationPreviewServiceImpl#assemble} 覆盖默认说明，
 * 所以预览里改完、再点「生成报告」，落盘的 JSON 里带的就是改后的文本。
 * <p>
 * 传空串 = 恢复默认说明（删键）。只有可生成报告的状态（31/32/33/52）允许写。
 *
 * @author <你的名字>
 */
@Data
public class InterpretationVariantNoteBo implements Serializable {

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

    /** 人工说明整段文本；空 = 恢复默认 */
    private String mutationDescription;
}
