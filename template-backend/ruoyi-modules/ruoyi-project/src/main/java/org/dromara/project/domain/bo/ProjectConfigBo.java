package org.dromara.project.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.project.domain.ProjectConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目配置业务对象 project_config
 *
 * @author levi
 */
@Data
@EqualsAndHashCode(callSuper = true)
// mapstruct-plus：生成 Bo → 实体的转换器。Service 里 MapstructUtils.convert(bo, X.class) 靠它，
// 少了这个注解，运行期会抛 ConvertException: cannot find converter from XxxBo to Xxx
@AutoMapper(target = ProjectConfig.class, reverseConvertGenerate = false)
public class ProjectConfigBo extends BaseEntity {

    /** 主键；新增时为空，更新时必填 */
    private Long id;

    // @fields:start
    /** 项目编号 */
    @NotBlank(message = "项目编号不能为空")
    @Size(max = 60, message = "项目编号长度不能超过 60")
    private String projectNo;

    /** 项目名称 */
    @NotBlank(message = "项目名称不能为空")
    @Size(max = 200, message = "项目名称长度不能超过 200")
    private String projectName;

    /** 客户名称 */
    @NotBlank(message = "客户名称不能为空")
    @Size(max = 200, message = "客户名称长度不能超过 200")
    private String customer;

    /** 收件邮箱 */
    @NotBlank(message = "收件邮箱不能为空")
    @Size(max = 500, message = "收件邮箱长度不能超过 500")
    private String receiveEmail;

    /** 抄送邮箱 */
    @Size(max = 500, message = "抄送邮箱长度不能超过 500")
    private String ccEmail;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
    // @fields:end
}
