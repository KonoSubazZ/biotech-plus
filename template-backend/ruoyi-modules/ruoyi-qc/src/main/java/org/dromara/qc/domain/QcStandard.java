package org.dromara.qc.domain;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 质控标准对象 qc_standard
 * <p>
 * 注意：实体只用于持久化，不直接作为接口返回值（用 QcStandardVo）。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qc_standard")
public class QcStandard extends BaseEntity {

    /** 主键 */
    @TableId(value = "id")
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

    /** 逻辑删除标志 */
    @TableLogic
    private String delFlag;
}
