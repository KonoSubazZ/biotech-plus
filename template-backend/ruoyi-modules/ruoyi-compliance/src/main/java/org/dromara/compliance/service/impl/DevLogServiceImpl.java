package org.dromara.compliance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.compliance.domain.DevLog;
import org.dromara.compliance.domain.bo.DevLogBo;
import org.dromara.compliance.domain.vo.DevLogVo;
import org.dromara.compliance.mapper.DevLogMapper;
import org.dromara.compliance.service.IDevLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 开发记录 业务层处理
 * <p>
 * 风格要点（对齐 template-backend/docs/backend-code-standard.md）：
 * 查询条件集中在 buildQueryWrapper 里「声明 → 基础条件 → 分支条件 → 排序」展开，
 * 不在方法体内堆链式调用；缺数据抛明确异常，不返回裸 null。
 * <p>
 * append-only：只提供新增 / 修改，不提供删除（合规上开发记录不可删改）。
 *
 * @author liushangzhi
 */
@RequiredArgsConstructor
@Service
public class DevLogServiceImpl implements IDevLogService {

    private final DevLogMapper baseMapper;

    @Override
    public TableDataInfo<DevLogVo> selectPageList(DevLogBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DevLog> wrapper = buildQueryWrapper(bo);
        Page<DevLogVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public DevLogVo queryById(Long id) {
        DevLogVo vo = baseMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("开发记录不存在：id=" + id);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(DevLogBo bo) {
        DevLog entity = MapstructUtils.convert(bo, DevLog.class);
        return baseMapper.insert(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(DevLogBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("修改开发记录必须提供主键");
        }
        // 先确认记录存在，避免把「改不到」当成「改成功」
        queryById(bo.getId());
        DevLog entity = MapstructUtils.convert(bo, DevLog.class);
        return baseMapper.updateById(entity) > 0;
    }

    /**
     * 组装查询条件：声明 Wrapper → 基础条件 → 分支条件 → 排序。
     * 排序按设计文档 dev-log.md §2：记录日期倒序，同日期再按主键倒序。
     */
    private LambdaQueryWrapper<DevLog> buildQueryWrapper(DevLogBo bo) {
        LambdaQueryWrapper<DevLog> wrapper = Wrappers.lambdaQuery();

        wrapper.like(StringUtils.isNotBlank(bo.getTitle()), DevLog::getTitle, bo.getTitle());
        wrapper.like(StringUtils.isNotBlank(bo.getDeveloper()), DevLog::getDeveloper, bo.getDeveloper());

        if (StringUtils.isNotBlank(bo.getCategory())) {
            wrapper.eq(DevLog::getCategory, bo.getCategory());
        }

        wrapper.orderByDesc(DevLog::getLogDate);
        wrapper.orderByDesc(DevLog::getId);
        return wrapper;
    }
}
