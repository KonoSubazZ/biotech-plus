package org.dromara.report.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 人工改靶父级 ID 的处理（源位点表 {@code parent_mutation_id} 是逗号分隔串）。
 * <p>
 * 解析（读库列）、去重（前端选择）、拼串（写库 + 进匹配键）三个口径必须完全一致，
 * 所以集中在这一个工具里，谁都不许各写一套。
 *
 * @author <你的名字>
 */
final class ParentMutationIds {

    private ParentMutationIds() {
    }

    /**
     * 源位点表 {@code parent_mutation_id}（逗号分隔）→ 父级ID列表，空段忽略。
     *
     * @param raw 库里的原始值（可为空）
     * @return 父级ID列表；无值时返回空列表
     */
    static List<Long> parse(Object raw) {
        List<Long> ids = new ArrayList<>();
        if (raw == null) {
            return ids;
        }
        for (String part : String.valueOf(raw).split(",")) {
            String value = part.trim();
            if (!value.isEmpty()) {
                ids.add(Long.valueOf(value));
            }
        }
        return ids;
    }

    /**
     * 去重：保持传入顺序、忽略 null。
     *
     * @param ids 原始ID列表（可为空）
     * @return 去重后的ID列表
     */
    static List<Long> distinct(List<Long> ids) {
        List<Long> result = new ArrayList<>();
        if (ids == null) {
            return result;
        }
        for (Long id : ids) {
            if (id != null && !result.contains(id)) {
                result.add(id);
            }
        }
        return result;
    }

    /**
     * 入库与匹配键用的父级串：去重后<b>升序</b>拼串
     * （顺序不影响匹配键，保证同输入必同键），未改靶返回空串。
     *
     * @param ids 父级ID列表（可为空）
     * @return 逗号分隔的升序串；无值时为空串
     */
    static String toKey(List<Long> ids) {
        List<Long> sorted = distinct(ids);
        Collections.sort(sorted);
        List<String> parts = new ArrayList<>();
        for (Long id : sorted) {
            parts.add(String.valueOf(id));
        }
        return String.join(",", parts);
    }
}
