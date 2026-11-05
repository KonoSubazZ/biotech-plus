package org.dromara.test.report;

import org.dromara.report.domain.vo.PreviewVariantVo;
import org.dromara.report.service.impl.preview.ReportVariantEdits;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * 报告位点人工修改（report_variant_edits）单测。
 * <p>
 * 只跑本类：mvn -Plocal -DskipTests=false -Dtest=ReportVariantEditsTest test
 *
 * @author <你的名字>
 */
@Tag("local")
@DisplayName("报告位点人工修改")
public class ReportVariantEditsTest {

    @Test
    @DisplayName("键 = gene_variant_sourceType:sourceId，同报告内不重复")
    void keyIsUnique() {
        Assertions.assertEquals("BRCA1_A2825E_CR_ALL:24",
            ReportVariantEdits.key("BRCA1", "A2825E", "CR_ALL", 24L));
        Assertions.assertEquals("BRCA1_A2825E_CR_ALL:24",
            ReportVariantEdits.key(" BRCA1 ", " A2825E ", "CR_ALL", 24L));

        // CNV 同基因同 variant、不同源行：靠 id 区分
        Assertions.assertEquals("BCL2L1_Amplification_CNV:5",
            ReportVariantEdits.key("BCL2L1", "Amplification", "CNV", 5L));
        Assertions.assertEquals("BCL2L1_Amplification_CNV:6",
            ReportVariantEdits.key("BCL2L1", "Amplification", "CNV", 6L));

        // sourceType 不能省：不同源表 id 会撞
        Assertions.assertNotEquals(ReportVariantEdits.key("X", "y", "CR_ALL", 5L),
            ReportVariantEdits.key("X", "y", "CNV", 5L));
    }

    @Test
    @DisplayName("merge：逐字段覆盖、空说明删说明、空对象删整条键")
    void mergePatches() {
        String key = "BRCA1_A2825E_CR_ALL:24";
        String first = ReportVariantEdits.merge(null, key,
            new ReportVariantEdits.Edit(List.of(1L, 2L, 1L), 1, "人工说明"));
        Assertions.assertEquals(List.of(1L, 2L),
            ReportVariantEdits.parse(first).get(key).getParentMutationIds());
        Assertions.assertEquals(1, ReportVariantEdits.parse(first).get(key).getClinicalSignificance());
        Assertions.assertEquals("人工说明",
            ReportVariantEdits.parse(first).get(key).getMutationDescription());

        // 只改临床意义：父靶点/说明保留
        String second = ReportVariantEdits.merge(first, key, new ReportVariantEdits.Edit(null, 2, null));
        Assertions.assertEquals(2, ReportVariantEdits.parse(second).get(key).getClinicalSignificance());
        Assertions.assertEquals(2, ReportVariantEdits.parse(second).get(key).getParentMutationIds().size());

        // 空说明 = 删说明；空数组 = 取消改靶（保留空数组这个明确值）
        String third = ReportVariantEdits.merge(second, key, new ReportVariantEdits.Edit(List.of(), null, ""));
        ReportVariantEdits.Edit edit = ReportVariantEdits.parse(third).get(key);
        Assertions.assertNull(edit.getMutationDescription());
        Assertions.assertTrue(edit.getParentMutationIds().isEmpty());
        Assertions.assertEquals(2, edit.getClinicalSignificance());

        // 三个字段都为 null = 不改（保留当前值）
        String unchanged = ReportVariantEdits.merge(third, key, new ReportVariantEdits.Edit(null, null, null));
        Assertions.assertNotNull(ReportVariantEdits.parse(unchanged).get(key));
    }

    @Test
    @DisplayName("parse：脏 JSON 返回空、越界临床意义丢弃")
    void parseSanitizes() {
        Assertions.assertTrue(ReportVariantEdits.parse("not-json").isEmpty());
        Assertions.assertTrue(ReportVariantEdits.parse(null).isEmpty());
        Map<String, ReportVariantEdits.Edit> map = ReportVariantEdits.parse(
            "{\"BRCA1_A2825E_CR_ALL:24\":{\"clinicalSignificance\":9}}");
        Assertions.assertTrue(map.isEmpty(), "越界值且无其它字段 → 整条丢弃");
    }

    @Test
    @DisplayName("说明覆盖：来源=历史的行不覆盖")
    void descriptionSkipsHistoryRows() {
        PreviewVariantVo kb = item(24L, false, "默认甲。", null);
        PreviewVariantVo history = item(25L, true, "默认乙。", null);
        Map<String, ReportVariantEdits.Edit> edits = Map.of(
            ReportVariantEdits.key(kb), new ReportVariantEdits.Edit(null, null, "人工甲"),
            ReportVariantEdits.key(history), new ReportVariantEdits.Edit(null, null, "人工乙"));

        ReportVariantEdits.applyDescription(List.of(kb, history), edits);

        Assertions.assertEquals("人工甲", kb.getMutationDescription());
        Assertions.assertTrue(kb.getMutationDescriptionEdited());
        Assertions.assertEquals("默认乙。", history.getMutationDescription());
        Assertions.assertFalse(history.getMutationDescriptionEdited());
    }

    private static PreviewVariantVo item(long id, boolean fromHistory, String hgvs, String locus) {
        PreviewVariantVo item = new PreviewVariantVo();
        item.setSourceType("CR_ALL");
        item.setSourceId(id);
        item.setGene("BRCA1");
        item.setVariant("A2825E");
        item.setFromHistory(fromHistory);
        item.setMutationExplanation(hgvs);
        item.setVariantDescription(locus);
        return item;
    }
}
