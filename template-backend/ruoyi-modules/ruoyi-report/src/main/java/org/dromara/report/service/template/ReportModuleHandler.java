package org.dromara.report.service.template;

import org.dromara.report.domain.dto.ReportModuleContext;
import org.dromara.report.domain.vo.ReportTemplateData;

/**
 * 报告个性化模块处理器（设计书 §7.4）。
 * <p>
 * 约束：
 * <ul>
 *   <li>{@link #moduleCode()} 在应用内唯一，与 report_template.module_code 里的编码做大小写统一。</li>
 *   <li>{@link #apply} 只改当前构建中的目标 DTO，不接受前端回传的预览 JSON。</li>
 *   <li>相同上下文必须产生相同 JSON：不得写当前时间或随机值。</li>
 *   <li>缺失业务值用 null；只有明确要展示的位（如补空行）才补 "/"。</li>
 *   <li>Handler 不调渲染器、不改报告状态、不落库（只读查询允许）。</li>
 * </ul>
 *
 * @author <你的名字>
 */
public interface ReportModuleHandler {

    /** 模块编码（与 report_template.module_code 中的值一致） */
    String moduleCode();

    /** 是否需要「产品启用基因」；返回 true 时 Service 会先查 product_gene 并放进上下文 */
    default boolean requiresProductGenes() {
        return false;
    }

    /** 组装本模块的个性化字段（写进 target 的顶层动态字段） */
    void apply(ReportModuleContext context, ReportTemplateData target);
}
