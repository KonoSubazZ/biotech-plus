package org.dromara.test.report;

import org.dromara.report.service.impl.revision.ReportOperationLock;
import org.mockito.Mockito;
import java.util.function.Supplier;

/** 服务单测直执临界区；锁本身由 ReportOperationLockTest 独立验证。 */
final class ReportTestLocks {
    private ReportTestLocks() { }

    static ReportOperationLock direct() {
        ReportOperationLock lock = Mockito.mock(ReportOperationLock.class);
        Mockito.when(lock.call(Mockito.anyLong(), Mockito.any())).thenAnswer(invocation -> {
            Supplier<?> action = invocation.getArgument(1);
            return action.get();
        });
        Mockito.doAnswer(invocation -> {
            Runnable action = invocation.getArgument(1);
            action.run();
            return Boolean.TRUE;
        }).when(lock).run(Mockito.anyLong(), Mockito.any());
        return lock;
    }
}
