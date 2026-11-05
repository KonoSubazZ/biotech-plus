package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 样本流转「处理」结果。
 * <p>
 * 前端只管照做：给了 targetTab + reportId 就跳转报告解读详情页；没给就按 message 提示并刷新列表
 * （认领类动作更新状态后数据会自动流转到下一个页签）。
 *
 * @author <你的名字>
 */
@Data
public class SampleFlowHandleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分析数据ID */
    private Long analysisId;

    /** 报告ID（跳转详情页必需；为空表示该批次还没有报告记录） */
    private Long reportId;

    /** 跳转目标页签（报告解读的页签名：files / variants / preview / review / send；为空表示不跳转） */
    private String targetTab;

    /** 处理后的报告状态（认领类动作返回，供页面提示） */
    private Integer reportStatus;

    /** 页面提示（不跳转时说明原因） */
    private String message;
}
