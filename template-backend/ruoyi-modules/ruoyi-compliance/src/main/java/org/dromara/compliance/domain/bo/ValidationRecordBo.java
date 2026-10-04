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
import org.dromara.compliance.domain.ValidationRecord;

import java.util.Date;

/**
 * 3Q 验证记录业务对象 validation_record
 * <p>
 * 校验注解都显式声明了 groups = {AddGroup, EditGroup}：Controller 用
 * @Validated(AddGroup.class) / @Validated(EditGroup.class) 触发分组校验，
 * 不写 groups 的约束属于 Default 组、**不会被执行**（校验形同虚设）。
 *
 * @author liushangzhi
 */
@Data
@EqualsAndHashCode(callSuper = true)
// mapstruct-plus：由 Bo 生成到实体的转换器（Service 里的 MapstructUtils.convert(bo, ValidationRecord.class) 靠它）
@AutoMapper(target = ValidationRecord.class, reverseConvertGenerate = false)
public class ValidationRecordBo extends BaseEntity {

    /** 主键；新增时为空，更新时必填 */
    private Long id;

    // @fields:start
    /** 验证类型（IQ / OQ / PQ） */
    @NotBlank(message = "验证类型不能为空", groups = {AddGroup.class, EditGroup.class})
    @Pattern(regexp = "IQ|OQ|PQ", message = "验证类型只能是 IQ / OQ / PQ",
        groups = {AddGroup.class, EditGroup.class})
    private String validationType;

    /** 验证标题 */
    @NotBlank(message = "验证标题不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 200, message = "验证标题长度不能超过 200", groups = {AddGroup.class, EditGroup.class})
    private String title;

    /** 版本号（本次验证对应的方案/镜像版本） */
    @Size(max = 50, message = "版本号长度不能超过 50", groups = {AddGroup.class, EditGroup.class})
    private String version;

    /** 执行人 */
    @Size(max = 80, message = "执行人长度不能超过 80", groups = {AddGroup.class, EditGroup.class})
    private String executedBy;

    /** 执行日期 */
    private Date executedDate;

    /** 结果（passed / failed / na） */
    @NotBlank(message = "结果不能为空", groups = {AddGroup.class, EditGroup.class})
    @Pattern(regexp = "passed|failed|na", message = "结果只能是 passed / failed / na",
        groups = {AddGroup.class, EditGroup.class})
    private String result;

    /** 验证摘要 */
    private String summary;

    /** 验证文档文件名（原文件名；由上传接口写入，表单只回填） */
    @Size(max = 255, message = "验证文档文件名长度不能超过 255", groups = {AddGroup.class, EditGroup.class})
    private String fileName;
    // @fields:end
}
