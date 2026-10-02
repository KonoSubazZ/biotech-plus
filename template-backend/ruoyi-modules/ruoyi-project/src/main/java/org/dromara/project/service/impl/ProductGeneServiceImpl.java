package org.dromara.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.project.domain.ProductGene;
import org.dromara.project.domain.bo.ProductGeneBo;
import org.dromara.project.domain.bo.ProductGeneImportBo;
import org.dromara.project.domain.vo.NcbiGeneVo;
import org.dromara.project.domain.vo.ProductGeneImportResultVo;
import org.dromara.project.domain.vo.ProductGeneVo;
import org.dromara.project.mapper.ProductGeneMapper;
import org.dromara.project.service.IProductGeneService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 产品关联基因 业务层处理
 * <p>
 * 导入是「宽容」策略：能匹配的都加上，匹配不到的列出来让人核对，不因为个别 symbol 写错就整体失败。
 * 具体规则见 importGenes 的注释。
 *
 * @author <你的名字>
 */
@RequiredArgsConstructor
@Service
public class ProductGeneServiceImpl implements IProductGeneService {

    /** 单次导入的 symbol 上限：防止一个请求把库拖死（超出的会被截断，由前端分批） */
    private static final int MAX_IMPORT_SYMBOLS = 20000;

    /** 基因搜索返回条数上限 */
    private static final int MAX_SEARCH_LIMIT = 50;

    private final ProductGeneMapper baseMapper;

