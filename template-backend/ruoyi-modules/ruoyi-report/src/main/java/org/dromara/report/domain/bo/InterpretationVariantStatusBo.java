package org.dromara.report.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 「入报告」开关入参
 * <p>
 * 不报出时是否必填过滤理由，取决于当前业务规则：本页先不强制（留字段，需要时再加校验）。
 *
 * @author <你的名字>
 */
@Data
public class InterpretationVariantStatusBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分析数据ID（校验位点归属） */
    @NotNull(message = "分析数据ID不能为空")
    private Long analysisId;

    /** 位点类型：SNP_INDEL / CNV / FUSION / CR_ALL */
    @NotBlank(message = "位点类型不能为空")
    private String sourceType;

    /** 位点主键（明细表 id） */
    @NotNull(message = "位点ID不能为空")
    private Long sourceId;

    /** 是否纳入报告：0 否 / 1 是 */
    @NotNull(message = "入报告状态不能为空")
    private Integer isReported;

    /** 过滤理由（不报出时可选） */
    private String filteredRationale;
}
