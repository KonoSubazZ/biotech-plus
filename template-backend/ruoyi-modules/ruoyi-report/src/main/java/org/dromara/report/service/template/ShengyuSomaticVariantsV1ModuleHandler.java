package org.dromara.report.service.template;

import lombok.RequiredArgsConstructor;
import org.dromara.report.domain.dto.ReportModuleContext;
import org.dromara.report.domain.vo.ReportTemplateData;
import org.dromara.report.service.impl.CommonReportModuleAssembler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 圣域个性化：体细胞变异解析（module_code = {@value #MODULE_CODE}）。
 * <p>
 * 口径（设计书 §7.7）：NKB 当前癌种或任一父级属于<b>乳腺癌 / 卵巢癌 / 前列腺癌</b>时，
 * 按产品启用基因输出 {@code shengyuSomaticVariants}；其他实体瘤输出<b>空列表</b>。
 * 产品未配置启用基因时输出空列表并加告警 —— 绝不静默当作「全部基因」。
 *
 * @author <你的名字>
 */
@Component
@RequiredArgsConstructor
public class ShengyuSomaticVariantsV1ModuleHandler implements ReportModuleHandler {

    /** 模块编码（与 report_template.module_code 一致） */
    public static final String MODULE_CODE = "SHENGYU_SOMATIC_VARIANTS_V1";

    /** 输出的顶层 JSON 字段名 */
    public static final String JSON_FIELD = "shengyuSomaticVariants";

    /** 需要输出体细胞个性化结果的癌种（当前癌种或其父级命中即输出） */
    private static final List<String> REPORTABLE_DISEASES = List.of("乳腺癌", "卵巢癌", "前列腺癌");

    private final CommonReportModuleAssembler commonModules;

    @Override
    public String moduleCode() {
        return MODULE_CODE;
    }

    @Override
    public boolean requiresProductGenes() {
        return true;
    }

    @Override
    public void apply(ReportModuleContext context, ReportTemplateData target) {
        List<String> genes = commonModules.productGenes(context.getProductGenes());
        if (!context.diseaseBelongsToAny(REPORTABLE_DISEASES)) {
            // 其他实体瘤：体细胞个性化结果为空（胚系仍由另一个 Handler 输出）
            target.putPersonalizedModule(JSON_FIELD, List.of());
            warnIfProductGenesEmpty(target, genes);
            return;
        }
        if (genes.isEmpty()) {
            target.putPersonalizedModule(JSON_FIELD, List.of());
            warnIfProductGenesEmpty(target, genes);
            return;
        }
        target.putPersonalizedModule(JSON_FIELD, commonModules.filterSomaticVariants(context.getSomaticVariants(), genes));
    }

    /** 产品没配启用基因时给一条告警，别让「空结果」看起来像「本来就没有位点」 */
    static void warnIfProductGenesEmpty(ReportTemplateData target, List<String> genes) {
        if (genes.isEmpty() && target.getWarnings() != null) {
            target.getWarnings().add("产品未配置启用的基因（product_gene），圣域个性化结果为空白，请先在产品配置里启用基因");
        }
    }
}
