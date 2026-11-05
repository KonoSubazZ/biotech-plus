package org.dromara.project.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 研究中心批量导入的结果。
 * <p>
 * 口径（用户 2026-10-29）：<b>先全量校验，任一行不通过就整体不导入</b>。
 * 所以一旦 {@link #errors} 非空，{@link #committed} = false，inserted / updated 都是 0，
 * 数据库一行都没动；前端据此提示「校验没通过，未导入任何数据」。
 *
 * @author levi
 */
@Data
public class CenterImportResultVo {

    /** 解析到的数据行数（不含表头、不含整行空白） */
    private int total;

    /** 实际新增条数（committed=false 时恒为 0） */
    private int inserted;

    /** 实际修改条数（committed=false 时恒为 0） */
    private int updated;

    /** 校验失败条数（= errors.size()） */
    private int failed;

    /** 是否真正写库（只有校验全过才为 true） */
    private boolean committed;

    /** 每条失败原因（含 Excel 行号与编号） */
    private List<String> errors = new ArrayList<>();

    /** 记一条失败原因 */
    public void addError(String message) {
        errors.add(message);
    }
}
