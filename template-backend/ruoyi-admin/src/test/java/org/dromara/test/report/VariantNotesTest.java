package org.dromara.test.report;

import org.dromara.report.domain.vo.PreviewVariantVo;
import org.dromara.report.service.impl.preview.VariantNotes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * 位点「突变说明」默认文本单测（en7 口径：HGVS 译文 + 位点说明直接拼接）。
 * <p>
 * 只跑本类：mvn -Plocal -DskipTests=false -Dtest=VariantNotesTest test
 *
 * @author <你的名字>
 */
@Tag("local")
@DisplayName("突变说明默认文本")
public class VariantNotesTest {

    @Test
    @DisplayName("默认说明 = HGVS 译文 + 位点说明（直接拼接）")
    void composeWithLocus() {
        PreviewVariantVo item = item("甲。", "乙。", "兜底。");
        Assertions.assertEquals("甲。乙。", VariantNotes.compose(item));
    }

    @Test
    @DisplayName("NKB 位点说明缺失时用四段兜底 description")
    void composeFallback() {
        PreviewVariantVo item = item("甲。", null, "兜底。");
        Assertions.assertEquals("甲。兜底。", VariantNotes.compose(item));
    }

    @Test
    @DisplayName("全部为空返回 null")
    void composeEmpty() {
        Assertions.assertNull(VariantNotes.compose(item(null, null, null)));
    }

    private static PreviewVariantVo item(String hgvs, String locus, String fallback) {
        PreviewVariantVo item = new PreviewVariantVo();
        item.setMutationExplanation(hgvs);
        item.setVariantDescription(locus);
        item.setDescription(fallback);
        return item;
    }
}
