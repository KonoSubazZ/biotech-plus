package org.dromara.report.service.template;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 处理器注册表（设计书 §7.5）：按 report_template.module_code 解析有序流水线。
 * <p>
 * Spring 自动注入全部 {@link ReportModuleHandler} 实现；新增 Handler 不需要改本类、
 * 不需要改 Controller、也不要有中心 switch。
 *
 * @author <你的名字>
 */
@Slf4j
@Component
public class ReportModuleHandlerRegistry {

    private final Map<String, ReportModuleHandler> handlers;

    public ReportModuleHandlerRegistry(List<ReportModuleHandler> handlerList) {
        Map<String, ReportModuleHandler> registered = new LinkedHashMap<>();
        for (ReportModuleHandler handler : handlerList) {
            String code = normalize(handler.moduleCode());
            if (registered.containsKey(code)) {
                throw new IllegalStateException("报告模块编码重复：" + code);
            }
            registered.put(code, handler);
        }
        this.handlers = Collections.unmodifiableMap(registered);
        log.info("报告模块处理器已注册 {} 个：{}", handlers.size(), handlers.keySet());
    }

    /**
     * 取单个已注册处理器。
     *
     * @param moduleCode 模块编码
     * @return 处理器
     */
    public ReportModuleHandler required(String moduleCode) {
        ReportModuleHandler handler = handlers.get(normalize(moduleCode));
        if (handler == null) {
            throw new ServiceException("未注册的报告模块：" + moduleCode + "（模板配置错误，请检查 report_template.module_code）");
        }
        return handler;
    }

    /**
     * 解析「分号分隔的有序模块列表」成流水线。
     * <p>
     * 空值/空串 → 空列表（只输出公共字段）；重复编码或未注册编码立即报错
     * （模板配置错误应阻止预览与生成，而不是静默跳过）。
     *
     * @param moduleCodes 分号分隔的模块编码
     * @return 有序处理器列表
     */
    public List<ReportModuleHandler> requiredPipeline(String moduleCodes) {
        List<ReportModuleHandler> pipeline = new ArrayList<>();
        if (!StringUtils.hasText(moduleCodes)) {
            return pipeline;
        }
        for (String code : moduleCodes.split(";")) {
            String trimmed = code.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            ReportModuleHandler handler = required(trimmed);
            if (pipeline.contains(handler)) {
                throw new ServiceException("报告模块在模板里重复配置：" + trimmed);
            }
            pipeline.add(handler);
        }
        return pipeline;
    }

    /**
     * 该流水线里是否至少有一个 Handler 需要「产品启用基因」。
     *
     * @param moduleCodes 分号分隔的模块编码
     * @return true 表示需要先查 product_gene
     */
    public boolean requiresProductGenes(String moduleCodes) {
        for (ReportModuleHandler handler : requiredPipeline(moduleCodes)) {
            if (handler.requiresProductGenes()) {
                return true;
            }
        }
        return false;
    }

    /** 编码做大小写统一（数据库值与代码值可能大小写不一致） */
    private static String normalize(String moduleCode) {
        return moduleCode == null ? "" : moduleCode.trim().toUpperCase(Locale.ROOT);
    }
}
