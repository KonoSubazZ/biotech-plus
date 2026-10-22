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

    /** 生信编码（= 上传目录第 2 层 product，与 analysis_data.product 逐字相等） */
    private String productBio;

    /** 产品名称（对外展示、报告用） */
    private String productName;

    /** 检测类型（组织 / 血液） */
    private String testType;

    /** panel类型（DNA / RNA / DNA_RNA / WES / HRD 等） */
    private String panelType;

    /** 相关疾病 */
    private String relatedDiseases;

    /** 报告周期天数 */
    private Integer reportCycleDays;

    /** 状态 */
    private String status;

    /** 备注 */
    private String remark;
    // @fields:end

    /** 逻辑删除标志 */
    @TableLogic
    private String delFlag;
}
