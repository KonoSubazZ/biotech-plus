package org.dromara.report.service;

import org.dromara.report.domain.bo.InterpretationGermlineSignificanceBo;
import org.dromara.report.domain.bo.InterpretationPreviewBo;
import org.dromara.report.domain.bo.InterpretationTargetBo;
import org.dromara.report.domain.vo.InterpretationPreviewVo;

/**
 * 报告预览（设计书 §8：只组装返回，不落库、不写预览文件）
 *
 * @author <你的名字>
 */
public interface IInterpretationPreviewService {

    /**
     * 组装预览 JSON
     *
     * @param bo 入参（analysisId / reportId / templateCode）
     * @return 预览 JSON
     */
    InterpretationPreviewVo buildPreview(InterpretationPreviewBo bo);

    /**
     * 人工确认胚系五级临床意义（保存前要求该位点已经建立匹配历史）
     *
     * @param bo 入参（analysisId / reportId / sourceId / clinicalSignificance）
     */
    void updateGermlineSignificance(InterpretationGermlineSignificanceBo bo);

    /**
     * 胚系改靶：更新人工父级，产生新的匹配键（下次预览会走继承逻辑）
     *
     * @param bo 入参（analysisId / sourceId / parentMutationId）
     */
    void updateGermlineTarget(InterpretationTargetBo bo);
}
