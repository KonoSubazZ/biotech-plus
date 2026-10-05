package org.dromara.report.service.template;

import lombok.RequiredArgsConstructor;
import org.dromara.report.domain.dto.ReportModuleContext;
import org.dromara.report.domain.vo.ReportTemplateData;
import org.dromara.report.service.impl.CommonReportModuleAssembler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 圣域个性化：胚系变异（module_code = {@value #MODULE_CODE}）。
 * <p>
 * 口径（设计书 §7.7）：<b>始终</b>按产品启用基因输出 {@code shengyuGermlineVariants}
 * （不区分癌种 —— 其他实体瘤也要出胚系结果）；产品未配置启用基因时输出空列表并加告警。
 *
 * @author <你的名字>
 */
@Component
@RequiredArgsConstructor
public class ShengyuGermlineVariantsV1ModuleHandler implements ReportModuleHandler {

    /** 模块编码（与 report_template.module_code 一致） */
    public static final String MODULE_CODE = "SHENGYU_GERMLINE_VARIANTS_V1";

    /** 输出的顶层 JSON 字段名 */
    public static final String JSON_FIELD = "shengyuGermlineVariants";

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
        if (genes.isEmpty()) {
            target.putPersonalizedModule(JSON_FIELD, List.of());
            ShengyuSomaticVariantsV1ModuleHandler.warnIfProductGenesEmpty(target, genes);
            return;
        }
        target.putPersonalizedModule(JSON_FIELD, commonModules.filterGermlineVariants(context.getGermlineVariants(), genes));
    }
}
