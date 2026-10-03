package org.dromara.qc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.qc.domain.QcRecord;
import org.dromara.qc.domain.bo.QcRecordBo;
import org.dromara.qc.domain.vo.QcRecordVo;
import org.dromara.qc.mapper.QcRecordMapper;
import org.dromara.qc.service.IQcRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 质控记录 业务层处理
 * <p>
 * 风格要点（对齐 template-backend/docs/backend-code-standard.md）：
 * 查询条件集中在 buildQueryWrapper 里「声明 → 基础条件 → 分支条件 → 排序」展开，
 * 不在方法体内堆链式调用；缺数据抛明确异常，不返回裸 null。
 *
 * @author <你的名字>
 */
@RequiredArgsConstructor
@Service
public class QcRecordServiceImpl implements IQcRecordService {

    private final QcRecordMapper baseMapper;

    @Override
    public TableDataInfo<QcRecordVo> selectPageList(QcRecordBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<QcRecord> wrapper = buildQueryWrapper(bo);
        Page<QcRecordVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public QcRecordVo queryById(Long id) {
        QcRecordVo vo = baseMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("质控记录不存在：id=" + id);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(QcRecordBo bo) {
        QcRecord entity = MapstructUtils.convert(bo, QcRecord.class);
        return baseMapper.insert(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(QcRecordBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("修改质控记录必须提供主键");
        }
        // 先确认记录存在，避免把「改不到」当成「改成功」
        queryById(bo.getId());
        QcRecord entity = MapstructUtils.convert(bo, QcRecord.class);
        return baseMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要删除的质控记录");
        }
        baseMapper.deleteByIds(ids);
        return Boolean.TRUE;
    }

    /**
     * 组装查询条件：声明 Wrapper → 基础条件 → 分支条件 → 排序。
     * <p>
     * 样本条码/质控项目按模糊匹配；人工状态/质控类别按等值匹配 —— 两个页面（湿实验质控、
     * 生信质控）共用本接口，靠 qcCategory 区分，由前端页面固定传入。
     */
    private LambdaQueryWrapper<QcRecord> buildQueryWrapper(QcRecordBo bo) {
        LambdaQueryWrapper<QcRecord> wrapper = Wrappers.lambdaQuery();

        wrapper.like(StringUtils.isNotBlank(bo.getSubbarcode()), QcRecord::getSubbarcode, bo.getSubbarcode());
        wrapper.like(StringUtils.isNotBlank(bo.getQcItem()), QcRecord::getQcItem, bo.getQcItem());

        if (StringUtils.isNotBlank(bo.getStatus())) {
            wrapper.eq(QcRecord::getStatus, bo.getStatus());
        }
        if (StringUtils.isNotBlank(bo.getQcCategory())) {
            wrapper.eq(QcRecord::getQcCategory, bo.getQcCategory());
        }

        wrapper.orderByDesc(QcRecord::getId);
        return wrapper;
    }
}
