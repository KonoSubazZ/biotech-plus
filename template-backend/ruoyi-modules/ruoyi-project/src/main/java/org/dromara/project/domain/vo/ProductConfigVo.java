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

    /** panel类型（DNA / RNA / DNA_RNA / WES / HRD 等） */
    private String panelType;

    /** 状态 */
    private String status;

    /** 备注 */
    private String remark;
    // @fields:end

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
