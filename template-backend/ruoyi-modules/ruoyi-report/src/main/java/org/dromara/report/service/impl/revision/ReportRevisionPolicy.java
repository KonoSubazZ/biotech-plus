package org.dromara.report.service.impl.revision;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.utils.ReportNameResolver;
import org.dromara.report.utils.ReportStatus;

import java.util.Objects;
import java.util.Set;

/** 报告重生成与定向复审口径。 */
public final class ReportRevisionPolicy {
    private static final Set<Integer> GENERATABLE = Set.of(31, 32, 33, 52);
    private static final int MAX_BASE_LENGTH = 80 - ("_V" + Integer.MAX_VALUE).length();

    private ReportRevisionPolicy() { }

    public static void requireGeneratable(int status) {
        if (!GENERATABLE.contains(status)) {
            throw new ServiceException("当前状态（" + ReportStatus.label(status) + "）不允许生成报告");
        }
    }

    public static String fileName(String base, int version) {
        return baseName(base) + "_V" + version;
    }

    public static String baseName(String base) {
        String safe = ReportNameResolver.toFileName(base);
        if (safe.isBlank()) {
            throw new ServiceException("报告名不能为空");
        }
        if (safe.length() > MAX_BASE_LENGTH) {
            safe = safe.substring(0, MAX_BASE_LENGTH);
        }
        return safe;
    }

    public static void requireReviewer(Long userId, Long producerId, Long reviewerId) {
        if (userId == null || Objects.equals(userId, producerId)) {
            throw new ServiceException("产出人不能审核自己的报告");
        }
        if (reviewerId != null && !reviewerId.equals(userId)) {
            throw new ServiceException("本次报告须由原审核人复审");
        }
    }
}
