package org.dromara.report.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.report.domain.ReportTemplate;

import java.util.List;

/**
 * 报告模板业务对象 report_template
 * <p>
 * 校验注解都带 {@code groups}：Controller 用 {@code @Validated(AddGroup.class)} /
 * {@code @Validated(EditGroup.class)}，Spring 只校验被点名分组里的约束 —— 不带 groups 的约束
 * 属于 Default 组，一条都不会执行（本仓库既有模块的坑，这里显式补上）。
 * <p>
 * {@code productIds} 不是 report_template 的列，是「模板 ↔ 产品」多对多关系的入参/出参，
 * 由 Service 落到 product_template。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
// mapstruct-plus：生成 Bo → 实体的转换器；少了会在运行期抛
// ConvertException: cannot find converter from ReportTemplateBo to ReportTemplate
@AutoMapper(target = ReportTemplate.class, reverseConvertGenerate = false)
public class ReportTemplateBo extends BaseEntity {

    /** 主键；新增时为空，更新时必填 */
    private Long templateId;

    /** 稳定模板编码（业务键，同租户内唯一） */
    @NotBlank(message = "模板编码不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 120, message = "模板编码长度不能超过 120", groups = {AddGroup.class, EditGroup.class})
    private String templateCode;

    /** 模板名称 */
    @NotBlank(message = "模板名称不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 255, message = "模板名称长度不能超过 255", groups = {AddGroup.class, EditGroup.class})
    private String templateName;

    /** 模板版本 */
    @Size(max = 40, message = "模板版本长度不能超过 40", groups = {AddGroup.class, EditGroup.class})
    private String templateVersion;

    /** 客户编码 */
    @Size(max = 120, message = "客户编码长度不能超过 120", groups = {AddGroup.class, EditGroup.class})
    private String customerCode;

    /** 报告类型 */
    @NotBlank(message = "报告类型不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 40, message = "报告类型长度不能超过 40", groups = {AddGroup.class, EditGroup.class})
    private String reportType;

    /** 有序个性化模块编码列表（分号分隔）；空 = 只输出公共字段 */
    @Size(max = 500, message = "模块编码长度不能超过 500", groups = {AddGroup.class, EditGroup.class})
    private String moduleCode;

    /** 报告命名模板：静态文本 + {{路径}} 动态取值；空 = 用默认命名 */
    @Size(max = 500, message = "报告命名模板长度不能超过 500", groups = {AddGroup.class, EditGroup.class})
    private String reportName;

    /** DOCX 模板文件路径（预留） */
    @NotBlank(message = "模板路径不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 1000, message = "模板路径长度不能超过 1000", groups = {AddGroup.class, EditGroup.class})
    private String templatePath;

    /** 模板文件 SHA-256（预留） */
    @Size(max = 64, message = "模板校验值长度不能超过 64", groups = {AddGroup.class, EditGroup.class})
    private String templateSha256;

    /** 状态（ENABLED / DISABLED） */
    @NotBlank(message = "状态不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 20, message = "状态长度不能超过 20", groups = {AddGroup.class, EditGroup.class})
    private String status;

    /** 关联产品ID集合（落到 product_template；空集合表示解除全部关联） */
    private List<Long> productIds;
}
