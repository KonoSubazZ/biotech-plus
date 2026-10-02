package org.dromara.project.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.project.domain.bo.ProductGeneBo;
import org.dromara.project.domain.bo.ProductGeneImportBo;
import org.dromara.project.domain.vo.NcbiGeneVo;
import org.dromara.project.domain.vo.GeneSymbolExcelRow;
import org.dromara.project.domain.vo.ProductGeneImportResultVo;
import org.dromara.project.domain.vo.ProductGeneVo;
import org.dromara.project.service.IProductGeneService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 产品关联基因
 * <p>
 * 「产品配置」详情里维护某个产品的 gene list：三个接口分别对应三种添加方式
 * （搜索勾选走 /import 的 genes 字段、粘贴走 /import 的 symbols 字段、Excel 走 /importExcel），
 * 入库逻辑全部收敛到 IProductGeneService.importGenes。
 *
 * @author <你的名字>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/project/productGene")
public class ProductGeneController extends BaseController {

    private final IProductGeneService productGeneService;

    /**
     * 分页查询某产品关联的基因（详情页列表）。
     *
     * @param bo        查询条件（productId 必传，geneSymbol 可选做模糊过滤）
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    @SaCheckPermission("project:productGene:query")
    @GetMapping("/list")
    public TableDataInfo<ProductGeneVo> list(ProductGeneBo bo, PageQuery pageQuery) {
        return productGeneService.selectPageList(bo, pageQuery);
    }

    /**
     * 某产品已关联的基因数（列表页/详情头部展示）。
     *
     * @param productId 产品配置 id
     * @return 关联数
     */
    @SaCheckPermission("project:productGene:query")
    @GetMapping("/count")
    public R<Long> count(@NotNull(message = "产品不能为空") @RequestParam Long productId) {
        return R.ok(productGeneService.countByProductId(productId));
    }

    /**
     * 从基因库（nkb.ncbi_gene）搜索基因，供「添加基因」的选择器使用。
     *
     * @param keyword 基因符号关键词
     * @param limit   条数上限（默认 20，最大 50）
     * @return 命中的基因
     */
    @SaCheckPermission("project:productGene:query")
    @GetMapping("/searchGene")
    public R<List<NcbiGeneVo>> searchGene(@RequestParam String keyword,
                                          @RequestParam(defaultValue = "20") int limit) {
        return R.ok(productGeneService.searchGene(keyword, limit));
    }

    /**
     * 批量导入关联基因（搜索勾选 / 粘贴 symbol 列表）。
     *
     * @param bo 导入参数：productId + genes（带 geneId）或 symbols（只有符号）
     * @return 导入结果（新增数 / 跳过数 / 未匹配 / 有歧义）
     */
    @SaCheckPermission("project:productGene:add")
    @Log(title = "产品关联基因", businessType = BusinessType.INSERT)
    @PostMapping("/import")
    public R<ProductGeneImportResultVo> importGenes(@Valid @RequestBody ProductGeneImportBo bo) {
        return R.ok(productGeneService.importGenes(bo));
    }

    /**
     * 上传 Excel 导入关联基因（表格里一列 gene_symbol，读第一列）。
     *
     * @param file      Excel 文件
     * @param productId 产品配置 id
     * @return 导入结果
     */
    @SaCheckPermission("project:productGene:add")
    @Log(title = "产品关联基因", businessType = BusinessType.IMPORT)
    @PostMapping("/importExcel")
    public R<ProductGeneImportResultVo> importExcel(@RequestParam("file") MultipartFile file,
                                                    @NotNull(message = "产品不能为空") @RequestParam Long productId) {
        if (file == null || file.isEmpty()) {
            return R.fail("请选择要上传的 Excel 文件");
        }
        List<String> symbols = readFirstColumn(file);

        ProductGeneImportBo bo = new ProductGeneImportBo();
        bo.setProductId(productId);
        bo.setSymbols(symbols);
        return R.ok(productGeneService.importGenes(bo));
    }

    /**
     * 批量删除关联关系。
     *
     * @param ids 主键集合
     * @return 操作结果
     */
    @SaCheckPermission("project:productGene:remove")
    @Log(title = "产品关联基因", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(productGeneService.deleteByIds(List.of(ids)));
    }

    /**
     * 读 Excel 第一列的非空文本（FastExcel 会自动跳过表头行）。
     * <p>
     * 表格约定是「一列 gene_symbol」，用 GeneSymbolExcelRow 按列序号读第 0 列，
     * 不依赖表头文案 —— 用户手上的表格表头写法五花八门，按序号读更稳。
     */
    private List<String> readFirstColumn(MultipartFile file) {
        List<String> symbols = new ArrayList<>();
        try {
            List<GeneSymbolExcelRow> rows = ExcelUtil.importExcel(file.getInputStream(), GeneSymbolExcelRow.class);
            for (GeneSymbolExcelRow row : rows) {
                if (row != null && row.getGeneSymbol() != null && !row.getGeneSymbol().isBlank()) {
                    symbols.add(row.getGeneSymbol());
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Excel 解析失败：" + e.getMessage(), e);
        }
        return symbols;
    }
}
