package org.dromara.report.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.domain.vo.InterpretationContextVo;
import org.dromara.report.domain.vo.InterpretationLimsVo;
import org.dromara.report.domain.vo.ReportTemplateVo;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.mapper.NkbEvidenceMapper;
import org.dromara.report.service.IInterpretationService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.dromara.report.service.impl.PreviewSupport.asLong;
import static org.dromara.report.service.impl.PreviewSupport.asString;
import static org.dromara.report.service.impl.PreviewSupport.firstNonBlank;
import static org.dromara.report.service.impl.PreviewSupport.nkb;
import static org.dromara.report.service.impl.PreviewSupport.normalizeGender;

/**
 * 预览上下文（{@link PreviewContext}）构建：把「报告行 + LIMS 上下文 + 模板」拼成
 * 匹配与组装都需要的上下文。
 * <p>
 * 拆出来单独一类的理由：预览组装（{@code InterpretationPreviewServiceImpl}）和人工干预
 * （{@code VariantEditService} 的临床意义/改靶校验）都要这份上下文，放一份才不会两边漂移。
 * <p>
 * 口径要点：
 * <ul>
 *   <li>客户：LIMS 医院名优先；缺失时用分析粒度占位 {@code UNKNOWN_CUSTOMER@ANALYSIS:<id>}，
 *       避免「未知客户」把不同样本串用成同一条匹配历史。</li>
 *   <li>癌种范围：本癌种 + 祖先 + 子孙，并按性别与实体瘤/血液瘤过滤（en7 getDiseaseList + solidTumorFiltration）。</li>
 *   <li>NKB 是跨库只读表（没有 tenant_id），所有查询走 {@code nkb(...)} 包在
 *       {@code TenantContext.withoutTenant} 里。</li>
 * </ul>
 *
 * @author <你的名字>
 */
@Component
@RequiredArgsConstructor
public class PreviewContextBuilder {

    /**
     * 需要解析成 NKB disease id 的癌种名（与圣域体细胞模块的白名单一致）。
     * <p>
     * 这里只负责「名字 → id」；是否输出体细胞个性化结果由
     * {@code ShengyuSomaticVariantsV1ModuleHandler} 自己判定。
     */
    private static final List<String> NAMED_DISEASE_NAMES = List.of("乳腺癌", "卵巢癌", "前列腺癌");

    private final InterpretationMapper interpretationMapper;

    private final NkbEvidenceMapper nkbEvidenceMapper;

    private final IInterpretationService interpretationService;

    private final ReportTemplateDataService reportTemplateDataService;

    private final NkbDiseaseScopeResolver diseaseScopeResolver;

    /**
     * 读报告行：报告必须存在，且与入参 analysisId 一致（避免拿别的批次的数据预览）。
     *
     * @param reportId   报告ID
     * @param analysisId 分析批次ID
     * @return 报告行（selectReportRow）
     */
    public Map<String, Object> loadReport(Long reportId, Long analysisId) {
        Map<String, Object> report = interpretationMapper.selectReportRow(reportId);
        if (report == null) {
            throw new ServiceException("报告不存在或已删除：reportId=" + reportId);
        }
        if (!analysisId.equals(asLong(report.get("analysisId")))) {
            throw new ServiceException("报告与分析批次不匹配：reportId=" + reportId + "，analysisId=" + analysisId);
        }
        return report;
    }

