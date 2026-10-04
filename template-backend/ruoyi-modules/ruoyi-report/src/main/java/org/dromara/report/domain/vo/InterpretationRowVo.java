package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 报告解读列表行（analysis_data ⟕ 最新 analysis_report）
 * <p>
 * 一行 = 一个分析批次（analysis_data）+ 该批次最新一份报告（可能为空 = 尚未解读）。
 * 字段名与前端 typings 的 Api.Report.InterpretationRow 一一对应。
 *
 * @author <你的名字>
 */
@Data
public class InterpretationRowVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分析数据ID */
    private Long analysisId;

    /** 分析日期（YYYYMMDD） */
    private String analysisDate;

    /** 样本编号 */
    private String subbarcode;

    /** 患者编号 */
    private String barcode;

    /** 产品名称 */
    private String product;

    /** 产品ID（product_config.id） */
    private Long productId;

    /** 解读人员 */
    private String analyzer;

    /** 集群驱动状态：DRIVING / LOADED / PARTIAL */
    private String driveStatus;

    /** 驱动执行时间 */
    private Date driveExecutedAt;

    /** 报告ID（为空表示该批次还没点过「解读」） */
    private Long reportId;

    /** 报告状态：INTERPRETING / PENDING_REVIEW / APPROVED / REJECTED / SENT */
    private String reportStatus;

    /** 解读癌种 */
    private String analysisDisease;

    /** 模板ID */
    private Long templateId;

    /** 报告模板编码 */
    private String template;

    /** 报告产出人 */
    private String reportGeneratedBy;

    /** 报告生成时间 */
    private Date reportGeneratedAt;
}
