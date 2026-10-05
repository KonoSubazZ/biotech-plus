package org.dromara.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.report.domain.ReportTemplate;
import org.dromara.report.domain.bo.ReportTemplateBo;
import org.dromara.report.domain.vo.ReportTemplateVo;
import org.dromara.report.mapper.ReportTemplateMapper;
import org.dromara.report.service.ReportNameVariables;
import org.dromara.report.service.IReportTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 报告模板 业务层处理
 * <p>
 * 三条业务规则：
 * <ol>
 *   <li>模板编码同租户唯一；唯一键不含 del_flag，所以「删了再加同编码」走<b>恢复</b>而不是重插。</li>
 *   <li>「模板 ↔ 产品」关系由本服务同步：先整体失效、再按传入集合逐条恢复或插入。</li>
 *   <li>已被 analysis_report 引用的模板禁止删除（否则报告会指向不存在的模板）。</li>
 * </ol>
 *
 * @author <你的名字>
 */
@RequiredArgsConstructor
@Service
public class ReportTemplateServiceImpl implements IReportTemplateService {

    private final ReportTemplateMapper baseMapper;

    private final ReportNameVariables reportNameVariables;

    @Override
    public TableDataInfo<ReportTemplateVo> selectPageList(ReportTemplateBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ReportTemplate> wrapper = buildQueryWrapper(bo);
        Page<ReportTemplateVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        fillProductRelations(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public ReportTemplateVo queryById(Long templateId) {
        ReportTemplateVo vo = baseMapper.selectVoById(templateId);
        if (vo == null) {
            throw new ServiceException("报告模板不存在：templateId=" + templateId);
        }
        vo.setProductIds(baseMapper.selectProductIds(templateId));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(ReportTemplateBo bo) {
        assertCodeUsable(bo.getTemplateCode(), null);
        reportNameVariables.validate(bo.getReportName());
        ReportTemplate entity = MapstructUtils.convert(bo, ReportTemplate.class);
        baseMapper.insert(entity);
        syncProductTemplates(entity.getTemplateId(), bo.getProductIds());
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(ReportTemplateBo bo) {
        if (bo.getTemplateId() == null) {
            throw new ServiceException("修改报告模板必须提供主键");
        }
        // 先确认记录存在，避免把「改不到」当成「改成功」
        queryById(bo.getTemplateId());
        assertCodeUsable(bo.getTemplateCode(), bo.getTemplateId());
        reportNameVariables.validate(bo.getReportName());
        ReportTemplate entity = MapstructUtils.convert(bo, ReportTemplate.class);
        baseMapper.updateById(entity);
        syncProductTemplates(bo.getTemplateId(), bo.getProductIds());
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要删除的报告模板");
        }
        for (Long templateId : ids) {
            assertNotReferenced(templateId);
        }
        baseMapper.deleteByIds(ids);
        for (Long templateId : ids) {
            baseMapper.invalidateProductTemplates(templateId);
        }
        return Boolean.TRUE;
    }

    @Override
    public List<Map<String, Object>> selectProductOptions() {
        return baseMapper.selectProductOptions();
    }

    /** 组装查询条件：声明 Wrapper → 模糊条件 → 精确条件 → 排序 */
    private LambdaQueryWrapper<ReportTemplate> buildQueryWrapper(ReportTemplateBo bo) {
        LambdaQueryWrapper<ReportTemplate> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StringUtils.isNotBlank(bo.getTemplateCode()),
            ReportTemplate::getTemplateCode, bo.getTemplateCode());
        wrapper.like(StringUtils.isNotBlank(bo.getTemplateName()),
            ReportTemplate::getTemplateName, bo.getTemplateName());
        wrapper.eq(StringUtils.isNotBlank(bo.getReportType()), ReportTemplate::getReportType, bo.getReportType());
        wrapper.eq(StringUtils.isNotBlank(bo.getStatus()), ReportTemplate::getStatus, bo.getStatus());
        wrapper.orderByDesc(ReportTemplate::getTemplateId);
        return wrapper;
    }

    /**
     * 模板编码查重（对应 uk_report_template_code）。
     * <p>
     * 普通查询带 del_flag='0'，所以「编码被已删模板占着」要单独查一次给出可读提示，
     * 否则会直接抛数据库的唯一键冲突。
     */
    private void assertCodeUsable(String templateCode, Long excludeId) {
        if (StringUtils.isBlank(templateCode)) {
            return;
        }
        LambdaQueryWrapper<ReportTemplate> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ReportTemplate::getTemplateCode, templateCode);
        wrapper.ne(excludeId != null, ReportTemplate::getTemplateId, excludeId);
        if (baseMapper.exists(wrapper)) {
            throw new ServiceException("模板编码已存在：" + templateCode);
        }
        if (excludeId == null && baseMapper.selectIdByCodeIncludeDeleted(templateCode) != null) {
            throw new ServiceException("模板编码曾被删除（仍占用唯一键）：" + templateCode + "，请换一个编码");
        }
    }

    /** 删除前的引用校验：被分析报告引用时禁止删除 */
    private void assertNotReferenced(Long templateId) {
        Long usedByReport = baseMapper.countAnalysisReportByTemplateId(templateId);
        if (usedByReport != null && usedByReport > 0) {
            throw new ServiceException("该模板已被 " + usedByReport + " 份报告引用，禁止删除", 409);
        }
    }

    /**
     * 同步「模板 ↔ 产品」关联。
     * <p>
     * 先整体置失效（del_flag='1'），再对传入集合逐条「恢复或插入」——
     * uk_product_template 不含 del_flag，直接 insert 会撞唯一键。
     * 空集合表示解除全部关联。
     */
    private void syncProductTemplates(Long templateId, List<Long> productIds) {
        baseMapper.invalidateProductTemplates(templateId);
        List<Long> wanted = distinctIds(productIds);
        if (wanted.isEmpty()) {
            return;
        }
        int sortOrder = 0;
        for (Long productId : wanted) {
            int activated = baseMapper.activateProductTemplate(templateId, productId, sortOrder);
            if (activated == 0) {
                baseMapper.insertProductTemplate(templateId, productId, sortOrder);
            }
            sortOrder++;
        }
    }

    /** 去重并丢弃 null，保持传入顺序 */
    private List<Long> distinctIds(List<Long> ids) {
        LinkedHashSet<Long> distinct = new LinkedHashSet<>();
        if (ids != null) {
            for (Long id : ids) {
                if (id != null) {
                    distinct.add(id);
                }
            }
        }
        return new ArrayList<>(distinct);
    }

    /** 列表页补充关联产品：一次批量查关系，再按模板分组回填 productIds 与 productNames */
    private void fillProductRelations(List<ReportTemplateVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> templateIds = new ArrayList<>();
        for (ReportTemplateVo row : rows) {
            templateIds.add(row.getTemplateId());
        }
        Map<Long, List<Long>> idsByTemplate = new HashMap<>();
        Map<Long, List<String>> namesByTemplate = new HashMap<>();
        for (Map<String, Object> relation : baseMapper.selectProductRelationsByTemplateIds(templateIds)) {
            collectRelation(relation, idsByTemplate, namesByTemplate);
        }
        for (ReportTemplateVo row : rows) {
            row.setProductIds(idsByTemplate.getOrDefault(row.getTemplateId(), new ArrayList<>()));
            row.setProductNames(namesByTemplate.getOrDefault(row.getTemplateId(), new ArrayList<>()));
        }
    }

    /** 把一行「模板-产品」关系按模板分组收集成 productIds / productNames */
    private void collectRelation(Map<String, Object> relation,
                                 Map<Long, List<Long>> idsByTemplate,
                                 Map<Long, List<String>> namesByTemplate) {
        Long templateId = toLong(relation.get("templateId"));
        Long productId = toLong(relation.get("productId"));
        String productName = toText(relation.get("productName"));
        idsByTemplate.computeIfAbsent(templateId, key -> new ArrayList<>()).add(productId);
        namesByTemplate.computeIfAbsent(templateId, key -> new ArrayList<>()).add(productName);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }

    private String toText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
