package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 报告预览：单条药物证据（对齐 report_en7 的 evidence 行）
 * <p>
 * 与 en7 的对应关系：
 * <ul>
 *   <li>{@code approveRange} = en7 的 {@code approve_range}：1-4 获益 A/B/C/D，5-8 耐药 A/B/C/D，9 其他（不输出）</li>
 *   <li>{@code levelName} = 由 approveRange 推导出的 A/B/C/D</li>
 *   <li>{@code relation} = BENEFIT / RESISTANT（对应 en7 {@code buildDrugAuditList} 的 relation）</li>
 *   <li>其余字段直接取 NKB 注释行（癌种、分期、关系、说明、是否需其他检测等）</li>
 * </ul>
 *
 * @author <你的名字>
 */
@Data
public class PreviewDrugVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 注释ID（en7 用它查临床试验数 give） */
    private Long annotationId;

    /** 药物ID（去重键之一） */
    private Long drugId;

    /** 命中的知识库节点ID（自身或父级） */
    private Long mutationId;

    /** 命中的节点名（如 V559D / Exon11 Mutation / Active Mutation） */
    private String nodeName;

    /** 是否来自「其他癌种获批药」（en7：一律降格为 C 级） */
    private Boolean fromOtherCancer;

    /** 药物名（中文） */
    private String drugName;

    /** 药物英文名 */
    private String drugNameEn;

    /** 证据癌种ID */
    private Long diseaseId;

    /** 证据癌种名 */
    private String disease;

    /** 用药关系（中文展示：敏感性增加/有益的/抗药性） */
    private String relationship;

    /** 关系字典ID（1 敏感性增加 / 4 有益的 / 6 抗药性）；判断耐药用它，别拿中文比 */
    private Integer relationshipId;

    /** 关系分类：BENEFIT / RESISTANT */
    private String relation;

    /** 是否直接靶向 */
    private String directTarget;

    /** 证据类型（治疗） */
    private String evidenceType;

    /** 证据分期中文（指南推荐/获批上市/临床试验III期…） */
    private String evidencePhase;

    /** 证据分期ID（等级计算的输入） */
    private Integer evidencePhaseId;

    /** 证据等级中文（已知/有报道/可能/预测/不确定） */
    private String evidenceRanking;

    /** 等级码：1-4 获益A-D，5-8 耐药A-D，9 其他 */
    private Integer approveRange;

    /** 等级名：A/B/C/D（由 approveRange 推导，9 时为空） */
    private String levelName;

    /** give 判定结果：2 父级癌种有临床试验 / 1 has_previous_clinical_result 为空或 Y / 0 其他 */
    private String give;

    /** 是否需要其他检测（en7 用它标红） */
    private String otherTestRequired;

    /** 既往是否有临床结果（Y / 空 / N；en7 getGive 用它判 1 或 0） */
    private String hasPreviousClinicalResult;

    /** 获批机构（approved_drug.approving_agency） */
    private String approvingAgency;

    /**
     * 获批上市(phase 24)说明：`approved_drug_evw.approval_description_chinese`。
     * 实际报告的「循证医学信息」列在获批上市行用的就是它（en7 模块化链路 §11.3）。
     */
    private String approvalDescription;

    /**
     * 指南推荐(phase 23)说明：按 NCCN/CSCO 拼好的中文串。
     * en7 getVarDrugNote 对获益的 phase 23 行**不用** annotation，改取 `guideline_drug_evw.guideline_description`。
     */
    private String guidelineDescription;

    /** 指南类型（NCCN / CSCO），界面加标签用 */
    private List<String> guidelineTypes;

    /** 证据说明（中文，= variant_drug_annotation.annotation_chinese）；无指南/批准说明时界面就显示它 */
    private String annotation;

    /** 备注 */
    private String comment;
}
