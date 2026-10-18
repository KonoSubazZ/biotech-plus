package org.dromara.audit.domain.dto;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.exception.ServiceException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** 需要比较的真实记录范围，值参数始终绑定，不接收客户端表名或列名。 */
@Getter
@EqualsAndHashCode
public class AuditTarget {
    private final AuditSource source;
    private final String keyColumn;
    private final List<String> keys;

    private AuditTarget(AuditSource source, String column, Collection<?> values) {
        if (!source.getPrimaryKeys().contains(column) && !source.getSelectors().contains(column)) {
            throw new ServiceException("审计定位列不在白名单");
        }
        this.source = source;
        this.keyColumn = column;
        this.keys = values.stream().filter(Objects::nonNull).map(String::valueOf).distinct().toList();
    }

    public String getTableName() {
        return source.getTableName();
    }

    public static AuditTarget rows(AuditSource source, String column, Object... keys) {
        return new AuditTarget(source, column, Arrays.asList(keys));
    }

    public static AuditTarget ids(AuditSource source, String column, Collection<?> keys) {
        return new AuditTarget(source, column, keys == null ? List.of() : keys);
    }

    public static List<AuditTarget> users(Collection<?> ids) {
        return List.of(new AuditTarget(AuditSource.USER, "user_id", ids),
            new AuditTarget(AuditSource.USER_ROLE, "user_id", ids));
    }

    public static List<AuditTarget> roles(Collection<?> ids) {
        return List.of(new AuditTarget(AuditSource.ROLE, "role_id", ids),
            new AuditTarget(AuditSource.ROLE_MENU, "role_id", ids));
    }

    public static List<AuditTarget> report(Long reportId) {
        return List.of(rows(AuditSource.REPORT, "report_id", reportId),
            rows(AuditSource.REPORT_HISTORY, "report_id", reportId),
            rows(AuditSource.EMAIL, "report_id", reportId),
            rows(AuditSource.SEQUENCE, "report_id", reportId));
    }

    public static List<AuditTarget> analysis(Long analysisId) {
        List<AuditTarget> result = new ArrayList<>();
        result.add(rows(AuditSource.ANALYSIS, "analysis_id", analysisId));
        result.add(rows(AuditSource.REPORT, "analysis_id", analysisId));
        result.add(rows(AuditSource.FILE, "analysis_id", analysisId));
        return result;
    }
    public static List<AuditTarget> variant(Long analysisId, String type, Long sourceId) {
        AuditSource source = switch (type) {
            case "SNP_INDEL" -> AuditSource.SNV;
            case "CNV" -> AuditSource.CNV;
            case "FUSION" -> AuditSource.FUSION;
            case "CR_ALL" -> AuditSource.GERMLINE;
            default -> throw new ServiceException("不支持的位点类型");
        };
        return List.of(rows(AuditSource.ANALYSIS, "analysis_id", analysisId), rows(source, "id", sourceId));
    }

    public static List<AuditTarget> reportVariant(Long reportId, Long analysisId, String type, Long sourceId) {
        List<AuditTarget> targets = new ArrayList<>(report(reportId));
        targets.addAll(variant(analysisId, type, sourceId));
        return targets;
    }

}
