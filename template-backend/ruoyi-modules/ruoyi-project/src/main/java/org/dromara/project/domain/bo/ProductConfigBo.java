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
    /** 产品编码 */
    @NotBlank(message = "产品编码不能为空")
    @Size(max = 60, message = "产品编码长度不能超过 60")
    private String productCode;

    /** 商品名 */
    @Size(max = 120, message = "商品名长度不能超过 120")
    private String tradeName;

    /** 生信编码（= 上传目录第 2 层 product，与 analysis_data.product_bio 逐字相等） */
    @NotBlank(message = "生信编码不能为空")
    @Size(max = 120, message = "生信编码长度不能超过 120")
    private String productBio;

    /** 产品名称（系统用） */
    @NotBlank(message = "产品名称不能为空")
    @Size(max = 120, message = "产品名称长度不能超过 120")
    private String productName;

    /** 适应症 */
    @Size(max = 500, message = "适应症长度不能超过 500")
    private String relatedDiseases;

    /** 样本类型（组织 / 血液） */
    @Size(max = 60, message = "样本类型长度不能超过 60")
    private String testType;

    /** 对照（血液 / PBMC / 无） */
    @Size(max = 60, message = "对照长度不能超过 60")
    private String controlType;

    /** 版本（LDT / IVD） */
    @Size(max = 30, message = "版本长度不能超过 30")
    private String productVersion;

    /** 测序平台（surf5000 Dx 等） */
    @Size(max = 60, message = "测序平台长度不能超过 60")
    private String sequencingPlatform;

    /** 基因数/DNA（panel 基因个数，如 1238） */
    private Integer geneCount;

    /** 报告周期天数 */
    private Integer reportCycleDays;

    /** 维护实验室 */
    @Size(max = 60, message = "维护实验室长度不能超过 60")
    private String maintainLab;

    /** 关联工序（如 全流程） */
    @Size(max = 100, message = "关联工序长度不能超过 100")
    private String relatedProcess;

    /** 探针类型（如 正常1238探针 / 正常1238探针+UMI） */
    @Size(max = 120, message = "探针类型长度不能超过 120")
    private String probeType;

    /** panel类型（DNA / RNA / DNA_RNA / WES / HRD 等） */
    @Size(max = 60, message = "panel类型长度不能超过 60")
    private String panelType;

    /** 状态 */
    @NotBlank(message = "状态不能为空")
    @Size(max = 20, message = "状态长度不能超过 20")
    private String status;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
    // @fields:end
}
