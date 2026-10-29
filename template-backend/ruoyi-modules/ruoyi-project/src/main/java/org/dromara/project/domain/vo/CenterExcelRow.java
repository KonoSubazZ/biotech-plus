package org.dromara.project.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 研究中心导入 / 导出的 Excel 行对象。
 * <p>
 * 导出 = 当前项目下的研究中心清单（同时也是导入模板，空项目导出只有表头），
 * 导入 = 同一份文件：用户在导出的表里原地改，新中心在末尾追加行。
 * <p>
 * 读表按 {@code index}（列序号）匹配，不看表头文案 —— 用户把「研究中心编号」改成
 * 「中心编号」也照样能读（与 report 模块的样本导入同一套约定）。
 * <p>
 * 只有四列：没有「删除标记」，删除只在页面上做（用户 2026-10-29 口径）。
 *
 * @author levi
 */
@Data
public class CenterExcelRow {

    /** 研究中心编号（同一项目内用它匹配：能对上=修改，对不上=新增） */
    @ExcelProperty(value = "研究中心编号", index = 0)
    private String researchCenterNo;

    /** 研究中心名称 */
    @ExcelProperty(value = "研究中心名称", index = 1)
    private String researchCenterName;

    /** 报告接收邮箱（多个用分号分隔） */
    @ExcelProperty(value = "报告接收邮箱", index = 2)
    private String receiveEmail;

    /** 备注 */
    @ExcelProperty(value = "备注", index = 3)
    private String remark;
}
