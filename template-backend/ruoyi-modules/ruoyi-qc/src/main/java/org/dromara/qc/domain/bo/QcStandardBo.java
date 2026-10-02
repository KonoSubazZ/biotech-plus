package org.dromara.qc.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.qc.domain.QcStandard;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 质控标准业务对象 qc_standard
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
// mapstruct-plus：由 Bo 生成到实体的转换器（Service 里的 MapstructUtils.convert(bo, QcStandard.class) 靠它）
@AutoMapper(target = QcStandard.class, reverseConvertGenerate = false)
public class QcStandardBo extends BaseEntity {

    /** 主键；新增时为空，更新时必填 */
    private Long id;

    // @fields:start
    /** 关联产品 */
    @NotNull(message = "关联产品不能为空")
    private Long productId;

    /** 质控项目名称 */
    @NotBlank(message = "质控项目名称不能为空")
    @Size(max = 100, message = "质控项目名称长度不能超过 100")
    private String qcItem;

    /** 质控类别 */
    @NotBlank(message = "质控类别不能为空")
    @Size(max = 20, message = "质控类别长度不能超过 20")
    private String qcCategory;

    /** 合格下限 */
    @Size(max = 50, message = "合格下限长度不能超过 50")
    private String minValue;

    /** 合格上限 */
    @Size(max = 50, message = "合格上限长度不能超过 50")
    private String maxValue;

    /** 单位 */
    @Size(max = 30, message = "单位长度不能超过 30")
    private String unit;

    /** 状态 */
    @NotBlank(message = "状态不能为空")
    @Size(max = 20, message = "状态长度不能超过 20")
    private String status;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
    // @fields:end
}
