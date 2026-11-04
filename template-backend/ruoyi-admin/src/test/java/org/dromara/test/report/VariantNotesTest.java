package org.dromara.test.report;

import org.dromara.report.domain.vo.PreviewVariantVo;
import org.dromara.report.service.impl.preview.VariantNotes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 位点「突变说明」人工覆盖单测（报告粒度 JSON：analysis_report.report_variant_notes）。
 * <p>
 * 只跑本类：mvn -Plocal -DskipTests=false -Dtest=VariantNotesTest test
 *
 * @author <你的名字>
 */
@Tag("local")
@DisplayName("位点说明人工覆盖")
public class VariantNotesTest {

    private static PreviewVariantVo item(String sourceType, long sourceId, String hgvs, String locus,
                                         String fallback) {
        PreviewVariantVo item = new PreviewVariantVo();
        item.setSourceType(sourceType);
        item.setSourceId(sourceId);
        item.setMutationExplanation(hgvs);
        item.setVariantDescription(locus);
        item.setDescription(fallback);
        return item;
    }

    @Test
    @DisplayName("默认说明 = HGVS 译文 + 位点说明（直接拼接，en7 口径）")
    void composeDefault() {
        Assertions.assertEquals("甲。乙。", VariantNotes.compose(item("CR_ALL", 1L, "甲。", "乙。", "兜底。")));
    }

    @Test
    @DisplayName("NKB 无位点说明时用四段兜底 description")
    void composeFallback() {
        Assertions.assertEquals("甲。兜底。", VariantNotes.compose(item("CR_ALL", 1L, "甲。", null, "兜底。")));
        Assertions.assertEquals("甲。兜底。", VariantNotes.compose(item("CR_ALL", 1L, "甲。", "  ", "兜底。")));
    }

    @Test
    @DisplayName("三类说明都没有 → null（模板 or '' 兜住）")
    void composeEmpty() {
        Assertions.assertNull(VariantNotes.compose(item("CR_ALL", 1L, null, null, null)));
    }

    @Test
    @DisplayName("有覆盖用覆盖并打 edited；没覆盖用默认且 edited=false")
    void applyOverride() {
        List<PreviewVariantVo> items = new ArrayList<>();
        items.add(item("CR_ALL", 1L, "甲。", "乙。", null));
        items.add(item("SNP_INDEL", 2L, "丙。", "丁。", null));

        Map<String, String> notes = new LinkedHashMap<>();
        notes.put("CR_ALL:1", "  人工改过的说明  ");
        VariantNotes.apply(items, notes);

        Assertions.assertEquals("人工改过的说明", items.get(0).getMutationDescription());
        Assertions.assertTrue(items.get(0).getMutationDescriptionEdited());
        Assertions.assertEquals("丙。丁。", items.get(1).getMutationDescription());
        Assertions.assertFalse(items.get(1).getMutationDescriptionEdited());
    }

    @Test
    @DisplayName("键按 来源类型:位点ID 精确匹配（不同来源同 ID 不串）")
    void applyKeyIsolation() {
        List<PreviewVariantVo> items = new ArrayList<>();
        items.add(item("CR_ALL", 7L, "甲。", "乙。", null));
        items.add(item("SNP_INDEL", 7L, "丙。", "丁。", null));
        VariantNotes.apply(items, Map.of("SNP_INDEL:7", "只改体细胞"));

        Assertions.assertEquals("甲。乙。", items.get(0).getMutationDescription());
        Assertions.assertEquals("只改体细胞", items.get(1).getMutationDescription());
    }

    @Test
    @DisplayName("merge：写入 / 覆盖 / 删除后整表为空返回 null")
    void merge() {
        String one = VariantNotes.merge(null, "CR_ALL", 1L, "  文本  ");
        Assertions.assertEquals(Map.of("CR_ALL:1", "文本"), VariantNotes.parse(one));

        String two = VariantNotes.merge(one, "CR_ALL", 2L, "第二条");
        Assertions.assertEquals(2, VariantNotes.parse(two).size());

        String replaced = VariantNotes.merge(two, "CR_ALL", 2L, "改第二条");
        Assertions.assertEquals("改第二条", VariantNotes.parse(replaced).get("CR_ALL:2"));

        Assertions.assertEquals(Map.of("CR_ALL:1", "文本"),
            VariantNotes.parse(VariantNotes.merge(replaced, "CR_ALL", 2L, " ")));
        Assertions.assertNull(VariantNotes.merge(one, "CR_ALL", 1L, null));
    }

    @Test
    @DisplayName("脏 JSON / 空值不炸，按全部默认处理")
    void parseBroken() {
        Assertions.assertTrue(VariantNotes.parse(null).isEmpty());
        Assertions.assertTrue(VariantNotes.parse("  ").isEmpty());
        Assertions.assertTrue(VariantNotes.parse("{oops").isEmpty());
        Assertions.assertTrue(VariantNotes.parse("[1,2,3]").isEmpty());
    }

    private static PreviewVariantVo artifactItem(String type, long id, String desc, boolean edited) {
        PreviewVariantVo item = item(type, id, "甲。", "乙。", null);
        item.setMutationDescription(desc);
        item.setMutationDescriptionEdited(edited);
        return item;
    }

    @Test
    @DisplayName("提交前校验：制品与覆盖表一致才放行")
    void matchesArtifactWhenSame() {
        List<PreviewVariantVo> items = new ArrayList<>();
        items.add(artifactItem("CR_ALL", 1L, "人工文本", true));
        items.add(artifactItem("SNP_INDEL", 2L, "默认说明", false));
        Assertions.assertTrue(VariantNotes.matchesArtifact(items, Map.of("CR_ALL:1", "人工文本")));

        List<PreviewVariantVo> plain = new ArrayList<>();
        plain.add(artifactItem("SNP_INDEL", 2L, "默认说明", false));
        Assertions.assertTrue(VariantNotes.matchesArtifact(plain, Map.of()));
    }

    @Test
    @DisplayName("提交前校验：生成后又改文本 / 生成后才新增 / 生成后又恢复默认 → 全部拒绝")
    void matchesArtifactWhenDrift() {
        List<PreviewVariantVo> stale = new ArrayList<>();
        stale.add(artifactItem("CR_ALL", 1L, "旧文本", true));
        Assertions.assertFalse(VariantNotes.matchesArtifact(stale, Map.of("CR_ALL:1", "新文本")));
        Assertions.assertFalse(VariantNotes.matchesArtifact(stale, Map.of()));

        List<PreviewVariantVo> notEditedYet = new ArrayList<>();
        notEditedYet.add(artifactItem("CR_ALL", 1L, "默认说明", false));
        Assertions.assertFalse(VariantNotes.matchesArtifact(notEditedYet, Map.of("CR_ALL:1", "生成后才加的文本")));
    }

    @Test
    @DisplayName("提交前校验：位点已不再报出时跳过，不误伤提交")
    void matchesArtifactSkipsMissingItem() {
        List<PreviewVariantVo> items = new ArrayList<>();
        items.add(artifactItem("SNP_INDEL", 2L, "默认说明", false));
        Assertions.assertTrue(VariantNotes.matchesArtifact(items, Map.of("CR_ALL:99", "已删位点的旧说明")));
    }
}
