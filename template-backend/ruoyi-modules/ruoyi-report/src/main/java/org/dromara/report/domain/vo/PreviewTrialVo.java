package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 报告预览：临床试验证据（第二类证据，与「药物证据」并列）。
 * <p>
 * 对齐 en7 {@code AnalysisReportDao.getClinicalTrial} + 实际报告的「临床试验信息」表，
 * 展示列：ID / 临床试验名称 / 肿瘤类型 / 阶段 / 药物 / 地点。
 * <p>
 * 只取「招募中（Recruiting）/ 邀请入组（Enrolling by invitation）」且已审核的试验，
 * 并剔除 {@code clinical_trial_exclude_disease} 中本癌种排除的试验；
 * 说明类字段**不进 match_result 冻结**，每次预览重新查。
 *
 * @author <你的名字>
 */
@Data
public class PreviewTrialVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 试验登记号（NCTxxxxxxxx，报告的「ID」列） */
    private String trialId;

    /** 临床试验名称（official_title_chinese） */
    private String title;

    /** 肿瘤类型（condition_chinese） */
    private String trialCondition;

    /** 阶段原值（Phase II 等） */
    private String phase;

    /** 阶段中文（II期 等，en7 translatePhase） */
    private String phaseText;

    /** 地点（location_chinese） */
    private String location;

    /** 该试验对应的药物（同一条证据上的药名） */
    private String drugName;

    /** 证据注释ID（用于前端把试验挂回对应证据，可空） */
    private Long annotationId;
}