    @Override
    public TableDataInfo<ProductGeneVo> selectPageList(ProductGeneBo bo, PageQuery pageQuery) {
        if (bo.getProductId() == null) {
            throw new ServiceException("请先选择产品");
        }
        LambdaQueryWrapper<ProductGene> wrapper = buildQueryWrapper(bo);
        // 显式声明中间变量：selectVoPage 的泛型返回会让 TableDataInfo.build 重载解析不明确
        Page<ProductGeneVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public Long countByProductId(Long productId) {
        if (productId == null) {
            return 0L;
        }
        Long count = baseMapper.countByProductId(productId);
        return count == null ? 0L : count;
    }

    @Override
    public List<NcbiGeneVo> searchGene(String keyword, int limit) {
        if (StringUtils.isBlank(keyword)) {
            return List.of();
        }
        int size = Math.min(Math.max(limit, 1), MAX_SEARCH_LIMIT);
        return baseMapper.searchGeneFromNcbi(keyword.trim(), size);
    }

    /**
     * 批量导入关联基因。三种添加方式共用这一条路径：
     * <ol>
     *   <li>从基因库搜索勾选 —— 前端带上 geneId + geneSymbol，直接采信</li>
     *   <li>粘贴一列 symbol —— 后端去 nkb.ncbi_gene 补全 geneId</li>
     *   <li>上传 Excel（一列 gene_symbol）—— Controller 把单元格读成 List&lt;String&gt; 后同上</li>
     * </ol>
     * 处理规则：基因库里查不到的 → 只报告不入库（gene_id 是 NOT NULL，塞假的会污染数据）；
     * 一个 symbol 对应多个 gene_id 的 → 取 gene_id 最小的，并在结果里提示人工核对；
     * 该产品已关联的 → 跳过，不报错。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductGeneImportResultVo importGenes(ProductGeneImportBo bo) {
        if (bo.getProductId() == null) {
            throw new ServiceException("请先选择产品");
        }
        ProductGeneImportResultVo result = new ProductGeneImportResultVo();

        // 1. 汇总候选：geneId -> geneSymbol（LinkedHashMap 去重且保持输入顺序）
        Map<Integer, String> candidates = new LinkedHashMap<>();

        if (bo.getGenes() != null) {
            for (ProductGeneBo gene : bo.getGenes()) {
                if (gene.getGeneId() == null || StringUtils.isBlank(gene.getGeneSymbol())) {
                    continue;
                }
                candidates.putIfAbsent(gene.getGeneId(), gene.getGeneSymbol().trim());
            }
        }

        List<String> symbols = normalizeSymbols(bo.getSymbols());
        if (!symbols.isEmpty()) {
            fillCandidatesBySymbols(symbols, candidates, result);
        }

        if (candidates.isEmpty()) {
            return result;
        }

        // 2. 去掉该产品已经关联过的（逻辑删除的不算已存在）
        Set<Integer> existing = existingGeneIds(bo.getProductId());
        Iterator<Map.Entry<Integer, String>> it = candidates.entrySet().iterator();
        while (it.hasNext()) {
            if (existing.contains(it.next().getKey())) {
                result.setSkippedCount(result.getSkippedCount() + 1);
                it.remove();
            }
        }

        // 3. 批量入库
        if (!candidates.isEmpty()) {
            List<ProductGene> rows = new ArrayList<>(candidates.size());
            candidates.forEach((geneId, geneSymbol) -> {
                ProductGene row = new ProductGene();
                row.setProductId(bo.getProductId());
                row.setGeneId(geneId);
                row.setGeneSymbol(geneSymbol);
                rows.add(row);
            });
            baseMapper.insertBatch(rows);
            result.setAddedCount(rows.size());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要删除的关联基因");
        }
        baseMapper.deleteByIds(ids);
        return Boolean.TRUE;
    }

    /**
     * 用 symbol 去基因库补全 geneId，填进 candidates；查不到 / 有歧义的记进 result。
     */
    private void fillCandidatesBySymbols(List<String> symbols, Map<Integer, String> candidates,
                                         ProductGeneImportResultVo result) {
        List<NcbiGeneVo> hits = baseMapper.selectGenesBySymbolsFromNcbi(symbols);
        Map<String, List<NcbiGeneVo>> bySymbol = new HashMap<>();
        for (NcbiGeneVo hit : hits) {
            bySymbol.computeIfAbsent(hit.getGeneSymbol(), key -> new ArrayList<>()).add(hit);
        }
        for (String symbol : symbols) {
            List<NcbiGeneVo> list = bySymbol.get(symbol);
            if (list == null || list.isEmpty()) {
                result.getUnmatched().add(symbol);
                continue;
            }
            if (list.size() > 1) {
                // 基因库里 tRNA 类基因会重名（如 TRNAN-GUU 对应 19 个 gene_id）
                result.getAmbiguous().add(symbol + "（" + list.size() + " 个 gene_id）");
            }
            NcbiGeneVo picked = list.get(0);
            candidates.putIfAbsent(picked.getGeneId(), picked.getGeneSymbol());
        }
    }

    /**
     * 清洗 symbol：去 BOM（Excel 导出常带）、去首尾空白、去重、限制条数，保持输入顺序。
     */
    private List<String> normalizeSymbols(List<String> raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        Set<String> seen = new HashSet<>();
        for (String item : raw) {
            if (item == null) {
                continue;
            }
            String value = item.replace("\uFEFF", "").trim();
            if (value.isEmpty() || !seen.add(value)) {
                continue;
            }
            out.add(value);
            if (out.size() >= MAX_IMPORT_SYMBOLS) {
                break;
            }
        }
        return out;
    }

    /**
     * 该产品已关联的 geneId 集合（不含逻辑删除的）。
     */
    private Set<Integer> existingGeneIds(Long productId) {
        LambdaQueryWrapper<ProductGene> wrapper = Wrappers.lambdaQuery();
        wrapper.select(ProductGene::getGeneId);
        wrapper.eq(ProductGene::getProductId, productId);
        return baseMapper.selectList(wrapper).stream()
            .map(ProductGene::getGeneId)
            .collect(Collectors.toSet());
    }

    /**
     * 组装查询条件：声明 Wrapper → 基础条件 → 分支条件 → 排序。
     */
    private LambdaQueryWrapper<ProductGene> buildQueryWrapper(ProductGeneBo bo) {
        LambdaQueryWrapper<ProductGene> wrapper = Wrappers.lambdaQuery();

        wrapper.eq(ProductGene::getProductId, bo.getProductId());
        wrapper.like(StringUtils.isNotBlank(bo.getGeneSymbol()), ProductGene::getGeneSymbol, bo.getGeneSymbol());

        wrapper.orderByAsc(ProductGene::getGeneSymbol);
        return wrapper;
    }
}
