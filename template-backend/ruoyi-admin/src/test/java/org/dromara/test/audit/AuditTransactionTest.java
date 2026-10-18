package org.dromara.test.audit;

import org.dromara.audit.domain.dto.AuditCapture;
import org.dromara.audit.domain.dto.AuditSource;
import org.dromara.audit.domain.dto.AuditTarget;
import org.dromara.audit.mapper.AuditSourceMapper;
import org.dromara.audit.service.impl.AuditRecorder;
import org.dromara.audit.service.impl.AuditWriter;
import org.dromara.common.satoken.utils.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 使用真实Spring事务协调器验证提交与回滚，不依赖伪造的提交回调。 */
@Tag("local")
class AuditTransactionTest {
    private Connection connection;
    private TransactionTemplate transactions;
    private AuditSourceMapper sourceMapper;
    private AuditWriter writer;
    private AuditRecorder recorder;

    @BeforeEach
    void setup() throws SQLException {
        connection = mock(Connection.class);
        when(connection.getAutoCommit()).thenReturn(true);
        DataSource source = mock(DataSource.class);
        when(source.getConnection()).thenReturn(connection);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(source));
        sourceMapper = mock(AuditSourceMapper.class);
        writer = mock(AuditWriter.class);
        recorder = new AuditRecorder(transactions, sourceMapper, writer);
    }

    private List<AuditTarget> target() {
        return List.of(AuditTarget.rows(AuditSource.SAMPLE, "subbarcode", "sample"));
    }

    @Test
    void nestedWritesMergeWithinSameTenantTransaction() throws SQLException {
        TenantContext.withTenant("tenant-a", () -> recorder.execute("导入", target(), () -> {
            recorder.execute("内部更新", target(), () -> true);
            return true;
        }));
        verify(writer, times(1)).write(any());
        verify(connection).commit();
    }

    @Test
    void tenantSwitchCreatesSeparateCapturesAndRestoresEarlierScope() {
        transactions.executeWithoutResult(status -> {
            TenantContext.withTenant("tenant-a", () -> recorder.execute("A", target(), () -> true));
            TenantContext.withTenant("tenant-b", () -> recorder.execute("B", target(), () -> true));
            TenantContext.withTenant("tenant-a", () -> recorder.execute("A再次", target(), () -> true));
        });
        ArgumentCaptor<AuditCapture> captures = ArgumentCaptor.forClass(AuditCapture.class);
        verify(writer, times(2)).write(captures.capture());
        assertEquals(List.of("tenant-a", "tenant-b"),
            captures.getAllValues().stream().map(AuditCapture::getTenantId).toList());
    }

    @Test
    void businessFailureRollsBackWithoutSuccessEvidence() throws SQLException {
        assertThrows(IllegalStateException.class, () -> TenantContext.withTenant("tenant-a",
            () -> recorder.execute("修改", target(), () -> { throw new IllegalStateException("business failure"); })));
        verify(writer, never()).write(any());
        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    @Test
    void auditFailureRollsBackBusinessTransaction() throws SQLException {
        doThrow(new IllegalStateException("audit unavailable")).when(writer).write(any());
        assertThrows(IllegalStateException.class, () -> TenantContext.withTenant("tenant-a",
            () -> recorder.execute("修改", target(), () -> true)));
        verify(connection).rollback();
        verify(connection, never()).commit();
    }
}
