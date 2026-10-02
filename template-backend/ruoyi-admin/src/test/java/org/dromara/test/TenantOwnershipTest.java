package org.dromara.test;

import org.apache.ibatis.reflection.SystemMetaObject;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.service.TenantService;
import org.dromara.common.core.utils.SpringUtils;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.mybatis.handler.InjectionMetaObjectHandler;
import org.dromara.common.satoken.utils.TenantContext;
import org.dromara.system.domain.SysNotice;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysRoleMenuMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import org.dromara.system.service.impl.SysRoleServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class TenantOwnershipTest {
    @Test
    void forgedTenantIsOverwrittenBeforeInsert() {
        TenantService service = mock(TenantService.class);
        try (var context = mockStatic(TenantContext.class); var spring = mockStatic(SpringUtil.class)) {
            context.when(TenantContext::getTenantId).thenReturn("tenant-a");
            context.when(TenantContext::isAdmin).thenReturn(false);
            spring.when(() -> SpringUtils.getBean(TenantService.class)).thenReturn(service);
            SysNotice notice = new SysNotice();
            notice.setTenantId("tenant-b");
            new InjectionMetaObjectHandler().insertFill(SystemMetaObject.forObject(notice));
            assertEquals("tenant-a", notice.getTenantId());
            verify(service).checkTenant("tenant-a");
        }
    }

    @Test
    void adminCanChooseAnExistingTenantForInsert() {
        TenantService service = mock(TenantService.class);
        try (var context = mockStatic(TenantContext.class); var spring = mockStatic(SpringUtil.class)) {
            context.when(TenantContext::getTenantId).thenReturn("000000");
            context.when(TenantContext::isAdmin).thenReturn(true);
            spring.when(() -> SpringUtils.getBean(TenantService.class)).thenReturn(service);
            SysNotice notice = new SysNotice();
            notice.setTenantId("tenant-b");
            new InjectionMetaObjectHandler().insertFill(SystemMetaObject.forObject(notice));
            assertEquals("tenant-b", notice.getTenantId());
            verify(service).checkTenant("tenant-b");
        }
    }

    @Test
    void mixedTenantBatchCannotDeleteOrInvalidateSessions() {
        TenantService service = mock(TenantService.class);
        SysUserRoleMapper relations = mock(SysUserRoleMapper.class);
        when(service.getUserTenantId(10L)).thenReturn("tenant-a");
        when(service.getUserTenantId(20L)).thenThrow(new ServiceException("无权访问"));
        var roles = new SysRoleServiceImpl(mock(SysRoleMapper.class), mock(SysRoleMenuMapper.class), relations, service);
        assertThrows(ServiceException.class, () -> roles.deleteAuthUsers(2L, new Long[]{10L, 20L}));
        verifyNoInteractions(relations);
    }
}