    /**
     * 组装上下文：癌种/癌种范围、性别、客户、产品项目、产品启用基因、标本类型。
     *
     * @param analysisId 分析批次ID
     * @param reportId   报告ID
     * @param report     报告行（{@link #loadReport}）
     * @param context    LIMS 等上下文（{@code IInterpretationService#loadContext}）
     * @return 预览上下文
     */
    public PreviewContext build(Long analysisId, Long reportId,
                                Map<String, Object> report, InterpretationContextVo context) {
        InterpretationLimsVo lims = context.getLims();
        String disease = firstNonBlank(asString(report.get("disease")), asString(report.get("cancerType")),
            lims.getCancerType(), "未知癌种");
        Long productId = asLong(report.get("productId"));
        String gender = normalizeGender(lims.getGender());
        Map<String, Long> diseaseIdByName = new LinkedHashMap<>();
        Long diseaseId = resolveDiseaseId(disease, diseaseIdByName);
        // 癌种范围：本癌种 + 祖先 + 子孙，并按性别/实体瘤·血液瘤剔除
        NkbDiseaseScopeResolver.DiseaseScope diseaseScope = diseaseScopeResolver.resolve(diseaseId, gender);
        List<Long> diseaseIds = diseaseScope.diseaseIds();

        // 客户：LIMS 医院名优先；缺失时用分析粒度占位（避免未知客户跨样本串用同一条历史）
        String customer = firstNonBlank(lims.getHospitalName(), "UNKNOWN_CUSTOMER@ANALYSIS:" + analysisId);

        PreviewContext pc = new PreviewContext();
        pc.setAnalysisId(analysisId);
        pc.setReportId(reportId);
        pc.setTemplateCode(firstNonBlank(asString(report.get("templateCode")), asString(report.get("template"))));
        pc.setTemplateVersion(asString(report.get("templateVersion")));
        pc.setModuleCode(asString(report.get("moduleCode")));
        pc.setDisease(disease);
        pc.setDiseaseId(diseaseId);
        pc.setDiseaseIds(diseaseIds);
        pc.setDiseaseScope(diseaseScope);
        pc.setGender(gender);
        pc.setCustomer(customer);
        pc.setProjectCode(firstNonBlank(asString(report.get("product")), ""));
        // 产品基因：product_id 为空时按产品名兼容定位（scanner 只写产品名）
        Long resolvedProductId = reportTemplateDataService.resolveProductId(productId, asString(report.get("product")));
        pc.setProductGenes(resolvedProductId == null
            ? List.of() : interpretationMapper.selectProductGeneSymbols(resolvedProductId));
        pc.setSpecimenType(lims.getSpecimenType());
        return pc;
    }

    /**
     * 用解析到的模板覆盖上下文里的模板信息与模块流水线
     * （模板缺失时退化成「只输出公共字段」）。
     *
     * @param pc       上下文
     * @param template 模板（模板解析已保证非空，这里仍按可空处理保持稳健）
     */
    public void applyTemplate(PreviewContext pc, ReportTemplateVo template) {
        if (template == null) {
            pc.setModuleCode(null);
            return;
        }
        pc.setTemplateCode(template.getTemplateCode());
        pc.setTemplateVersion(template.getTemplateVersion());
        pc.setModuleCode(template.getModuleCode());
    }

    /**
     * 癌种名 → NKB disease id（圣域癌种判定用）；查不到就不放进去，
     * 判定会自动降级成中文名比较。
     *
     * @return 癌种名 → disease id
     */
    public Map<String, Long> namedDiseaseIds() {
        Map<String, Long> resolved = new LinkedHashMap<>();
        for (String name : NAMED_DISEASE_NAMES) {
            Map<String, Object> hit = nkb(() -> nkbEvidenceMapper.selectDiseaseByName(name));
            if (hit != null && hit.get("diseaseId") != null) {
                resolved.put(name, asLong(hit.get("diseaseId")));
            }
        }
        return resolved;
    }

    /**
     * 癌种解析：只取 NKB do_id（范围展开交给 {@link NkbDiseaseScopeResolver}）。
     *
     * @param disease          癌种名
     * @param diseaseIdByName  出参：名字 → id（命中时写入）
     * @return NKB disease id；没配到知识库返回 null（按空范围处理，预览仍出「无证据」）
     */
    private Long resolveDiseaseId(String disease, Map<String, Long> diseaseIdByName) {
        Map<String, Object> hit = nkb(() -> nkbEvidenceMapper.selectDiseaseByName(disease));
        Long rootId = null;
        if (hit != null) {
            rootId = asLong(hit.get("diseaseId"));
            diseaseIdByName.put(disease, rootId);
        }
        return rootId;
    }
}
