package org.dromara.report.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 历史检出 导出用行（页面「导出 xlsx」）
 * <p>
 * 列 = 页面上看到的列（报告ID 页面隐藏，导出也不带），用药信息按分组展开成 8 列，
 * 方便在 Excel 里直接筛选/透视。
 *
 * @author <你的名字>
 */
@Data
public class ReportVariantHistoryExcelRow implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "样本编号", index = 0)
    private String subbarcode;

    @ExcelProperty(value = "客户", index = 1)
    private String client;

    @ExcelProperty(value = "基因", index = 2)
    private String gene;

    @ExcelProperty(value = "变异", index = 3)
    private String variant;

    @ExcelProperty(value = "丰度", index = 4)
    private String abundance;

    @ExcelProperty(value = "位点类型", index = 5)
    private String siteType;

    @ExcelProperty(value = "突变类型", index = 6)
    private String variantType;

    @ExcelProperty(value = "原始位点", index = 7)
    private String oriVariant;

    @ExcelProperty(value = "解读癌种", index = 8)
    private String disease;

    @ExcelProperty(value = "产品", index = 9)
    private String product;

    @ExcelProperty(value = "位点等级", index = 10)
    private String level;

    @ExcelProperty(value = "获益A级", index = 11)
    private String drugsA;

    @ExcelProperty(value = "获益B级", index = 12)
    private String drugsB;

    @ExcelProperty(value = "获益C级", index = 13)
    private String drugsC;

    @ExcelProperty(value = "获益D级", index = 14)
    private String drugsD;

    @ExcelProperty(value = "耐药A级", index = 15)
    private String resistantDrugsA;

    @ExcelProperty(value = "耐药B级", index = 16)
    private String resistantDrugsB;

    @ExcelProperty(value = "耐药C级", index = 17)
    private String resistantDrugsC;

    @ExcelProperty(value = "耐药D级", index = 18)
    private String resistantDrugsD;

    @ExcelProperty(value = "解读人", index = 19)
    private String analyzer;

    @ExcelProperty(value = "报告时间", index = 20)
    private String reportGeneratedAt;
}
