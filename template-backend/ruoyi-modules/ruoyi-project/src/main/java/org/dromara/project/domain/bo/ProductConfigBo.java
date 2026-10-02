package org.dromara.project.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.project.domain.ProductConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 产品配置业务对象 product_config
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
// mapstruct-plus：生成 Bo → 实体的转换器。Service 里 MapstructUtils.convert(bo, X.class) 靠它，
// 少了这个注解，运行期会抛 ConvertException: cannot find converter from XxxBo to Xxx
@AutoMapper(target = ProductConfig.class, reverseConvertGenerate = false)
public class ProductConfigBo extends BaseEntity {

    /** 主键；新增时为空，更新时必填 */
    private Long id;

    // @fields:start
    /** 产品名称 */
    @NotBlank(message = "产品名称不能为空")
    @Size(max = 120, message = "产品名称长度不能超过 120")
    private String name;

    /** 产品编码 */
    @NotBlank(message = "产品编码不能为空")
    @Size(max = 60, message = "产品编码长度不能超过 60")
    private String code;

    /** 检测类型 */
    @Size(max = 60, message = "检测类型长度不能超过 60")
    private String testType;

    /** 相关疾病 */
    @Size(max = 500, message = "相关疾病长度不能超过 500")
    private String relatedDiseases;

    /** 报告周期天数 */
    private Integer reportCycleDays;

    /** 状态 */
    @NotBlank(message = "状态不能为空")
    @Size(max = 20, message = "状态长度不能超过 20")
    private String status;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
    // @fields:end
}
