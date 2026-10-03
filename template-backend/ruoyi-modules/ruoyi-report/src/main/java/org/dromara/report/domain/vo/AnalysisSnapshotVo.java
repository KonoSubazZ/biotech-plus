package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分析批次快照（analysis_data 的少量字段）
 * <p>
 * 「进入解读」创建报告记录时，把 analysis_data 的样本/产品信息复制到 analysis_report，
 * 避免列表页为了取这几个字段再查一次批次表。
 *
 * @author <你的名字>
 */
@Data
public class AnalysisSnapshotVo implements Serializable {

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

    /** 产品ID（product_config.id，可能为空：scanner 只写产品名） */
    private Long productId;

    /** 解读人员 */
    private String analyzer;
}
