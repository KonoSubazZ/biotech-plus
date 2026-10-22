package org.dromara.project.domain.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.project.domain.ProductConfig;

/**
 * 产品配置视图对象 product_config
 *
 * @author <你的名字>
 */
@Data
@NoArgsConstructor
// mapstruct-plus：生成 Vo ↔ 实体的双向转换器（查询结果 Entity → Vo 靠它）
@AutoMapper(target = ProductConfig.class)
public class ProductConfigVo {

    /** 主键 */
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

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
