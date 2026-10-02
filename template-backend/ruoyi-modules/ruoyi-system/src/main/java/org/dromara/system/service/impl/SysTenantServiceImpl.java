package org.dromara.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.service.TenantService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.satoken.utils.TenantContext;
import org.dromara.system.domain.SysTenant;
import org.dromara.system.mapper.SysTenantMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysTenantServiceImpl implements TenantService {
    private final SysTenantMapper mapper;
    private final SysUserMapper userMapper;
    private final DataSource dataSource;

    public TableDataInfo<SysTenant> list(SysTenant query, PageQuery page) {
        return TableDataInfo.build(mapper.selectPage(page.build(), new LambdaQueryWrapper<SysTenant>()
            .like(query.getTenantName() != null && !query.getTenantName().isBlank(), SysTenant::getTenantName, query.getTenantName())
            .eq(query.getTenantId() != null && !query.getTenantId().isBlank(), SysTenant::getTenantId, query.getTenantId())
            .eq(query.getStatus() != null && !query.getStatus().isBlank(), SysTenant::getStatus, query.getStatus())
            .orderByAsc(SysTenant::getId)));
    }

    public List<SysTenant> options() {
        boolean isSuperAdmin = LoginHelper.isSuperAdmin();
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysTenant::getId, SysTenant::getTenantId, SysTenant::getTenantName, SysTenant::getStatus)
            .eq(SysTenant::getStatus, "0");

        if (!isSuperAdmin) {
            wrapper.eq(SysTenant::getTenantId, TenantContext.getTenantId());
        }

        wrapper.orderByAsc(SysTenant::getId);
        return mapper.selectList(wrapper);
    }

    public SysTenant detail(Long id) {
        SysTenant tenant = mapper.selectById(id);
        if (tenant == null) throw new ServiceException("租户不存在");
        return tenant;
    }

    public int create(SysTenant tenant) {
        if (TenantContext.PLATFORM_LOG_TENANT_ID.equals(tenant.getTenantId())) {
            throw new ServiceException("该租户编号为系统保留标识");
        }
        tenant.setId(null);
        if (mapper.exists(new LambdaQueryWrapper<SysTenant>().eq(SysTenant::getTenantId, tenant.getTenantId()))) {
            throw new ServiceException("租户编号已存在");
        }
        return mapper.insert(tenant);
    }

    public int update(SysTenant tenant) {
        if (tenant.getId() == null) throw new ServiceException("租户ID不能为空");
        SysTenant original = detail(tenant.getId());
        if (!original.getTenantId().equals(tenant.getTenantId())) throw new ServiceException("租户编号创建后不能修改");
        if (TenantContext.DEFAULT_TENANT_ID.equals(tenant.getTenantId()) && !"0".equals(tenant.getStatus())) {
            throw new ServiceException("默认租户不能停用");
        }
        return mapper.updateById(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    public int delete(List<Long> ids) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<String> tables = jdbc.queryForList("SELECT TABLE_NAME FROM information_schema.COLUMNS "
            + "WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME = 'tenant_id' AND TABLE_NAME <> 'sys_tenant'", String.class);
        for (Long id : ids) {
            SysTenant tenant = detail(id);
            if (TenantContext.DEFAULT_TENANT_ID.equals(tenant.getTenantId())) throw new ServiceException("默认租户不能删除");
            for (String table : tables) {
                if (!table.matches("[A-Za-z0-9_]+")) throw new ServiceException("无效的业务表名");
                Long count = jdbc.queryForObject("SELECT COUNT(*) FROM `" + table + "` WHERE tenant_id = ?", Long.class, tenant.getTenantId());
                if (count != null && count > 0) throw new ServiceException("租户仍有关联用户或数据，请停用租户");
            }
        }
        return mapper.deleteByIds(ids);
    }

    @Override
    public void checkTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) throw new ServiceException("用户必须归属一个租户");
        SysTenant tenant = mapper.selectOne(new LambdaQueryWrapper<SysTenant>().eq(SysTenant::getTenantId, tenantId));
        if (tenant == null || !"0".equals(tenant.getStatus())) throw new ServiceException("租户不存在或已停用");
    }

    @Override
    public String getUserTenantId(Long userId) {
        var user = userMapper.selectVoById(userId);
        if (user == null) throw new ServiceException("用户不存在或无权访问");
        return user.getTenantId();
    }
}
