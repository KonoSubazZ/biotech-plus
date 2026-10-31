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

    /** 合同名称（展示字段，不参与业务逻辑） */
    @Size(max = 200, message = "合同名称长度不能超过 200")
    private String contractName;

    /** 合同编号（展示字段，不参与业务逻辑） */
    @Size(max = 100, message = "合同编号长度不能超过 100")
    private String contractNo;

    /** 研究方案编号（展示字段，不参与业务逻辑） */
    @Size(max = 100, message = "研究方案编号长度不能超过 100")
    private String schemeNo;

    /**
     * 收件邮箱（报告发送用）
     * <p>
     * 页面上已隐藏（2026-10-29），只保留存储：历史数据与 report-driver 取数仍按这一列走。
     * 因为界面不再展示/编辑，这里不能要求必填 —— 新增项目时前端根本不会传值。
     */
    @Size(max = 500, message = "收件邮箱长度不能超过 500")
    private String receiveEmail;

    /** 项目通知收件邮箱（页面上的「项目通知（收件）邮箱」，可空） */
    @Size(max = 500, message = "项目通知邮箱长度不能超过 500")
    private String projectNotifyEmail;

    /** 抄送邮箱 */
    @Size(max = 500, message = "抄送邮箱长度不能超过 500")
    private String ccEmail;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
    // @fields:end
}
