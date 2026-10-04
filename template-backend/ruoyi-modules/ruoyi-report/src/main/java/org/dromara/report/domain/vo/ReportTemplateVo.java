package org.dromara.report.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.report.domain.ReportTemplate;

import java.util.Date;
import java.util.List;

/**
 * 报告模板视图对象 report_template
 *
 * @author <你的名字>
 */
@Data
@NoArgsConstructor
// mapstruct-plus：生成 Vo ↔ 实体的双向转换器（查询结果 Entity → Vo 靠它）
@AutoMapper(target = ReportTemplate.class)
public class ReportTemplateVo {

    /** 主键 */
    private Long templateId;

    /** 稳定模板编码 */
    private String templateCode;

    /** 模板名称 */
    private String templateName;

    /** 模板版本 */
    private String templateVersion;

    /** 客户编码 */
    private String customerCode;

    /** 报告类型 */
    private String reportType;

    /** 有序个性化模块编码列表（分号分隔） */
    private String moduleCode;

    /** DOCX 模板文件路径 */
    private String templatePath;

    /** 模板文件 SHA-256 */
    private String templateSha256;

    /** 状态 */
    private String status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 关联产品ID集合（详情/编辑用，来自 product_template） */
    private List<Long> productIds;

    /** 关联产品名称集合（列表展示用） */
    private List<String> productNames;
}
