package org.dromara.project.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 产品关联基因的批量导入结果。
 * <p>
 * 导入是「宽容」的：能找到的都加上，找不到的列出来让人核对，不因为个别 symbol 写错就整体失败。
 *
 * @author <你的名字>
 */
@Data
public class ProductGeneImportResultVo {

    /** 新增成功条数 */
    private int addedCount;

    /** 因为该产品已关联而跳过的条数 */
    private int skippedCount;

    /** 基因库里查不到的 symbol（没有入库） */
    private List<String> unmatched = new ArrayList<>();

    /** 一个 symbol 在基因库里对应多个 gene_id 的（已按 gene_id 最小的关联，提醒人工核对） */
    private List<String> ambiguous = new ArrayList<>();
}
