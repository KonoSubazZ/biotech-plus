package org.dromara.report.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 解读页 Tab③ 位点列表查询条件
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InterpretationVariantQueryBo extends BaseEntity {

    /** 位点类型：SNP_INDEL / CNV / FUSION / CR_ALL（必填，决定查哪张明细表） */
    @NotBlank(message = "位点类型不能为空")
    private String sourceType;

    /** 基因（模糊） */
    private String gene;

    /** 是否入报告：0 / 1（不传表示不限） */
    private Integer isReported;
}
