package org.dromara.test.report;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.service.impl.revision.ReportOperationLock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.concurrent.TimeUnit;

@Tag("local")
class ReportOperationLockTest {
    private final RedissonClient redisson = Mockito.mock(RedissonClient.class);
    private final RLock lock = Mockito.mock(RLock.class);
    private final ReportOperationLock guard = new ReportOperationLock(redisson);

    @Test
    void busyReportRejectsMutation() throws Exception {
        Mockito.when(redisson.getLock("report:operation:7")).thenReturn(lock);
        Mockito.when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(false);
        Assertions.assertThrows(ServiceException.class, () -> guard.call(7L, () -> "changed"));
        Mockito.verify(lock, Mockito.never()).unlock();
    }

    @Test
    void transactionHoldsLockUntilCompletion() throws Exception {
        Mockito.when(redisson.getLock("report:operation:7")).thenReturn(lock);
        Mockito.when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(true);
        TransactionSynchronizationManager.initSynchronization();
        try {
            Assertions.assertEquals("done", guard.call(7L, () -> "done"));
            Mockito.verify(lock, Mockito.never()).unlock();
            TransactionSynchronizationManager.getSynchronizations().get(0)
                .afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            Mockito.verify(lock).unlock();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void failedNonTransactionalMutationReleasesTheLock() throws Exception {
        Mockito.when(redisson.getLock("report:operation:7")).thenReturn(lock);
        Mockito.when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(true);
        Assertions.assertThrows(ServiceException.class, () -> guard.call(7L, () -> {
            throw new ServiceException("render failed");
        }));
        Mockito.verify(lock).unlock();
    }
}
