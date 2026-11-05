package org.dromara.test.report;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.domain.dto.PreviewContext;
import org.dromara.report.domain.vo.PreviewVariantVo;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.service.impl.NkbDrugMatcher;
import org.dromara.report.service.impl.preview.VariantDescriptionEnricher;
import org.dromara.report.service.impl.preview.VariantMatcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 审核通过「从报告 JSON 冻结历史」的单测（不连库，mapper 全 mock）。
 *
 * @author <你的名字>
 */
@Tag("local")
@DisplayName("审核通过按制品冻结历史")
public class HistoryFreezeFromArtifactTest {

    private final InterpretationMapper mapper = Mockito.mock(InterpretationMapper.class);

    private final VariantMatcher matcher = new VariantMatcher(mapper,
        Mockito.mock(NkbDrugMatcher.class), Mockito.mock(VariantDescriptionEnricher.class));

    @Test
    @DisplayName("未命中历史：按制品字段插入 history_germline（含临床意义）")
    void insertsFromArtifact() {
        Mockito.when(mapper.selectGermlineHistory(Mockito.anyString())).thenReturn(null);
        PreviewVariantVo item = item(1);

        matcher.freezeFromItems(context(), new LinkedHashMap<>(), List.of(item), true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> row = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(mapper).insertGermlineHistory(row.capture());
        Assertions.assertEquals("BRCA1", row.getValue().get("gene"));
        Assertions.assertEquals(1, row.getValue().get("clinicalSignificance"));
        Assertions.assertEquals(14L, row.getValue().get("sourceReportId"));
        Assertions.assertEquals("NOT_MATCHED", row.getValue().get("matchStatus"));
    }

    @Test
    @DisplayName("命中历史且结果一致：只登记复用，不插入、不覆盖")
    void reusesWhenSame() {
        Map<String, Object> history = history(expectedMatchResult(), 1);
        Mockito.when(mapper.selectGermlineHistory(Mockito.anyString())).thenReturn(history);

        matcher.freezeFromItems(context(), new LinkedHashMap<>(), List.of(item(1)), true);

        Mockito.verify(mapper, Mockito.never()).insertGermlineHistory(Mockito.anyMap());
        Mockito.verify(mapper).touchHistoryReuse(Mockito.anyString(), Mockito.eq(true));
    }

    @Test
    @DisplayName("B1：命中历史但结果不一致 → 阻止审核通过")
    void blocksWhenDifferent() {
        Map<String, Object> different = expectedMatchResult();
        different.put("inNkb", true);
        different.put("matchedNode", "别的节点");
        different.put("variationClass", "II类");
        Map<String, Object> history = history(different, 3);
        Mockito.when(mapper.selectGermlineHistory(Mockito.anyString())).thenReturn(history);

        ServiceException e = Assertions.assertThrows(ServiceException.class,
            () -> matcher.freezeFromItems(context(), new LinkedHashMap<>(), List.of(item(1)), true));
        Assertions.assertTrue(e.getMessage().contains("已被其他报告冻结且结果不一致"));
        Mockito.verify(mapper, Mockito.never()).insertGermlineHistory(Mockito.anyMap());
    }

    private static PreviewContext context() {
        PreviewContext pc = new PreviewContext();
        pc.setAnalysisId(34L);
        pc.setReportId(14L);
        pc.setProjectNo("NG0101");
        pc.setProjectCode("novopm2_blo_1238");
        pc.setDisease("乳腺癌");
        pc.setGender("FEMALE");
        pc.setCustomer("测试医院");
        return pc;
    }

    private static PreviewVariantVo item(int significance) {
        PreviewVariantVo item = new PreviewVariantVo();
        item.setSourceType("CR_ALL");
        item.setSourceId(24L);
        item.setGene("BRCA1");
        item.setVariant("A2825E");
        item.setOriVariant("NM_007294.4 exon23 c.5470_5477del p.Ile1824Aspfs");
        item.setMutationType("G");
        item.setZygosity("het");
        item.setMatchStatus("NOT_MATCHED");
        item.setVariationClass("III类");
        item.setClinicalSignificance(significance);
        item.setInNkb(false);
        item.setDescription("d");
        item.setDrugGroups(Map.of());
        item.setDrugAuditList(List.of());
        item.setDrugMatch(List.of());
        return item;
    }

    private static Map<String, Object> expectedMatchResult() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inNkb", false);
        result.put("matchedNode", null);
        result.put("mutationId", null);
        result.put("effectText", null);
        result.put("variationClass", "III类");
        result.put("description", "d");
        result.put("evidence", List.of());
        result.put("drugGroups", Map.of());
        result.put("drugAuditList", List.of());
        return result;
    }

    private static Map<String, Object> history(Map<String, Object> matchResult, int significance) {
        Map<String, Object> history = new LinkedHashMap<>();
        history.put("match_result", org.dromara.report.service.impl.PreviewSupport.toJson(matchResult));
        history.put("match_status", "NOT_MATCHED");
        history.put("variation_class", "III类");
        history.put("clinical_significance", significance);
        return history;
    }
}
