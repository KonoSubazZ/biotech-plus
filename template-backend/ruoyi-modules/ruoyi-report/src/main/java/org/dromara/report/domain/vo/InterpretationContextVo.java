package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 解读页上下文（GET /report/interpretation/context）
 * <p>
 * 解读详情页一次加载：报告头 + LIMS 信息 + 生成前校验结论。
 * files（Tab②）/ variants（Tab③）等在后续页面按需追加，避免一次拉全量。
 *
 * @author <你的名字>
 */
@Data
public class InterpretationContextVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 报告头信息 */
    private AnalysisReportVo report;

    /** LIMS 信息（Tab①） */
    private InterpretationLimsVo lims;

    /** 是否满足继续解读/生成的最低数据要求 */
    private Boolean canGenerate;

    /** 阻止生成的错误（必填数据缺失等） */
    private List<String> errors = new ArrayList<>();

    /** 不阻断流程的告警（非关键字段缺失） */
    private List<String> warnings = new ArrayList<>();
}
