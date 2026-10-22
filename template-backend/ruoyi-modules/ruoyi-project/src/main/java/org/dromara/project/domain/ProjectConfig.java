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

    // @fields:start
    /** 项目编号 */
    private String projectNo;

    /** 项目名称 */
    private String projectName;

    /** 客户名称 */
    private String customer;

    /** 收件邮箱 */
    private String receiveEmail;

    /** 抄送邮箱 */
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
