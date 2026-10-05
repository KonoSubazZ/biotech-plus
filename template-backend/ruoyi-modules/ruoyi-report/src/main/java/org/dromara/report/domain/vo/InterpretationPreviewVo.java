package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 报告预览返回体：数据 + 来源标记。
 * <p>
 * 报告已生成过时预览直接读最近一次生成的 JSON 制品（多人共享、不用重复匹配），
 * 所以要把「这份数据是哪来的」明确告诉前端，页面才能提示用户。
 *
 * @author <你的名字>
 */
@Data
public class InterpretationPreviewVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 数据来源：artifact = 已生成的 JSON 制品；realtime = 实时查库重新匹配组装 */
    private String source;

    /** 制品生成时间（source=artifact 时有值） */
    private String artifactGeneratedAt;

    /** 预览 JSON（与生成时同一份契约 ReportTemplateData） */
    private ReportTemplateData data;
}
