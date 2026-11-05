package org.dromara.report.service.impl.preview;

import org.dromara.report.domain.vo.PreviewVariantVo;
import org.springframework.util.StringUtils;

/**
 * 位点「突变说明」的默认文本（en7 口径）。
 * <p>
 * 默认 = HGVS 中文译文 + NKB 位点说明，<b>直接拼接成一段</b>；NKB 位点说明缺失时用四段兜底
 * {@code description}。人工修改不在这里：见 {@link ReportVariantEdits}（报告级 edits）。
 * 每次预览/生成实时计算，不进 history_* 冻结。
 *
 * @author <你的名字>
 */
public final class VariantNotes {

    private VariantNotes() {
    }

    /**
     * 默认说明（en7 口径）：HGVS 中文译文 + 位点说明，直接拼接；
     * NKB 位点说明缺失时用四段兜底 {@code description}。
     *
     * @param item 位点（需已由 {@link VariantDescriptionEnricher} 填好三类说明）
     * @return 默认说明；都没有时 null
     */
    public static String compose(PreviewVariantVo item) {
        String hgvs = item.getMutationExplanation() == null ? "" : item.getMutationExplanation();
        String locus = StringUtils.hasText(item.getVariantDescription())
            ? item.getVariantDescription()
            : (item.getDescription() == null ? "" : item.getDescription());
        String text = hgvs + locus;
        return StringUtils.hasText(text) ? text : null;
    }
}
