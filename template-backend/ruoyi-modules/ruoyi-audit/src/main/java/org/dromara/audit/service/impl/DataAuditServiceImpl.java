package org.dromara.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.audit.domain.AuditChange;
import org.dromara.audit.domain.DataAudit;
import org.dromara.audit.domain.bo.DataAuditBo;
import org.dromara.audit.domain.vo.AuditChangeVo;
import org.dromara.audit.domain.vo.DataAuditDetailVo;
import org.dromara.audit.domain.vo.DataAuditVo;
import org.dromara.audit.mapper.AuditChangeMapper;
import org.dromara.audit.mapper.DataAuditMapper;
import org.dromara.audit.service.IDataAuditService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

/** 租户拦截器同时限制列表和详情；数据审计权限不授予平台身份。 */
@Service
@RequiredArgsConstructor
public class DataAuditServiceImpl implements IDataAuditService {
    private final DataAuditMapper eventMapper;
    private final AuditChangeMapper changeMapper;

    @Override
    public TableDataInfo<DataAuditVo> selectPageList(DataAuditBo bo, PageQuery pageQuery) {
        int pageSize = pageQuery.getPageSize() == null ? 20 : pageQuery.getPageSize();
        pageQuery.setPageSize(Math.max(1, Math.min(pageSize, 200)));
        pageQuery.setOrderByColumn(null);
        pageQuery.setIsAsc(null);
        LambdaQueryWrapper<DataAudit> wrapper = buildQuery(bo);
        Page<DataAuditVo> page = eventMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    private LambdaQueryWrapper<DataAudit> buildQuery(DataAuditBo bo) {
        LambdaQueryWrapper<DataAudit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(bo.getSubbarcode()), DataAudit::getSubbarcode, bo.getSubbarcode());
        wrapper.eq(StringUtils.isNotBlank(bo.getAuditScope()), DataAudit::getAuditScope, bo.getAuditScope());
        wrapper.like(StringUtils.isNotBlank(bo.getAction()), DataAudit::getAction, bo.getAction());
        wrapper.like(StringUtils.isNotBlank(bo.getActorName()), DataAudit::getActorName, bo.getActorName());
        if (bo.getAnalysisId() != null) {
            if (Boolean.TRUE.equals(bo.getIncludeSampleCommon()) && StringUtils.isNotBlank(bo.getSubbarcode())) {
                wrapper.and(scope -> scope.eq(DataAudit::getAnalysisId, bo.getAnalysisId())
                    .or().isNull(DataAudit::getAnalysisId));
            } else {
                wrapper.eq(DataAudit::getAnalysisId, bo.getAnalysisId());
            }
        }
        wrapper.eq(bo.getReportId() != null, DataAudit::getReportId, bo.getReportId());
        wrapper.ge(StringUtils.isNotBlank(bo.getBeginTime()), DataAudit::getOccurredAt, bo.getBeginTime());
        wrapper.le(StringUtils.isNotBlank(bo.getEndTime()), DataAudit::getOccurredAt, bo.getEndTime());
        if (Boolean.TRUE.equals(bo.getAscending())) {
            wrapper.orderByAsc(DataAudit::getOccurredAt, DataAudit::getId);
        } else {
            wrapper.orderByDesc(DataAudit::getOccurredAt, DataAudit::getId);
        }
        return wrapper;
    }

    @Override
    public DataAuditDetailVo queryById(Long id) {
        DataAuditVo event = eventMapper.selectVoById(id);
        if (event == null) {
            throw new ServiceException("审计记录不存在或无权查看");
        }
        LambdaQueryWrapper<AuditChange> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditChange::getEventId, id);
        wrapper.orderByAsc(AuditChange::getId);
        java.util.List<AuditChangeVo> changes = changeMapper.selectVoList(wrapper);
        DataAuditDetailVo detail = new DataAuditDetailVo();
        detail.setEvent(event);
        detail.setChanges(changes);
        return detail;
    }
}
