package org.dromara.report.service;

import org.dromara.report.domain.bo.InterpretationGermlineSignificanceBo;
import org.dromara.report.domain.bo.InterpretationPreviewBo;
import org.dromara.report.domain.bo.InterpretationTargetBo;
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
     * 人工选模板用：报告产品对应的候选模板列表（默认模板排最前）。
     *
     * @param analysisId 分析数据ID
     * @param reportId   报告ID
     * @return 每项 {templateId, templateName, templateVersion, defaultTemplate}；产品没配模板返回空列表
     */
    List<Map<String, Object>> templateOptions(Long analysisId, Long reportId);

    /**
     * 人工确认胚系五级临床意义（保存前要求该位点已经建立匹配历史）
     *
     * @param bo 入参（analysisId / reportId / sourceId / clinicalSignificance）
     */
    void updateGermlineSignificance(InterpretationGermlineSignificanceBo bo);

    /**
     * 改靶（体细胞 / 胚系共用）：给位点人工指定一个或多个 NKB 父级节点，
     * 写入源位点表 parent_mutation_id（逗号分隔）→ 产生新的匹配键，下次预览走继承逻辑。
     * <p>
     * 保存前按 en7 口径校验「改靶后必须能出药物证据」，出不来直接拒绝。
     *
     * @param bo 入参（analysisId / reportId / sourceType / sourceId / parentMutationIds，空列表 = 取消改靶）
     */
    void updateVariantTarget(InterpretationTargetBo bo);

    /**
     * 改靶候选：按关键词查 NKB 位点节点（Approved），供人工指定父级
     *
     * @param keyword 关键词（基因符号 / 节点名片段）
     * @return 候选节点列表
     */
    List<NkbVariantNodeVo> searchParentNodes(String keyword);
}
