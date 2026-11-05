package org.dromara.project.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.project.domain.ProjectConfig;

/**
 * 研究中心业务对象（project_config 里 parent_id &gt; 0 的行）
 * <p>
 * 项目名/客户名/合同三项不在这里 —— 它们只存在父项目行上，中心行写空，
 * 避免一份数据两处维护；研究中心编号与名称成对唯一。
 *
 * @author levi
 */
@Data
@AutoMapper(target = ProjectConfig.class, reverseConvertGenerate = false)
public class ProjectCenterBo {

    /** 主键；新增时为空，更新时必填 */
    private Long id;

    /** 所属项目行主键（新增必填；更新时可空，服务端按现有行取） */
    private Long parentId;

    /** 研究中心编号（同一项目下与名称成对唯一） */
    @NotBlank(message = "研究中心编号不能为空")
    @Size(max = 100, message = "研究中心编号长度不能超过 100")
    private String researchCenterNo;

    /** 研究中心名称（同一项目下与编号成对唯一） */
    @NotBlank(message = "研究中心名称不能为空")
    @Size(max = 200, message = "研究中心名称长度不能超过 200")
    private String researchCenterName;

    /** 收件邮箱（多个用分号分隔） */
    @NotBlank(message = "收件邮箱不能为空")
    @Size(max = 500, message = "收件邮箱长度不能超过 500")
    private String receiveEmail;

    /** 抄送邮箱（多个用分号分隔） */
    @Size(max = 500, message = "抄送邮箱长度不能超过 500")
    private String ccEmail;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
