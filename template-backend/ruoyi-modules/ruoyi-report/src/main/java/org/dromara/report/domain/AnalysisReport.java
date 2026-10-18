package org.dromara.report.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * 分析报告对象 analysis_report
 * <p>
 * 表来源见 script/sql/business/report-interpretation.sql（按参考工程 report_management_schema.sql 改造）。
 * 语义（docs/context/report/report-module-design.md）：
 * <ul>
 *   <li>analysis_data 表示一次分析批次，analysis_report 表示一份报告；一个批次可有多份报告（不同模板 / 驳回重出）。</li>
 *   <li>report_status 是状态机（int 码，取值与中文见 {@link org.dromara.report.utils.ReportStatus}）。</li>
 *   <li>product_id 指向 product_config.id（本仓产品主数据），不是参考工程里的 product 表。</li>
 * </ul>
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("analysis_report")
public class AnalysisReport extends BaseEntity {

    /** 报告ID */
    @TableId(value = "report_id")
    private Long reportId;

    private Integer reportVersion;
    private Long reportSequenceId;
    private Integer reportGenerationNo;
    private Long reportReviewerId;
    private Long sourceReportId;
    private Long reportReviewClaimedBy;
    private String reportName;

    /** 所属分析批次 */
    private Long analysisId;

    /** 分析日期（YYYYMMDD，与 analysis_data 同口径） */
    private String analysisDate;

    /** 样本编号 */
    private String subbarcode;

    /** 产品名称（analysis_data.product 的快照） */
    private String product;

    /** 产品ID（逻辑关联 product_config.id） */
    private Long productId;

    /** 产品描述 */
    private String productDesc;

    /** 解读癌种ID */
    private Long analysisDiseaseId;

    /** 解读癌种 */
    private String analysisDisease;

    /** LIMS 癌种 */
    private String cancerType;

    /** 化疗癌种 */
    private String chemDisease;

    /** 报告类型 */
    private String reportType;

    /** 原始 DOCX 报告路径 */
    private String reportFileRawPath;

    /** 报告（PDF）路径 */
    private String reportFilePath;

    /** 子报告路径 */
    private String subReportFilePath;

    /** 模板ID（逻辑关联 report_template.template_id） */
    private Long templateId;

    /** 报告模板编码 */
    private String template;

    /** 正式生成 JSON 文件路径 */
    private String reportJsonPath;

    /** 报告生成人（sys_user.user_id） */
    private Long reportGeneratedBy;

    /** 报告生成时间 */
    private Date reportGeneratedAt;

    /** 报告复核人（sys_user.user_id） */
    private Long reportCheckedBy;

    /** 报告审核时间 */
    private Date reportCheckedAt;

    /** 报告发送人（sys_user.user_id，非用户名） */
    private Long reportSentBy;

    /** 报告发放时间 */
    private Date reportSentAt;

    /** 报告状态（int 状态码，取值见 {@link org.dromara.report.utils.ReportStatus}） */
    private Integer reportStatus;

    /** 备注（含最新驳回原因） */
    private String comment;

    /** 逻辑删除标志 */
    @TableLogic
    private String delFlag;
}
