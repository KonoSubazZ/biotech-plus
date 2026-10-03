package org.dromara.qc.domain.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.qc.domain.QcRecord;

/**
 * 质控记录视图对象 qc_record
 *
 * @author <你的名字>
 */
@Data
@NoArgsConstructor
// mapstruct-plus：生成 Vo ↔ 实体的双向转换器（查询结果 Entity → Vo 靠它）
@AutoMapper(target = QcRecord.class)
public class QcRecordVo {

    /** 主键 */
    private Long id;

    // @fields:start
    /** 样本条码 */
    private String subbarcode;

    /** 质控项目 */
    private String qcItem;

    /** 质控结果 */
    private String qcResult;

    /** 操作员 */
    private String operator;

    /** 检测时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date testedAt;

    /** 备注 */
    private String remark;

    /** 人工状态 */
    private String status;

    /** 质控类别 */
    private String qcCategory;
    // @fields:end

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
