package org.dromara.qc.domain.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.qc.domain.QcStandard;

/**
 * 质控标准视图对象 qc_standard
 *
 * @author <你的名字>
 */
@Data
@NoArgsConstructor
// mapstruct-plus：生成 Vo ↔ 实体的双向转换器（查询结果 Entity → Vo 靠它）
@AutoMapper(target = QcStandard.class)
public class QcStandardVo {

    /** 主键 */
    private Long id;

    // @fields:start
    /** 关联产品 */
    private Long productId;

    /** 质控项目名称 */
    private String qcItem;

    /** 质控类别 */
    private String qcCategory;

    /** 合格下限 */
    private String minValue;

    /** 合格上限 */
    private String maxValue;

    /** 单位 */
    private String unit;

    /** 状态 */
    private String status;

    /** 备注 */
    private String remark;
    // @fields:end

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
