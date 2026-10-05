package org.dromara.report.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.domain.dto.ReportModuleContext;
import org.dromara.report.domain.vo.ReportTemplateData;
import org.dromara.report.domain.vo.ReportTemplateVo;
import org.dromara.report.mapper.ReportTemplateMapper;
import org.dromara.report.service.template.ReportModuleHandler;
import org.dromara.report.service.template.ReportModuleHandlerRegistry;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 报告模板数据组装（设计书 §7.1 / §8.1）。
 * <p>
 * 流程：解析模板 → 建上下文 → 公共字段（CommonReportModuleAssembler）→
 * 按 report_template.module_code 的分号顺序执行个性化 Handler → 返回 ReportTemplateData。
 * <p>
 * 只组装、不落库、不写文件：预览直接返回；正式生成由 {@link ReportGenerationService} 复用同一结果。
 *
 * @author <你的名字>
 */
@Service
@RequiredArgsConstructor
public class ReportTemplateDataService {

    private final CommonReportModuleAssembler commonModules;

    private final ReportModuleHandlerRegistry handlerRegistry;

    private final ReportTemplateMapper reportTemplateMapper;

    /**
     * 组装报告 JSON。
     *
     * @param input 已算好位点匹配的入参
     * @return 总实体类（公共字段 + 模板专属顶层字段）
     */
    public ReportTemplateData build(ReportBuildInput input) {
        ReportTemplateVo template = input.getTemplate();
        ReportModuleContext context = moduleContext(input, template);

        ReportTemplateData data = new ReportTemplateData();
        // 模板由产品决定，进到这里必定非空（requireTemplateByProduct 已拦）
        data.setTemplateId(template.getTemplateId());
        data.setTemplateCode(template.getTemplateCode());
        data.setTemplateVersion(template.getTemplateVersion());
        data.setAnalysisId(input.getContext().getAnalysisId());
        data.setReportId(input.getContext().getReportId());

        commonModules.apply(context, data);

        // 空 module_code = 只输出公共字段；未注册/重复编码在注册表里直接报错
        for (ReportModuleHandler handler : handlerRegistry.requiredPipeline(moduleCodeOf(template))) {
            handler.apply(context, data);
        }
        data.setWarnings(distinctWarnings(data.getWarnings()));
        return data;
    }

    /**
     * 该产品可选的模板（启用中，默认优先）。
     *
     * @param productId   报告产品ID（可为空）
     * @param productName 报告产品名（product_id 为空时按产品名/编码定位产品）
     * @return 候选模板列表；产品没绑定启用模板时返回空列表
     */
    public List<ReportTemplateVo> optionsByProduct(Long productId, String productName) {
        Long resolvedProductId = resolveProductId(productId, productName);
        return resolvedProductId == null ? List.of() : reportTemplateMapper.selectEnabledByProductId(resolvedProductId);
    }

    /**
     * 解析本次使用的模板：报告的产品 → product_template → report_template。
     * <p>
     * 一个产品可能配多个模板，所以允许人工指定 {@code templateId}（必须属于该产品的候选列表）；
     * 不指定就用默认模板（候选列表第一个）；产品没配模板直接报错，让用户看到「当前产品未配置模板」。
     *
     * @param productId    报告产品ID（可为空）
     * @param productName  报告产品名（product_id 为空时按产品名/编码定位产品）
     * @param templateId   人工选择的模板ID（可空 = 用默认）
     * @return 生效的模板，必定非空
     */
    public ReportTemplateVo requireTemplateByProduct(Long productId, String productName, Long templateId) {
        List<ReportTemplateVo> options = optionsByProduct(productId, productName);
        if (templateId != null) {
            return options.stream()
                .filter(option -> templateId.equals(option.getTemplateId()))
                .findFirst()
                .orElseThrow(() -> new ServiceException("所选模板不属于当前产品：" + productLabel(productId, productName)
                    + "；可选：" + optionNames(options)));
        }
        if (options.isEmpty()) {
            throw new ServiceException("当前产品未配置模板：" + productLabel(productId, productName));
        }
        return options.get(0);
    }

    /** 报错文案里的候选模板名（没候选时写「无」） */
    private String optionNames(List<ReportTemplateVo> options) {
        return options.isEmpty() ? "无"
            : options.stream().map(ReportTemplateVo::getTemplateName).collect(Collectors.joining("、"));
    }

    /** 报错文案里的产品标识：优先产品名，其次 productId */
    private String productLabel(Long productId, String productName) {
        return StringUtils.hasText(productName) ? productName : "productId=" + productId;
    }

    /**
     * 解析产品ID：报告里的 product_id 为空时按产品名/编码定位 product_config
     * （scanner 只写产品名，product_id 常为空）。
     *
     * @param productId   报告上的产品ID（可空）
     * @param productName 报告上的产品名（可空）
     * @return product_config.id；定位不到返回 null
     */
    public Long resolveProductId(Long productId, String productName) {
        if (productId != null) {
            return productId;
        }
        return StringUtils.hasText(productName) ? reportTemplateMapper.selectProductIdByName(productName) : null;
    }

    /** 该模板的流水线里是否有 Handler 需要「产品启用基因」 */
    public boolean requiresProductGenes(ReportTemplateVo template) {
        return template != null && handlerRegistry.requiresProductGenes(template.getModuleCode());
    }

    /** 组装传给 Handler 的上下文（公共查询只在这里做一次） */
    private ReportModuleContext moduleContext(ReportBuildInput input, ReportTemplateVo template) {
        PreviewContext pc = input.getContext();
        ReportModuleContext context = new ReportModuleContext();
        context.setAnalysisId(pc.getAnalysisId());
        context.setReportId(pc.getReportId());
        context.setReport(input.getReport());
        context.setLims(input.getLims());
        context.setSomaticVariants(input.getSomaticVariants());
        context.setGermlineVariants(input.getGermlineVariants());
        context.setTemplate(template);
        context.setProductGenes(input.getContext().getProductGenes() == null
            ? List.of() : input.getContext().getProductGenes());
        context.setDisease(pc.getDisease());
        context.setDiseaseId(pc.getDiseaseId());
        context.setDiseaseIds(pc.getDiseaseIds() == null ? List.of() : pc.getDiseaseIds());
        context.setNamedDiseaseIds(input.getNamedDiseaseIds());
        context.setGender(pc.getGender());
        context.setWarnings(input.getWarnings());
        context.setSpecimenType(commonModules.specimenType(context));
        return context;
    }

    private String moduleCodeOf(ReportTemplateVo template) {
        return template == null ? null : template.getModuleCode();
    }

    /** 告警去重（多个 Handler 可能补同一条），保持原顺序 */
    private List<String> distinctWarnings(List<String> warnings) {
        if (warnings == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(new LinkedHashSet<>(warnings));
    }
}
