package org.dromara.report.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 样本信息批量导入的结果。
 * <p>
 * 导入是「宽容」的：单行出错不影响其它行，出错的行把原因列出来让人核对，
 * 不因为个别单元格写错就整体回滚（与 project 模块的基因导入同一口径）。
 *
 * @author <你的名字>
 */
@Data
public class SampleInfoImportResultVo {

    /** 解析到的数据行数（不含表头） */
    private int total;

    /** 新增条数 */
    private int inserted;

    /** 更新条数（样本编号已存在，按新文件覆盖） */
    private int updated;

    /** 失败条数（= errors.size()） */
    private int failed;

    /** 每条失败原因（含行号与样本编号） */
    private List<String> errors = new ArrayList<>();

    /** 记一条失败原因 */
    public void addError(String message) {
        errors.add(message);
    }
}
