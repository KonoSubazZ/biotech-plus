package org.dromara.report.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.report.domain.ReportTemplate;

import java.util.Date;
import java.util.List;
import java.util.Map;

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

    /** 模板归属项目（project_config.id） */
    private Long projectId;

    /** 项目编号（列表/搜索展示用，由 Service 批量回填） */
    private String projectNo;

    /** 项目名称（列表展示用） */
    private String projectName;

    /** 报告类型 */
    private String reportType;

    /** 有序个性化模块编码列表（分号分隔） */
    private String moduleCode;

    /** 报告命名模板：静态文本 + {{路径}} 动态取值 */
    private String reportName;

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

    /**
     * 关联产品明细（列表展示用）：每项含 productId / productBio / productName / productCode。
     * <p>
     * 页面口径（2026-10-22）：关联产品列显示「生信编码 product_bio」换行「产品编码 product_code」，
     * 所以后端把三个标识一起回填，前端不再自己拼 label。
     */
    private List<Map<String, Object>> products;
}
