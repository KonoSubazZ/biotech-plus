package org.dromara.test.report;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.service.impl.revision.ReportRevisionPolicy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("local")
class ReportRevisionPolicyTest {
    @Test
    void generationWhitelistIncludesSignatureFailureOnly() {
        for (int status : new int[]{31, 32, 33, 52}) {
            Assertions.assertDoesNotThrow(() -> ReportRevisionPolicy.requireGeneratable(status));
        }
        for (int status : new int[]{20, 21, 30, 40, 41, 42, 43, 50, 53, 60, 61, 62}) {
            Assertions.assertThrows(ServiceException.class,
                () -> ReportRevisionPolicy.requireGeneratable(status));
        }
    }

    @Test
    void versionSuffixSurvivesLongAndUnsafeNames() {
        String name = ReportRevisionPolicy.fileName("客户/" + "报告".repeat(100), 12);
        Assertions.assertTrue(name.endsWith("_V12"));
        Assertions.assertFalse(name.contains("/"));
        Assertions.assertTrue(name.length() <= 80);
        Assertions.assertEquals("客户_样本_模板_V1",
            ReportRevisionPolicy.fileName("客户_样本_模板", 1));
    }

    @Test
    void onlyTheAssignedIndependentReviewerCanReview() {
        Assertions.assertDoesNotThrow(() -> ReportRevisionPolicy.requireReviewer(2L, 1L, 2L));
        Assertions.assertDoesNotThrow(() -> ReportRevisionPolicy.requireReviewer(3L, 1L, null));
        Assertions.assertThrows(ServiceException.class,
            () -> ReportRevisionPolicy.requireReviewer(3L, 1L, 2L));
        Assertions.assertThrows(ServiceException.class,
            () -> ReportRevisionPolicy.requireReviewer(1L, 1L, 1L));
    }

    @Test
    void longNameBaseDoesNotChangeWhenVersionGainsADigit() {
        String input = "报告".repeat(100);
        String ninth = ReportRevisionPolicy.fileName(input, 9);
        String tenth = ReportRevisionPolicy.fileName(input, 10);
        Assertions.assertEquals(ninth.substring(0, ninth.length() - 3),
            tenth.substring(0, tenth.length() - 4));
        Assertions.assertTrue(ReportRevisionPolicy.fileName(input, Integer.MAX_VALUE).length() <= 80);
    }
}
