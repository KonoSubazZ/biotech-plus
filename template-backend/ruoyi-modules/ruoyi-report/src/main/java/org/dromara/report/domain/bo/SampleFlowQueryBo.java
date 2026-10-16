package org.dromara.report.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 样本流转列表查询条件。
 * <p>
 * tab 决定「看哪个环节」（9 个页签各一套状态与可见性过滤，口径见 SampleFlowServiceImpl）；
 * showAll 是页面上的「显示全部」开关 —— 打开后不再限制「只看自己认领 / 只看别人的」，
 * 但仍然不能处理别人的报告（处理动作在服务端按状态与人校验）。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SampleFlowQueryBo extends BaseEntity {

    /** 页签：driver / bioinfo-view / bioinfo-review / pending-interpret / interpreting /
     *  pending-review / reviewing / signing / sending */
    private String tab;

    /** 分析日期（精确匹配，YYYYMMDD；空表示不限） */
    private String analysisDate;

    /** 样本编号（模糊） */
    private String subbarcode;

    /** 产品名称（模糊） */
    private String product;

    /** 显示全部（true = 不按「自己认领 / 别人的」过滤） */
    private Boolean showAll;
}
