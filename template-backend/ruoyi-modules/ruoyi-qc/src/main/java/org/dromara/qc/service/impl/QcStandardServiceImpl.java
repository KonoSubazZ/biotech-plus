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
import org.dromara.qc.domain.QcStandard;
import org.dromara.qc.domain.bo.QcStandardBo;
import org.dromara.qc.domain.vo.QcStandardVo;
import org.dromara.qc.mapper.QcStandardMapper;
import org.dromara.qc.service.IQcStandardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 质控标准 业务层处理
 * <p>
 * 风格要点（对齐 template-backend/docs/backend-code-standard.md）：
 * 查询条件集中在 buildQueryWrapper 里「声明 → 基础条件 → 分支条件 → 排序」展开，
 * 不在方法体内堆链式调用；缺数据抛明确异常，不返回裸 null。
 *
 * @author <你的名字>
 */
@RequiredArgsConstructor
@Service
public class QcStandardServiceImpl implements IQcStandardService {

    private final QcStandardMapper baseMapper;

    @Override
    public TableDataInfo<QcStandardVo> selectPageList(QcStandardBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<QcStandard> wrapper = buildQueryWrapper(bo);
        Page<QcStandardVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<QcStandardVo> selectActiveList() {
        LambdaQueryWrapper<QcStandard> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(QcStandard::getStatus, "active");
        wrapper.orderByAsc(QcStandard::getId);
        return baseMapper.selectVoList(wrapper);
    }

    @Override
    public QcStandardVo queryById(Long id) {
        QcStandardVo vo = baseMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("质控标准不存在：id=" + id);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(QcStandardBo bo) {
        validateUniqueRule(bo, null);
        QcStandard entity = MapstructUtils.convert(bo, QcStandard.class);
        return baseMapper.insert(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(QcStandardBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("修改质控标准必须提供主键");
        }
        // 先确认记录存在，避免把「改不到」当成「改成功」
        queryById(bo.getId());
        validateUniqueRule(bo, bo.getId());
        QcStandard entity = MapstructUtils.convert(bo, QcStandard.class);
        return baseMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要删除的质控标准");
        }
        for (Long id : ids) {
            assertNotReferenced(id);
        }
        baseMapper.deleteByIds(ids);
        return Boolean.TRUE;
    }

    /**
     * 组装查询条件：声明 Wrapper → 基础条件 → 分支条件 → 排序。
     * 权限/租户等上下文在分支内读取，避免同一判断重复计算。
     */
    private LambdaQueryWrapper<QcStandard> buildQueryWrapper(QcStandardBo bo) {
        LambdaQueryWrapper<QcStandard> wrapper = Wrappers.lambdaQuery();

        // 按 qc_standard 的实际字段过滤（product_id / qc_item / qc_category / status）
        wrapper.eq(bo.getProductId() != null, QcStandard::getProductId, bo.getProductId());
        wrapper.like(StringUtils.isNotBlank(bo.getQcItem()), QcStandard::getQcItem, bo.getQcItem());
        wrapper.eq(StringUtils.isNotBlank(bo.getQcCategory()), QcStandard::getQcCategory, bo.getQcCategory());

        if (StringUtils.isNotBlank(bo.getStatus())) {
            wrapper.eq(QcStandard::getStatus, bo.getStatus());
        }

        wrapper.orderByAsc(QcStandard::getId);
        return wrapper;
    }

    /**
     * 校验「同一产品 + 同一质控项目 + 同一类别」唯一（对应表上的
     * uk_qc_standard_product_item_category）。租户条件由租户拦截器自动加，这里不手写。
     * 排除自身，避免改其它字段时误判冲突。
     */
    private void validateUniqueRule(QcStandardBo bo, Long excludeId) {
        LambdaQueryWrapper<QcStandard> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(QcStandard::getProductId, bo.getProductId());
        wrapper.eq(QcStandard::getQcItem, bo.getQcItem());
        wrapper.eq(QcStandard::getQcCategory, bo.getQcCategory());
        wrapper.ne(excludeId != null, QcStandard::getId, excludeId);
        if (baseMapper.exists(wrapper)) {
            throw new ServiceException("该产品下已存在同一质控项目/类别的标准："
                + bo.getQcItem() + " / " + bo.getQcCategory());
        }
    }

    /**
     * 删除前的引用校验。
     * <p>
     * TODO(业务确认)：qc_standard 与 data_retention_policy 表尚未建，等表落地后在此接入
     * 引用查询；当前只做占位，不要以为已经保护住了数据完整性。
     */
    private void assertNotReferenced(Long id) {
        // 接入示例：
        // if (qcStandardMapper.exists(Wrappers.<QcStandard>lambdaQuery().eq(QcStandard::getProductId, id))) {
        //     throw new ServiceException("该产品已被质控标准引用，禁止删除：id=" + id);
        // }
    }
}
