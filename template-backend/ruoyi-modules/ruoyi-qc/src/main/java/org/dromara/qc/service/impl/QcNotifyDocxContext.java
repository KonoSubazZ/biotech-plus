package org.dromara.qc.service.impl;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.qc.domain.bo.QcFailedNotifyBo;
import org.dromara.qc.domain.vo.QcNotifySampleVo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 样本质控不合格通知：DOCX 渲染上下文 + 勾选项映射
 * <p>
 * 从 {@link QcFailedNotifyServiceImpl} 拆出来（单文件行数红线 500，那边加完湿实验分支就超了）。
 * 职责只有一件：把「表单 + 样本信息」翻成模板要的 27 个变量，以及把多选框翻成落库的中文名。
 * <p>
 * 模板里引用的变量：字符串一律给空串（避免渲染出 None），布尔一律给 true/false；
 * 勾选项在页面是多选框，落库存中文名（顿号分隔），渲染时模板用 {@code {{ box(flag) }}} 出 ☑/□。
 */
final class QcNotifyDocxContext {

    /** 多选落库分隔符（中文顿号，与页面展示一致） */
    private static final String LIST_SEPARATOR = "、";

    /** 通知单里「样本质控环节」勾选项：中文名按原文案，便于直接看库 */
    static final List<Option> STAGES = List.of(
        new Option("样本接收", QcFailedNotifyBo::getStageReceive),
        new Option("病理质控", QcFailedNotifyBo::getStagePathology),
        new Option("核酸质控", QcFailedNotifyBo::getStageNucleic),
        new Option("文库质控", QcFailedNotifyBo::getStageLibrary),
        new Option("测序质控", QcFailedNotifyBo::getStageSequencing));

    /** 通知单里「是否终止检测」勾选项 */
    static final List<Option> MEASURES = List.of(
        new Option("是", QcFailedNotifyBo::getTerminateYes),
        new Option("暂停相关检测，待申办方确认后处理", QcFailedNotifyBo::getTerminatePause));

    /** 通知单里「建议处理办法」勾选项 */
    static final List<Option> REMEDIES = List.of(
        new Option("重新采集符合要求的样本重新寄送", QcFailedNotifyBo::getRemedyResample),
        new Option("更换肿瘤含量较高的蜡块重新切片后寄送", QcFailedNotifyBo::getRemedyWaxBlock),
        new Option("选取组织面积较大或较多的切片重新寄送", QcFailedNotifyBo::getRemedyMoreSlices),
        new Option("重新送样", QcFailedNotifyBo::getRemedyResend),
        new Option("其他", QcFailedNotifyBo::getRemedyOther));

    private QcNotifyDocxContext() {
    }

    /** 勾选项 = 中文名 + 取布尔值的方法引用 */
    record Option(String label, Function<QcFailedNotifyBo, Boolean> selected) {
    }

    /** 模板上下文：按模板里的变量名逐个填，缺的一律空串 */
    static Map<String, Object> build(QcFailedNotifyBo bo, QcNotifySampleVo sample) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("studyNo", text(bo.getStudyNo()));
        context.put("projectNo", text(bo.getProjectNo()));
        context.put("participantNo", text(bo.getParticipantNo()));
        context.put("sampleNo", text(sample.getSubbarcode()));
        context.put("center", text(bo.getCenter()));
        context.put("receiveDate", text(bo.getReceiveDate()));
        context.put("sampleType", text(bo.getSampleType()));
        context.put("sampleCount", text(bo.getSampleCount()));
        context.put("product", text(bo.getProduct()));
        context.put("stageReceive", flag(bo.getStageReceive()));
        context.put("stagePathology", flag(bo.getStagePathology()));
        context.put("stageNucleic", flag(bo.getStageNucleic()));
        context.put("stageLibrary", flag(bo.getStageLibrary()));
        context.put("stageSequencing", flag(bo.getStageSequencing()));
        context.put("reason", text(bo.getReason()));
        context.put("terminateYes", flag(bo.getTerminateYes()));
        context.put("terminatePause", flag(bo.getTerminatePause()));
        context.put("remedyResample", flag(bo.getRemedyResample()));
        context.put("remedyWaxBlock", flag(bo.getRemedyWaxBlock()));
        context.put("remedyMoreSlices", flag(bo.getRemedyMoreSlices()));
        context.put("remedyResend", flag(bo.getRemedyResend()));
        context.put("remedyOther", flag(bo.getRemedyOther()));
        context.put("remedyOtherText", text(bo.getRemedyOtherText()));
        context.put("reporter", text(bo.getReporter()));
        context.put("reportTime", text(bo.getReportTime()));
        context.put("reviewer", text(bo.getReviewer()));
        context.put("reviewTime", text(bo.getReviewTime()));
        return context;
    }

    /** 勾选中的项拼成中文名（顿号分隔），用于落库与检索 */
    static String joinSelected(QcFailedNotifyBo bo, List<Option> options) {
        List<String> labels = new ArrayList<>();
        for (Option option : options) {
            if (Boolean.TRUE.equals(option.selected().apply(bo))) {
                labels.add(option.label());
            }
        }
        return String.join(LIST_SEPARATOR, labels);
    }

    private static String text(String value) {
        return StringUtils.blankToDefault(value, "");
    }

    private static boolean flag(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}
