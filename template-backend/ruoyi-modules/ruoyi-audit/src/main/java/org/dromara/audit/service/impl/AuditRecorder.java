package org.dromara.audit.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.audit.domain.dto.AuditCapture;
import org.dromara.audit.domain.dto.AuditTarget;
import org.dromara.audit.mapper.AuditSourceMapper;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.satoken.utils.TenantContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** 显式业务入口调用；同一事务内合并多表变化，写入失败共同回滚。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditRecorder {
    private final TransactionTemplate transactionTemplate;
    private final AuditSourceMapper sourceMapper;
    private final AuditWriter writer;

    public <T> T execute(String action, List<AuditTarget> targets, Supplier<T> operation) {
        return execute(action, targets, null, operation);
    }

    /** 外部进程调用前后各自登记过程事实，不宣称捕获了外部直接写库的逐行差异。 */
    public void process(String action, String outcome, List<AuditTarget> targets) {
        execute(action, targets, outcome, () -> true);
    }

    private <T> T execute(String action, List<AuditTarget> targets, String outcome, Supplier<T> operation) {
        return transactionTemplate.execute(status -> {
            AuditSynchronization synchronization = current();
            if (synchronization == null) {
                AuditCapture capture = newCapture(action);
                capture.setOutcome(outcome);
                synchronization = new AuditSynchronization(capture);
                TransactionSynchronizationManager.registerSynchronization(synchronization);
            }
            synchronization.watch(targets);
            return operation.get();
        });
    }

    /** afterCommit 中的失败处置必须真正开启新事务，不能加入已提交的资源。 */
    public <T> T executeNew(String action, List<AuditTarget> targets, Supplier<T> operation) {
        TransactionTemplate separate = new TransactionTemplate(transactionTemplate.getTransactionManager());
        separate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return separate.execute(status -> execute(action, targets, operation));
    }

    /** 新建主记录得到真实主键后、写入其关联表之前登记比较范围。 */
    public void watch(List<AuditTarget> targets) {
        AuditSynchronization synchronization = current();
        if (synchronization == null) {
            throw new IllegalStateException("审计比较范围必须登记在受控事务内");
        }
        synchronization.watch(targets);
    }

    private AuditSynchronization current() {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            if (synchronization instanceof AuditSynchronization audit && audit.sameScope()) {
                return audit;
            }
        }
        return null; // 当前事务尚无审计上下文，由显式业务入口创建。
    }

    private AuditCapture newCapture(String action) {
        AuditCapture capture = new AuditCapture();
        capture.setAction(action);
        capture.setOperationId(UUID.randomUUID().toString().replace("-", ""));
        capture.setOccurredAt(new Date());
        capture.setUnrestricted(TenantContext.isAdmin() || TenantContext.isIgnored());
        if (!capture.isUnrestricted()) {
            capture.setTenantId(TenantContext.getTenantId());
        }
        LoginUser user = null;
        try {
            user = LoginHelper.getLoginUser();
        } catch (RuntimeException exception) {
            log.debug("审计当前操作无登录上下文，使用受控服务身份", exception);
        }
        if (user == null) {
            capture.setActorId("SYSTEM");
            capture.setActorName("系统或上游服务");
            capture.setSource("SERVICE");
        } else {
            capture.setActorId(String.valueOf(user.getUserId()));
            capture.setActorName(user.getUsername());
            capture.setSource("USER");
        }
        return capture;
    }

    private class AuditSynchronization implements TransactionSynchronization {
        private final AuditCapture capture;

        AuditSynchronization(AuditCapture capture) {
            this.capture = capture;
        }

        boolean sameScope() {
            boolean unrestricted = TenantContext.isAdmin() || TenantContext.isIgnored();
            if (capture.isUnrestricted() != unrestricted) {
                return false;
            }
            return unrestricted || capture.getTenantId().equals(TenantContext.getTenantId());
        }

        void watch(List<AuditTarget> targets) {
            for (AuditTarget target : targets) {
                capture.getBefore().computeIfAbsent(target, item -> sourceMapper.snapshot(item,
                    capture.getTenantId(), !capture.isUnrestricted()));
            }
        }

        @Override
        public void beforeCommit(boolean readOnly) {
            if (!readOnly) {
                writer.write(capture);
            }
        }
    }
}
