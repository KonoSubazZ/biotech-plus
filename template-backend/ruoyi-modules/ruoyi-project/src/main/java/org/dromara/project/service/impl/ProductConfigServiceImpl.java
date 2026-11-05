package org.dromara.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.project.domain.ProductConfig;
import org.dromara.project.domain.bo.ProductConfigBo;
import org.dromara.project.domain.vo.ProductConfigVo;
import org.dromara.project.mapper.ProductConfigMapper;
import org.dromara.project.service.IProductConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 产品配置 业务层处理
 * <p>
 * 风格要点（对齐 template-backend/docs/backend-code-standard.md）：
 * 查询条件集中在 buildQueryWrapper 里「声明 → 基础条件 → 分支条件 → 排序」展开，
 * 不在方法体内堆链式调用；缺数据抛明确异常，不返回裸 null。
 *
 * @author <你的名字>
 */
@RequiredArgsConstructor
@Service
public class ProductConfigServiceImpl implements IProductConfigService {

    private final ProductConfigMapper baseMapper;

    @Override
    public TableDataInfo<ProductConfigVo> selectPageList(ProductConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ProductConfig> wrapper = buildQueryWrapper(bo);
        Page<ProductConfigVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<ProductConfigVo> selectActiveList() {
        LambdaQueryWrapper<ProductConfig> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ProductConfig::getStatus, "active");
        wrapper.orderByAsc(ProductConfig::getId);
        return baseMapper.selectVoList(wrapper);
    }

    @Override
    public ProductConfigVo queryById(Long id) {
        ProductConfigVo vo = baseMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("产品配置不存在：id=" + id);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(ProductConfigBo bo) {
        validateProductCodeUnique(bo.getProductCode(), null);
        ProductConfig entity = MapstructUtils.convert(bo, ProductConfig.class);
        return baseMapper.insert(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(ProductConfigBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("修改产品配置必须提供主键");
        }
        // 先确认记录存在，避免把「改不到」当成「改成功」
        queryById(bo.getId());
        validateProductCodeUnique(bo.getProductCode(), bo.getId());
        ProductConfig entity = MapstructUtils.convert(bo, ProductConfig.class);
        return baseMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要删除的产品配置");
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
    private LambdaQueryWrapper<ProductConfig> buildQueryWrapper(ProductConfigBo bo) {
        LambdaQueryWrapper<ProductConfig> wrapper = Wrappers.lambdaQuery();

        wrapper.like(StringUtils.isNotBlank(bo.getProductName()), ProductConfig::getProductName, bo.getProductName());
        wrapper.like(StringUtils.isNotBlank(bo.getProductBio()), ProductConfig::getProductBio, bo.getProductBio());
        wrapper.like(StringUtils.isNotBlank(bo.getProductCode()), ProductConfig::getProductCode, bo.getProductCode());
        wrapper.eq(StringUtils.isNotBlank(bo.getTestType()), ProductConfig::getTestType, bo.getTestType());
        wrapper.eq(StringUtils.isNotBlank(bo.getPanelType()), ProductConfig::getPanelType, bo.getPanelType());

        if (StringUtils.isNotBlank(bo.getStatus())) {
            wrapper.eq(ProductConfig::getStatus, bo.getStatus());
        }

        wrapper.orderByAsc(ProductConfig::getId);
        return wrapper;
    }

    /**
     * 校验产品编码唯一，<b>含逻辑删除行</b>。排除自身，避免"改其它字段"时误判冲突。
     * <p>
     * 产品编码是业务唯一标识、不复用：唯一键 uk_product_config_product_code 不含 del_flag，
     * 软删的产品仍占着编码，所以这里必须连软删行一起查（普通 wrapper 会被 @TableLogic 过滤掉它们），
     * 命中直接抛 409 提示，而不是让 insert 撞唯一键报 500。
     */
    private void validateProductCodeUnique(String productCode, Long excludeId) {
        if (StringUtils.isBlank(productCode)) {
            return;
        }
        Long used = baseMapper.countByProductCodeIncludeDeleted(productCode, excludeId);
        if (used != null && used > 0) {
            throw new ServiceException("产品编码已存在（含已删除的产品）：" + productCode, 409);
        }
    }

    /**
     * 删除前的引用校验：产品被质控标准引用时禁止删除（设计文档 product-config.md §3「删除保护」）。
     * <p>
     * data_retention_policy 表尚未建，等它落地后在此追加一处引用检查（设计文档要求两处都查）。
     */
    private void assertNotReferenced(Long id) {
        Long usedByQcStandard = baseMapper.countQcStandardByProductId(id);
        if (usedByQcStandard != null && usedByQcStandard > 0) {
            throw new ServiceException("该产品已被 " + usedByQcStandard + " 条质控标准引用，禁止删除", 409);
        }
    }
}
