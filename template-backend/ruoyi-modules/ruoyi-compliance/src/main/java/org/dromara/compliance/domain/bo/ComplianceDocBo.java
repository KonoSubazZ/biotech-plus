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
import org.dromara.compliance.domain.ComplianceDoc;

/**
 * 3Q 文档管理业务对象 compliance_document
 * <p>
 * 校验注解都显式声明了 groups = {AddGroup, EditGroup}：Controller 用
 * @Validated(AddGroup.class) / @Validated(EditGroup.class) 触发分组校验，
 * 不写 groups 的约束属于 Default 组、**不会被执行**（校验形同虚设）。
 *
 * @author liushangzhi
 */
@Data
@EqualsAndHashCode(callSuper = true)
// mapstruct-plus：由 Bo 生成到实体的转换器（Service 里的 MapstructUtils.convert(bo, ComplianceDoc.class) 靠它）
@AutoMapper(target = ComplianceDoc.class, reverseConvertGenerate = false)
public class ComplianceDocBo extends BaseEntity {

    /** 主键；新增时为空，更新时必填 */
    private Long id;

    // @fields:start
    /** 文档类型（IQ / OQ / PQ / DEV_TEST） */
    @NotBlank(message = "文档类型不能为空", groups = {AddGroup.class, EditGroup.class})
    @Pattern(regexp = "IQ|OQ|PQ|DEV_TEST", message = "文档类型只能是 IQ / OQ / PQ / DEV_TEST",
        groups = {AddGroup.class, EditGroup.class})
    private String docType;

    /** 文档标题 */
    @NotBlank(message = "文档标题不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 200, message = "文档标题长度不能超过 200", groups = {AddGroup.class, EditGroup.class})
    private String title;

    /** 版本号（设计文档里为必填） */
    @NotBlank(message = "版本号不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "版本号长度不能超过 50", groups = {AddGroup.class, EditGroup.class})
    private String version;

    /** 文档文件名（原文件名；由上传接口写入，表单只回填） */
    @Size(max = 255, message = "文档文件名长度不能超过 255", groups = {AddGroup.class, EditGroup.class})
    private String fileName;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过 500", groups = {AddGroup.class, EditGroup.class})
    private String remark;
    // @fields:end
}
