package org.dromara.project.domain;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目配置对象 project_config
 * <p>
 * 注意：实体只用于持久化，不直接作为接口返回值（用 ProjectConfigVo）。
 *
 * @author levi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_config")
public class ProjectConfig extends BaseEntity {

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 父节点 ID（0=项目行；研究中心行=所属项目行 id） */
    private Long parentId;

    // @fields:start
    /** 项目编号（项目行唯一；研究中心行冗余存） */
    private String projectNo;

    /** 项目名称（仅项目行有值） */
    private String projectName;

    /** 客户名称（仅项目行有值） */
    private String customer;

    /** 合同名称（仅项目行有值；展示字段，不参与业务逻辑） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String contractName;

    /** 合同编号（仅项目行有值；展示字段） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String contractNo;

    /** 研究方案编号（仅项目行有值；展示字段） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String schemeNo;

    /** 研究中心编号（仅研究中心行有值；项目行为空串） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String researchCenterNo;

    /** 研究中心名称（仅研究中心行有值；项目行为空串） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String researchCenterName;

    /** 收件邮箱（项目行=项目自身，研究中心行=该中心） */
    private String receiveEmail;

    /** 项目通知收件邮箱（;分隔；仅项目行使用，与报告收件邮箱分开维护） */
    private String projectNotifyEmail;

    /** 抄送邮箱（项目行=项目自身，研究中心行=该中心） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String ccEmail;

    /** 备注 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;
    // @fields:end

    /** 逻辑删除标志 */
    @TableLogic
    private String delFlag;
}
