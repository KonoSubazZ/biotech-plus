package org.dromara.qc.domain;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 质控记录对象 qc_record
 * <p>
 * 注意：实体只用于持久化，不直接作为接口返回值（用 QcRecordVo）。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qc_record")
public class QcRecord extends BaseEntity {

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    // @fields:start
    /** 样本条码 */
    private String subbarcode;

    /** 质控项目名称（如 mapping_rate、average_depth） */
    private String qcItem;

    /** 质控结果数值（字符串形式，如 "98.5"；保持 varchar 以兼容 "<0.5" 这类写法） */
    private String qcResult;

    /** 操作员 */
    private String operator;

    /** 检测时间 */
    private Date testedAt;

    /** 备注 */
    private String remark;

    /** 人工状态（pending 待确认 / passed 通过 / failed 未通过） */
    private String status;

    /** 质控类别（wet_lab 湿实验 / bioinfo 生信） */
    private String qcCategory;
    // @fields:end

    /** 逻辑删除标志 */
    @TableLogic
    private String delFlag;
}
