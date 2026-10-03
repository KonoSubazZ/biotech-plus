package org.dromara.qc.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.qc.domain.QcRecord;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 质控记录业务对象 qc_record
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
// mapstruct-plus：生成 Bo → 实体的转换器。Service 里 MapstructUtils.convert(bo, X.class) 靠它，
// 少了这个注解，运行期会抛 ConvertException: cannot find converter from XxxBo to Xxx
@AutoMapper(target = QcRecord.class, reverseConvertGenerate = false)
public class QcRecordBo extends BaseEntity {

    /** 主键；新增时为空，更新时必填 */
    private Long id;

    // @fields:start
    /** 样本条码 */
    @NotBlank(message = "样本条码不能为空")
    @Size(max = 80, message = "样本条码长度不能超过 80")
    private String subbarcode;

    /** 质控项目名称（如 mapping_rate、average_depth） */
    @NotBlank(message = "质控项目不能为空")
    @Size(max = 100, message = "质控项目长度不能超过 100")
    private String qcItem;

    /** 质控结果数值（字符串形式，如 "98.5"） */
    @Size(max = 50, message = "质控结果长度不能超过 50")
    private String qcResult;

    /** 操作员 */
    @Size(max = 80, message = "操作员长度不能超过 80")
    private String operator;

    /** 检测时间 */
    private Date testedAt;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;

    /** 人工状态（pending 待确认 / passed 通过 / failed 未通过） */
    @NotBlank(message = "人工状态不能为空")
    @Size(max = 20, message = "人工状态长度不能超过 20")
    private String status;

    /** 质控类别（wet_lab 湿实验 / bioinfo 生信） */
    @NotBlank(message = "质控类别不能为空")
    @Size(max = 20, message = "质控类别长度不能超过 20")
    private String qcCategory;
    // @fields:end
}
