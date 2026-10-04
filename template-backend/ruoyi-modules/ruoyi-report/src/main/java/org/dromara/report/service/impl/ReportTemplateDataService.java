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
        data.setTemplateId(template == null ? null : template.getTemplateId());
        data.setTemplateCode(template == null ? input.getContext().getTemplateCode() : template.getTemplateCode());
        data.setTemplateVersion(template == null ? input.getContext().getTemplateVersion() : template.getTemplateVersion());
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
     * 解析本次使用的模板（优先级：入参编码 → 报告已绑定模板 → 产品默认模板 → 无模板）。
     *
     * @param templateCode     请求里的模板编码（可为空）
     * @param reportTemplateId 报告已绑定的 template_id（可为空）
     * @param productId        报告产品ID（可为空）
     * @param productName      报告产品名（product_id 为空时按名字定位产品）
     * @return 模板；都没有则返回 null（只输出公共字段）
     */
    public ReportTemplateVo resolveTemplate(String templateCode, Long reportTemplateId, Long productId, String productName) {
        if (StringUtils.hasText(templateCode)) {
            ReportTemplateVo byCode = reportTemplateMapper.selectEnabledByCode(templateCode.trim());
            if (byCode == null) {
                throw new ServiceException("报告模板不存在或已停用：" + templateCode);
            }
            return byCode;
        }
        if (reportTemplateId != null) {
            ReportTemplateVo byId = reportTemplateMapper.selectVoById(reportTemplateId);
            if (byId != null) {
                return byId;
            }
        }
        Long resolvedProductId = productId != null ? productId : reportTemplateMapper.selectProductIdByName(productName);
        return resolvedProductId == null ? null : reportTemplateMapper.selectDefaultByProductId(resolvedProductId);
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
