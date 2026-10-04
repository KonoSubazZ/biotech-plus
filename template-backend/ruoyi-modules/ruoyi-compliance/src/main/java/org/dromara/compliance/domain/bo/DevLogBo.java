package org.dromara.compliance.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.compliance.domain.DevLog;

import java.util.Date;

/**
 * 开发记录业务对象 dev_log
 * <p>
 * 校验注解都显式声明了 groups = {AddGroup, EditGroup}：Controller 用
 * @Validated(AddGroup.class) / @Validated(EditGroup.class) 触发分组校验，
 * 不写 groups 的约束属于 Default 组、**不会被执行**（校验形同虚设）。
 *
 * @author liushangzhi
 */
@Data
@EqualsAndHashCode(callSuper = true)
// mapstruct-plus：由 Bo 生成到实体的转换器（Service 里的 MapstructUtils.convert(bo, DevLog.class) 靠它）
@AutoMapper(target = DevLog.class, reverseConvertGenerate = false)
public class DevLogBo extends BaseEntity {

    /** 主键；新增时为空，更新时必填 */
    private Long id;

    // @fields:start
    /** 记录标题 */
    @NotBlank(message = "记录标题不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 200, message = "记录标题长度不能超过 200", groups = {AddGroup.class, EditGroup.class})
    private String title;

    /** 分类（feature 功能新增 / fix 缺陷修复 / change 变更调整） */
    @NotBlank(message = "分类不能为空", groups = {AddGroup.class, EditGroup.class})
    @Pattern(regexp = "feature|fix|change", message = "分类只能是 feature / fix / change",
        groups = {AddGroup.class, EditGroup.class})
    private String category;

    /** 详细内容 */
    private String content;

    /** 开发人员 */
    @Size(max = 80, message = "开发人员长度不能超过 80", groups = {AddGroup.class, EditGroup.class})
    private String developer;

    /** 记录日期 */
    private Date logDate;
    // @fields:end
}
