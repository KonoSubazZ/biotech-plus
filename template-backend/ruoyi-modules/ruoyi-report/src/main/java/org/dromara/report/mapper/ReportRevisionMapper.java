package org.dromara.report.mapper;

import org.apache.ibatis.annotations.Param;
import java.util.Map;

/** 报告版本与复审的条件更新，SQL 统一落 XML。 */
public interface ReportRevisionMapper {
    int ensureSequence(@Param("row") Map<String, Object> row);
    Map<String, Object> lockSequence(@Param("row") Map<String, Object> row);
    int claimSequence(@Param("id") Long id, @Param("reportId") Long reportId);
    int startGeneration(@Param("row") Map<String, Object> row);
    int completeVersion(@Param("reportId") Long reportId);
    int submitReview(@Param("reportId") Long reportId, @Param("reviewerId") Long reviewerId);
    int countReviewer(@Param("userId") Long userId, @Param("tenantId") String tenantId);
    int countBadFiles(@Param("analysisId") Long analysisId);
    int countFiles(@Param("analysisId") Long analysisId);
    Long selectChild(@Param("sourceReportId") Long sourceReportId);
    int markSent(@Param("reportId") Long reportId, @Param("userId") Long userId);
    int countUnconfirmedSend(@Param("reportId") Long reportId);
    Map<String, Object> lockSequenceById(@Param("id") Long id);
    int rememberBaseName(@Param("id") Long id, @Param("baseName") String baseName);
}
