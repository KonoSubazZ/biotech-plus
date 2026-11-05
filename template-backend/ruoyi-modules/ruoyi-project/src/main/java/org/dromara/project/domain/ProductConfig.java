package org.dromara.project.domain;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 产品配置对象 product_config
 * <p>
 * 注意：实体只用于持久化，不直接作为接口返回值（用 ProductConfigVo）。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product_config")
public class ProductConfig extends BaseEntity {

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    // @fields:start
    /** 产品编码 */
    private String productCode;

    /** 商品名 */
    private String tradeName;

    /** 生信编码（= 上传目录第 2 层 product，与 analysis_data.product_bio 逐字相等） */
    private String productBio;

    /** 产品名称（系统用） */
    private String productName;

    /** 适应症 */
    private String relatedDiseases;

    /** 样本类型（组织 / 血液） */
    private String testType;

    /** 对照（血液 / PBMC / 无） */
    private String controlType;

    /** 版本（LDT / IVD） */
    private String productVersion;

    /** 测序平台（surf5000 Dx 等） */
    private String sequencingPlatform;

    /** 基因数/DNA（panel 基因个数，如 1238） */
    private Integer geneCount;

    /** 报告周期天数 */
    private Integer reportCycleDays;

    /** 维护实验室 */
    private String maintainLab;

    /** 关联工序（如 全流程） */
    private String relatedProcess;

    /** 探针类型（如 正常1238探针 / 正常1238探针+UMI） */
    private String probeType;

    /** panel类型（DNA / RNA / DNA_RNA / WES / HRD 等，图里没有，冗余保留） */
    private String panelType;

    /** 状态 */
    private String status;

    /** 备注 */
    private String remark;
    // @fields:end

    /** 逻辑删除标志 */
    @TableLogic
    private String delFlag;
}
