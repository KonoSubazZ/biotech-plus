package org.dromara.report.service.impl.revision;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** 所有报告制品与状态写操作共用互斥锁，事务调用保持到事务结束。 */
@Component
@RequiredArgsConstructor
public class ReportOperationLock {
    private final RedissonClient redisson;

    public <T> T call(Long reportId, Supplier<T> action) {
        RLock lock = redisson.getLock("report:operation:" + reportId);
        if (lock.isHeldByCurrentThread()) {
            return action.get();
        }
        acquire(lock);
        boolean transactional = TransactionSynchronizationManager.isSynchronizationActive();
        if (transactional) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    lock.unlock();
                }
            });
        }
        try {
            return action.get();
        } finally {
            if (!transactional) {
                lock.unlock();
            }
        }
    }

    public void run(Long reportId, Runnable action) {
        call(reportId, () -> {
            action.run();
            return Boolean.TRUE;
        });
    }

    private void acquire(RLock lock) {
        try {
            if (!lock.tryLock(0, TimeUnit.SECONDS)) {
                throw new ServiceException("报告正在处理，请稍后刷新重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("等待报告操作被中断");
        }
    }
}
