package org.dromara.project.domain.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.project.domain.ProjectConfig;

/**
 * 项目配置视图对象 project_config
 *
 * @author levi
 */
@Data
@NoArgsConstructor
// mapstruct-plus：生成 Vo ↔ 实体的双向转换器（查询结果 Entity → Vo 靠它）
@AutoMapper(target = ProjectConfig.class)
public class ProjectConfigVo {

    /** 主键 */
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
    private String ccEmail;

    /** 备注 */
    private String remark;
    // @fields:end

    /** 创建者 */
    private Long createBy;

    /** 创建人名称（列表「创建人 / 创建时间」两行显示用；由 @Translation 按 createBy 换出用户昵称） */
    @Translation(type = TransConstant.USER_ID_TO_NICKNAME, mapper = "createBy")
    private String createByName;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新者 */
    private Long updateBy;

    /** 更新人名称（列表「更新人 / 更新时间」两行显示用） */
    @Translation(type = TransConstant.USER_ID_TO_NICKNAME, mapper = "updateBy")
    private String updateByName;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
