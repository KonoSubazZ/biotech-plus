package org.dromara.test.report;

import org.dromara.report.domain.CmsSample;
import org.dromara.report.domain.dto.ReportModuleContext;
import org.dromara.report.domain.vo.ReportTemplateData;
import org.dromara.report.service.template.CommonReportModuleAssembler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * sampleInfo 的订单维度汇总（sampleTypes / subbarcodes）单测。
 * <p>
 * 双样本（组织 + 对照血）在 cms_sample 是同一 order_no 的两行；报告只用组织那条，
 * 但报告 JSON 的 sampleInfo 要带出订单下全部样本类型与编号。
 * <p>
 * 只跑本类：mvn -Plocal -DskipTests=false -Dtest=CommonReportModuleAssemblerSampleInfoTest test
 *
 * @author <你的名字>
 */
@Tag("local")
@DisplayName("sampleInfo 订单样本汇总")
public class CommonReportModuleAssemblerSampleInfoTest {

    private final CommonReportModuleAssembler assembler = new CommonReportModuleAssembler();

    private static CmsSample sample(String subbarcode, String sampleType) {
        CmsSample sample = new CmsSample();
        sample.setSubbarcode(subbarcode);
        sample.setSampleType(sampleType);
        return sample;
    }

    private static ReportModuleContext context(CmsSample current, List<CmsSample> orderSamples) {
        ReportModuleContext context = new ReportModuleContext();
        context.setSample(current);
        context.setOrderSamples(orderSamples);
        return context;
    }

    @Test
    @DisplayName("双样本订单：类型与编号都带出组织 + 对照血，顺序按编号升序")
    void dualSampleOrder() {
        CmsSample tissue = sample("AJYFS260000046-1A", "石蜡切片");
        CmsSample blood = sample("AJYWB260000046-2A", "全血(白细胞)");
        ReportTemplateData.SampleInfo info = assembler.sampleInfo(context(tissue, List.of(tissue, blood)));

        Assertions.assertEquals("石蜡切片、全血(白细胞)", info.getSampleTypes());
        Assertions.assertEquals("AJYFS260000046-1A、AJYWB260000046-2A", info.getSubbarcodes());
        // 单值字段仍是报告主样本（组织），不被汇总覆盖
        Assertions.assertEquals("AJYFS260000046-1A", info.getSubbarcode());
        Assertions.assertEquals("石蜡切片", info.getSampleType());
    }

    @Test
    @DisplayName("单样本订单：两个汇总字段与本条同值")
    void singleSampleOrder() {
        CmsSample tissue = sample("AJYFS260000047-1A", "石蜡切片");
        ReportTemplateData.SampleInfo info = assembler.sampleInfo(context(tissue, List.of(tissue)));

        Assertions.assertEquals("石蜡切片", info.getSampleTypes());
        Assertions.assertEquals("AJYFS260000047-1A", info.getSubbarcodes());
    }

    @Test
    @DisplayName("没有订单样本（order_no 为空/查询失败）时退化为当前样本")
    void fallbackToCurrentSample() {
        CmsSample tissue = sample("AJYFS260000051-1A", "石蜡切片");
        ReportTemplateData.SampleInfo info = assembler.sampleInfo(context(tissue, List.of()));

        Assertions.assertEquals("石蜡切片", info.getSampleTypes());
        Assertions.assertEquals("AJYFS260000051-1A", info.getSubbarcodes());
    }

    @Test
    @DisplayName("同类型多份样本：类型去重，编号不去重")
    void duplicateTypesDeduped() {
        CmsSample first = sample("AJYFS260000095-1A", "石蜡切片");
        CmsSample second = sample("AJYFS260000095-1B", "石蜡切片");
        ReportTemplateData.SampleInfo info = assembler.sampleInfo(context(first, List.of(first, second)));

        Assertions.assertEquals("石蜡切片", info.getSampleTypes());
        Assertions.assertEquals("AJYFS260000095-1A、AJYFS260000095-1B", info.getSubbarcodes());
    }
}
