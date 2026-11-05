package org.dromara.report.service;

import org.dromara.report.domain.bo.InterpretationPreviewBo;
import org.dromara.report.domain.bo.InterpretationVariantEditBo;
import org.dromara.report.domain.vo.InterpretationPreviewVo;
import org.dromara.report.domain.vo.NkbVariantNodeVo;
import org.dromara.report.domain.vo.ReportTemplateData;

import java.util.List;
import java.util.Map;

/**
 * 报告预览（设计书 §8：只组装返回，不落库、不写预览文件）
 *
 * @author <你的名字>
 */
public interface IInterpretationPreviewService {

    /**
     * 组装报告 JSON（公共字段 + report_template.module_code 驱动的个性化模块）
     *
     * @param bo 入参（analysisId / reportId / templateId：人工选模板，可空）
     * @return 报告 JSON（ReportTemplateData）
     */
    ReportTemplateData buildPreview(InterpretationPreviewBo bo);

    /**
     * 页面预览：报告已生成过、且本次生效模板与制品一致时，直接读最近一次生成的 JSON 制品
     * （多人共享、免重复匹配）；否则实时查库组装。入参 forceRealtime=true 强制实时组装。
     *
     * @param bo 入参（analysisId / reportId / templateId / forceRealtime）
     * @return 预览结果（含数据来源标记）
     */
    InterpretationPreviewVo preview(InterpretationPreviewBo bo);

    /**
     * 冻结匹配历史：报告<b>审核通过</b>时调用，<b>只读已生成的报告 JSON</b> 派生 history_* 与历史检出，
     * 不再现场重算（报告内容 = 历史冻结结果）。读不到制品 → 拒绝审核通过。
     *
     * @param reportId   报告ID
     * @param analysisId 分析数据ID
     */
    void freezeFromArtifact(Long reportId, Long analysisId);

    /**
     * 报告生成参数 · 解读癌种候选（NKB 疾病分类）。
     *
     * @return [{diseaseId, diseaseName}]
     */
    List<Map<String, Object>> diseaseOptions();

    /**
     * 报告生成参数 · 解读人候选（启用中的系统用户）。
     *
     * @return [{userId, userName, nickName}]
     */
    List<Map<String, Object>> analyzerOptions();

    /**
     * 人工选模板用：报告产品对应的候选模板列表（默认模板排最前）。
     *
     * @param analysisId 分析数据ID
     * @param reportId   报告ID
     * @return 每项 {templateId, templateName, templateVersion, defaultTemplate}；产品没配模板返回空列表
     */
    List<Map<String, Object>> templateOptions(Long analysisId, Long reportId);

    /**
     * 位点人工修改（父靶点 / 胚系临床意义 / 突变说明），报告粒度存储。
     * <p>
     * 只有未入库位点可改；来源=历史只读。写 {@code analysis_report.report_variant_edits}，
     * 生成报告时解析进 JSON；不写 history_*、不写源位点表。
     *
     * @param bo 入参（analysisId / reportId / sourceType / sourceId / 三个可选字段）
     */
    void updateVariantEdits(InterpretationVariantEditBo bo);

    /**
     * 改靶候选：按关键词查 NKB 位点节点（Approved），供人工指定父级
     *
     * @param keyword 关键词（基因符号 / 节点名片段）
     * @return 候选节点列表
     */
    List<NkbVariantNodeVo> searchParentNodes(String keyword);
}
