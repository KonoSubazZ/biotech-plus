package org.dromara.project.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * Excel 导入「产品关联基因」时的行对象。
 * <p>
 * 表格约定：**第一列**放 gene_symbol（可以有表头，FastExcel 会跳过表头行）。
 * 只按列序号读（index = 0），不按表头名匹配 —— 用户手上的表格表头写得五花八门，
 * 按序号读更不容易因为表头文案不同而读空。
 *
 * @author <你的名字>
 */
@Data
public class GeneSymbolExcelRow {

    /** 基因符号（Excel 第一列） */
    @ExcelProperty(index = 0)
    private String geneSymbol;
}
