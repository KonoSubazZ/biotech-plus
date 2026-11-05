package org.dromara.report.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 药企样本批量导入的结果。
 * <p>
 * 导入是「宽容」的：单行出错不影响其它行，出错的行把原因列出来让人核对。
 *
 * @author <你的名字>
 */
@Data
public class CmsSampleImportResultVo {

    /** 解析到的数据行数（不含表头） */
    private int total;

    /** 新增条数 */
    private int inserted;

    /** 更新条数（CMS 订单主键 + CMS 样本主键已存在，按新文件覆盖） */
    private int updated;

    /** 失败条数（= errors.size()） */
    private int failed;

    /** 每条失败原因（含行号与业务键） */
    private List<String> errors = new ArrayList<>();

    /** 记一条失败原因 */
    public void addError(String message) {
        errors.add(message);
    }
}
