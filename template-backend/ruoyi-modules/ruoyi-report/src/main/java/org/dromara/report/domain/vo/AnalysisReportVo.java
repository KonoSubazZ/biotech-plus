package org.dromara.report.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.report.domain.AnalysisReport;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 分析报告视图对象 analysis_report
 * <p>
 * 「报告解读」各页共用的报告头信息：列表页状态列、详情页顶部摘要、审核/发送页读写状态都用它。
 *
 * @author <你的名字>
 */
@Data
// mapstruct-plus：生成 实体 → Vo 的转换器。BaseMapperPlus.selectVoById / selectVoList 靠它，
// 少了这个注解运行期会抛 ConvertException: cannot find converter from AnalysisReport to AnalysisReportVo
@AutoMapper(target = AnalysisReport.class)
public class AnalysisReportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 报告ID */
    private Long reportId;

    private Integer reportVersion;
    private Integer reportGenerationNo;
    private Long reportReviewerId;
    private Long sourceReportId;
    private Long reportReviewClaimedBy;
    private String reportName;

    /** 所属分析批次 */
    private Long analysisId;

    /** 分析日期（YYYYMMDD） */
    private String analysisDate;

    /** 样本编号 */
    private String subbarcode;

    /** 生信产品编码（= analysis_data.product_bio） */
    private String productBio;

    /** 产品ID（product_config.id） */
    private Long productId;

    /** 项目编号（= cms_sample.project_no） */
    private String projectNo;

    /** 上传批次目录名（= analysis_data.analysis_batch_date） */
    private String analysisBatchDate;

    /** 解读癌种 */
    private String analysisDisease;

    /** 模板ID */
    private Long templateId;

    /** 报告模板编码 */
    private String template;

    /** 状态 */
    /** 报告状态（int 码，与实体 reportStatus / 库 report_status 同名同类型） */
    private Integer reportStatus;

    /** 备注 / 驳回原因 */
    private String comment;

    /** 报告产出人 */
    private String reportGeneratedBy;

    /** 报告生成时间 */
    private Date reportGeneratedAt;

    /** 报告审核人 */
    private String reportCheckedBy;

    /** 报告审核时间 */
    private Date reportCheckedAt;

    /** 报告发放人 */
    private String reportSentBy;

    /** 报告发放时间 */
    private Date reportSentAt;

    /** 创建时间 */
    private Date createTime;
}
