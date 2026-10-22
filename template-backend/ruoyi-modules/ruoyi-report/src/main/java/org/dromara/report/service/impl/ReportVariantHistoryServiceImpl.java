package org.dromara.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.report.domain.ReportVariantHistory;
import org.dromara.report.domain.bo.ReportVariantHistoryBo;
import org.dromara.report.domain.vo.ReportVariantHistoryExcelRow;
import org.dromara.report.domain.vo.ReportVariantHistoryVo;
import org.dromara.report.mapper.ReportVariantHistoryMapper;
import org.dromara.report.service.IReportVariantHistoryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.dromara.report.service.impl.preview.ReportVariantHistoryWriter;

/**
 * 报告位点检出历史 业务层处理
 * <p>
 * 只读：写入由 {@link ReportVariantHistoryWriter} 在「提交审核」事务里完成。
 * 报告时间范围用字符串比较（`>= begin` 且 `<= end + " 23:59:59"`），与 sample-info 的委托日期同一套写法。
 * 用药信息库里是一列原始 JSON，这里解析成 8 个平铺字段给页面（页面不接触原始串）。
 *
 * @author <你的名字>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ReportVariantHistoryServiceImpl implements IReportVariantHistoryService {

    /** 导出「报告时间」列的格式 */
    private static final DateTimeFormatter EXCEL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 位点类型（source_type）→ 展示标签，与前端 SOURCE_TYPE_OPTIONS 同口径（报告模板里 SNP_INDEL 也叫 SNV/Indel） */
    private static final Map<String, String> SITE_TYPE_LABELS = siteTypeLabels();

    private final ReportVariantHistoryMapper baseMapper;

    private final ObjectMapper objectMapper;

    @Override
    public TableDataInfo<ReportVariantHistoryVo> selectPageList(ReportVariantHistoryBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ReportVariantHistory> wrapper = buildQueryWrapper(bo);
        Page<ReportVariantHistoryVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        page.getRecords().forEach(row -> fillDrugGroups(row, row.getDrugInfo()));
        return TableDataInfo.build(page);
    }

    @Override
    public List<ReportVariantHistoryExcelRow> selectExcelList(ReportVariantHistoryBo bo) {
        return selectList(bo).stream().map(this::toExcelRow).toList();
    }

    /** 不分页查询：与分页同一套条件、同一套用药信息解析（导出走它，保证导出与页面一致） */
    private List<ReportVariantHistoryVo> selectList(ReportVariantHistoryBo bo) {
        List<ReportVariantHistoryVo> rows = baseMapper.selectVoList(buildQueryWrapper(bo));
        rows.forEach(row -> fillDrugGroups(row, row.getDrugInfo()));
        return rows;
    }

    /**
     * 导出列映射：列与页面一致（报告ID 页面已隐藏，导出也不带），
     * 用药信息按 8 个分组各占一列，方便在 Excel 里筛选。
     */
    private ReportVariantHistoryExcelRow toExcelRow(ReportVariantHistoryVo vo) {
        ReportVariantHistoryExcelRow row = new ReportVariantHistoryExcelRow();
        row.setSubbarcode(vo.getSubbarcode());
        row.setClient(vo.getClient());
        row.setProjectNo(vo.getProjectNo());
        row.setGene(vo.getGene());
        row.setVariant(vo.getVariant());
        row.setAbundance(vo.getAbundance());
        row.setSiteType(siteTypeLabel(vo.getSourceType()));
        row.setVariantType(vo.getVariantType());
        row.setOriVariant(vo.getOriVariant());
        row.setDisease(vo.getDisease());
        row.setProduct(vo.getProduct());
        row.setLevel(vo.getLevel());
        row.setDrugsA(vo.getDrugsA());
        row.setDrugsB(vo.getDrugsB());
        row.setDrugsC(vo.getDrugsC());
        row.setDrugsD(vo.getDrugsD());
        row.setResistantDrugsA(vo.getResistantDrugsA());
        row.setResistantDrugsB(vo.getResistantDrugsB());
        row.setResistantDrugsC(vo.getResistantDrugsC());
        row.setResistantDrugsD(vo.getResistantDrugsD());
        row.setAnalyzer(vo.getAnalyzer());
        row.setReportGeneratedAt(formatTime(vo));
        return row;
    }

    /** 位点类型（source_type）→ 展示标签；认不出的原样返回，不静默吞掉 */
    private static Map<String, String> siteTypeLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("SNP_INDEL", "SNP/Indel");
        labels.put("CNV", "CNV");
        labels.put("FUSION", "Fusion");
        labels.put("CR_ALL", "CR_ALL");
        return labels;
    }

    /** 取位点类型的展示标签（空进空出，页面自己显示 -；认不出的原样返回，不静默吞掉） */
    private String siteTypeLabel(String sourceType) {
        if (StringUtils.isBlank(sourceType)) {
            return sourceType;
        }
        return SITE_TYPE_LABELS.getOrDefault(sourceType.toUpperCase(), sourceType);
    }

    /** 报告时间 → 字符串（导出用；空值给 null，Excel 里该格为空） */
    private String formatTime(ReportVariantHistoryVo vo) {
        Date time = vo.getReportGeneratedAt();
        return time == null
            ? null
            : LocalDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault()).format(EXCEL_TIME);
    }

    /**
     * 把原始 {@code drug_info}（en7 drugGroups JSON）解析到 8 个平铺字段。
     * <p>
     * 空分组保持 null —— 页面「没有就不展示」直接判空即可。
     * 解析失败只记日志不影响列表：原始串仍在库里（{@code drug_info}）。
     */
    private void fillDrugGroups(ReportVariantHistoryVo vo, String drugInfo) {
        if (StringUtils.isBlank(drugInfo)) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(drugInfo);
            vo.setDrugsA(text(root, "drugsA"));
            vo.setDrugsB(text(root, "drugsB"));
            vo.setDrugsC(text(root, "drugsC"));
            vo.setDrugsD(text(root, "drugsD"));
            vo.setResistantDrugsA(text(root, "resistantDrugsA"));
            vo.setResistantDrugsB(text(root, "resistantDrugsB"));
            vo.setResistantDrugsC(text(root, "resistantDrugsC"));
            vo.setResistantDrugsD(text(root, "resistantDrugsD"));
        } catch (Exception e) {
            log.warn("用药信息（drug_info）解析失败，本次不显示分组：{}", e.getMessage());
        }
    }

    /** 取某个分组的药名串；节点缺失 / null / 空白一律当没有（空分组 = 页面不渲染） */
    private String text(JsonNode root, String key) {
        JsonNode node = root.get(key);
        String value = node == null || node.isNull() ? null : node.asText();
        return StringUtils.isBlank(value) ? null : value;
    }

    /**
     * 组装查询条件：声明 Wrapper → 4 个模糊条件 → 报告时间范围 → 排序。
     * <p>
     * 搜索项都是前导通配的模糊匹配（`like`），B-tree 用不上，所以只给报告时间建了索引；
     * 排序按「报告时间倒序 + id 倒序」保证分页稳定。
     */
    private LambdaQueryWrapper<ReportVariantHistory> buildQueryWrapper(ReportVariantHistoryBo bo) {
        LambdaQueryWrapper<ReportVariantHistory> wrapper = Wrappers.lambdaQuery();

        wrapper.like(StringUtils.isNotBlank(bo.getSubbarcode()),
            ReportVariantHistory::getSubbarcode, bo.getSubbarcode());
        wrapper.like(StringUtils.isNotBlank(bo.getDisease()), ReportVariantHistory::getDisease, bo.getDisease());
        wrapper.like(StringUtils.isNotBlank(bo.getProduct()), ReportVariantHistory::getProduct, bo.getProduct());
        wrapper.like(StringUtils.isNotBlank(bo.getClient()), ReportVariantHistory::getClient, bo.getClient());
        wrapper.like(StringUtils.isNotBlank(bo.getProjectNo()),
            ReportVariantHistory::getProjectNo, bo.getProjectNo());

        Map<String, Object> params = bo.getParams();
        Object beginTime = params == null ? null : params.get("beginTime");
        Object endTime = params == null ? null : params.get("endTime");
        if (beginTime != null && endTime != null) {
            wrapper.ge(ReportVariantHistory::getReportGeneratedAt, beginTime);
            wrapper.le(ReportVariantHistory::getReportGeneratedAt, endTime + " 23:59:59");
        }

        wrapper.orderByDesc(ReportVariantHistory::getReportGeneratedAt);
        wrapper.orderByDesc(ReportVariantHistory::getId);
        return wrapper;
    }
}
