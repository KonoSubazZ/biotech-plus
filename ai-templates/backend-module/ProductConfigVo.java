package org.dromara.biotech.domain.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.biotech.domain.ProductConfig;

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
    /** 产品名称 */
    private String name;

    /** 产品编码 */
    private String code;

    /** 检测类型 */
    private String testType;

    /** 相关疾病 */
    private String relatedDiseases;

    /** 报告周期（天） */
    private Integer reportCycleDays;

    /** 状态：active / inactive */
    private String status;

    /** 备注 */
    private String remark;
    // @fields:end

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
