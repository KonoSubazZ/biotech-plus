package org.dromara.test.audit;

import org.dromara.audit.service.impl.AuditRecorder;
import org.mockito.Mockito;
import java.util.function.Supplier;

/** 业务单元测试只验证原业务规则；审计行为由审计测试单独验证。 */
public final class AuditTestSupport {
    private AuditTestSupport() { }

    public static AuditRecorder recorder() {
        AuditRecorder recorder = Mockito.mock(AuditRecorder.class);
        Mockito.when(recorder.execute(Mockito.anyString(), Mockito.anyList(), Mockito.any()))
            .thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(2).get());
        Mockito.when(recorder.executeNew(Mockito.anyString(), Mockito.anyList(), Mockito.any()))
            .thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(2).get());
        return recorder;
    }
}
