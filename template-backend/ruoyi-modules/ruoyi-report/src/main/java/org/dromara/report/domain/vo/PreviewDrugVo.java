package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 报告预览：单条药物证据（对应设计书 match_result 里的 Item）
 *
 * @author <你的名字>
 */
@Data
public class PreviewDrugVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 药物名（NKB drug.drug_name_chinese） */
    private String drugName;

    /** 药物英文名 */
    private String drugNameEn;

    /** 证据所属癌种（NKB disease.disease_name_chinese） */
    private String disease;

    /** 是否直接靶向（variant_drug_annotation.direct_target_or_not） */
    private String directTarget;

    /** 证据类型（evidence_type 字典） */
    private String evidenceType;

    /** 证据等级（evidence_ranking / variant_evidence_ranking 字典） */
    private String evidenceRanking;

    /** 证据分期（evidence_phase 字典） */
    private String evidencePhase;

    /** 用药关系（relationship 字典） */
    private String relationship;

    /** 证据说明（中文） */
    private String annotation;

    /** 备注 */
    private String comment;
}
