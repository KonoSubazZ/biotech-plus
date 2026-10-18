package org.dromara.qc.service.impl;

import org.dromara.audit.service.impl.AuditRecorder;
import org.dromara.audit.domain.dto.AuditTarget;
import org.dromara.audit.domain.dto.AuditSource;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.TenantContext;
import org.dromara.qc.domain.BioinfoQc;
import org.dromara.qc.domain.QcStandard;
import org.dromara.qc.domain.bo.BioinfoQcBo;
import org.dromara.qc.domain.vo.BioinfoQcReceiveResultVo;
import org.dromara.qc.domain.vo.BioinfoQcTenantVo;
import org.dromara.qc.domain.vo.BioinfoQcVo;
import org.dromara.qc.mapper.BioinfoQcMapper;
import org.dromara.qc.mapper.QcStandardMapper;
import org.dromara.qc.service.IBioinfoQcService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 生信质控 业务层处理
 * <p>
 * 数据来源只有一个：上游通过 {@code POST /qc/bioinfo/receive} 推送（见 {@link #receive}）。
 * 页面只读，没有新增/修改/删除接口。
 *
 * @author <你的名字>
 */
@RequiredArgsConstructor
@Service
public class BioinfoQcServiceImpl implements IBioinfoQcService {

    private final AuditRecorder auditRecorder;

    /** 质控状态：全部判定项达标 */
    private static final String STATUS_PASSED = "passed";
    /** 质控状态：至少一项越界 */
    private static final String STATUS_FAILED = "failed";
    /** 质控状态：没有可用阈值、或本行没有可比数值 */
    private static final String STATUS_PENDING = "pending";
    /** 判定所读的质控标准类别 */
    private static final String QC_CATEGORY_BIOINFO = "bioinfo";
    /** 只读启用中的标准 */
    private static final String STANDARD_ACTIVE = "active";

    private final BioinfoQcMapper baseMapper;
    private final QcStandardMapper qcStandardMapper;

    @Override
    public TableDataInfo<BioinfoQcVo> selectPageList(BioinfoQcBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<BioinfoQc> wrapper = buildQueryWrapper(bo);
        Page<BioinfoQcVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public BioinfoQcVo queryById(Long id) {
        BioinfoQcVo vo = baseMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("生信质控记录不存在：id=" + id);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(BioinfoQcBo bo) {
        return auditRecorder.execute("质控记录修改",
            List.of(AuditTarget.rows(AuditSource.BIO_QC, "id", bo.getId())),
            () -> updateByBoPersisted(bo));
    }

    private Boolean updateByBoPersisted(BioinfoQcBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("修改生信质控记录必须提供主键");
        }
        BioinfoQc existing = baseMapper.selectById(bo.getId());
        if (existing == null) {
            throw new ServiceException("生信质控记录不存在：id=" + bo.getId());
        }
        // 只覆盖请求里带了的字段（忽略 null），再按合并后的值重新判定状态。
        // 样本编号 / 上游记录 id 是身份字段，不参与修改（接口按 subbarcode upsert，改了就对不上上游）
        BioinfoQc submitted = MapstructUtils.convert(bo, BioinfoQc.class);
        submitted.setSubbarcode(null);
        submitted.setSourceId(null);
        BeanUtil.copyProperties(submitted, existing, CopyOptions.create().setIgnoreNullValue(true));
        existing.setStatus(judgeStatus(existing));
        return baseMapper.updateById(existing) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(List<Long> ids) {
        return auditRecorder.execute("质控记录删除",
            List.of(AuditTarget.ids(AuditSource.BIO_QC, "id", ids)),
            () -> deleteByIdsPersisted(ids));
    }

    private Boolean deleteByIdsPersisted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的生信质控记录");
        }
        baseMapper.deleteByIds(ids);
        return Boolean.TRUE;
    }

    @Override
    public BioinfoQcTenantVo resolveTenant(String subbarcode) {
        BioinfoQcTenantVo result = new BioinfoQcTenantVo();
        result.setSubbarcode(subbarcode);
        result.setFound(false);
        if (StringUtils.isBlank(subbarcode)) {
            return result;
        }
        // 静默查询：样本可能属于别的药企，带租户条件会查不到（withoutTenant 关掉拦截器的租户过滤）
        String customerName = TenantContext.withoutTenant(() -> baseMapper.selectCustomerNameBySubbarcode(subbarcode));
        result.setCustomerName(customerName);
        if (StringUtils.isBlank(customerName)) {
            return result;
        }
        String tenantId = TenantContext.withoutTenant(() -> baseMapper.selectTenantIdByCustomer(customerName));
        result.setTenantId(tenantId);
        result.setFound(StringUtils.isNotBlank(tenantId));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BioinfoQcReceiveResultVo receive(List<BioinfoQcBo> items) {
        BioinfoQcReceiveResultVo summary = new BioinfoQcReceiveResultVo();
        summary.setReceived(items.size());

        int inserted = 0;
        int updated = 0;
        List<BioinfoQcReceiveResultVo.SkippedRow> skippedRows = new ArrayList<>();

        for (BioinfoQcBo item : items) {
            String subbarcode = StringUtils.trim(item.getSubbarcode());
            if (StringUtils.isBlank(subbarcode)) {
                skippedRows.add(new BioinfoQcReceiveResultVo.SkippedRow(null, "subbarcode 为空"));
                continue;
            }
            BioinfoQcTenantVo tenant = resolveTenant(subbarcode);
            if (!Boolean.TRUE.equals(tenant.getFound())) {
                skippedRows.add(new BioinfoQcReceiveResultVo.SkippedRow(subbarcode, tenantSkipReason(tenant)));
                continue;
            }
            // 归属药企由服务端解析，调用方指定不了：withTenant 后租户拦截器会按该药企读写
            boolean isInsert = TenantContext.withTenant(tenant.getTenantId(), () -> saveOne(item, subbarcode));
            if (isInsert) {
                inserted++;
            } else {
                updated++;
            }
        }

        summary.setInserted(inserted);
        summary.setUpdated(updated);
        summary.setSkipped(skippedRows);
        return summary;
    }

    /**
     * 解析不到药企时给出可直接定位问题的原因
     */
    private String tenantSkipReason(BioinfoQcTenantVo tenant) {
        if (StringUtils.isBlank(tenant.getCustomerName())) {
            return "sample_file 里查不到该样本编号（或其 customer_name 为空）";
        }
        return "客户名 " + tenant.getCustomerName() + " 匹配不到启用中的药企（sys_tenant）";
    }

    /**
     * 在药企作用域内落库：按样本编号有则更新、无则新增；两种情况下 status 都由服务端判定后写入
     *
     * @return true = 新增，false = 覆盖更新
     */
    private boolean saveOne(BioinfoQcBo item, String subbarcode) {
        return auditRecorder.execute("生信质控导入",
            List.of(AuditTarget.rows(AuditSource.BIO_QC, "subbarcode", subbarcode)),
            () -> saveOnePersisted(item, subbarcode));
    }

    private boolean saveOnePersisted(BioinfoQcBo item, String subbarcode) {
        BioinfoQc entity = MapstructUtils.convert(item, BioinfoQc.class);
        entity.setSubbarcode(subbarcode);
        entity.setStatus(judgeStatus(entity));

        BioinfoQc existing = selectBySubbarcode(subbarcode);
        if (existing == null) {
            baseMapper.insert(entity);
            return true;
        }
        entity.setId(existing.getId());
        baseMapper.updateById(entity);
        return false;
    }

    /**
     * 阈值判定：读 qc_standard 里 qc_category='bioinfo' 且 status='active' 的项，
     * 按「标准项的质控参数 qc_param（英文原始字段名）== 表列名」取本行同名列的值，逐项比 min / max（NULL 表示该侧不设限）。
     * <p>
     * 任一项越界 → failed；全部达标 → passed；一条可比项都没有 → pending。
     * 阈值在「质控标准」页面维护，改完对后续推送生效（历史行不回溯重算）。
     */
    private String judgeStatus(BioinfoQc entity) {
        List<QcStandard> standards = loadActiveStandards();
        if (CollUtil.isEmpty(standards)) {
            return STATUS_PENDING;
        }

        Map<String, Object> metrics = BeanUtil.beanToMap(entity, false, true);
        boolean judged = false;
        boolean allPass = true;
        for (QcStandard standard : standards) {
            BigDecimal value = Convert.toBigDecimal(metrics.get(StrUtil.toCamelCase(standard.getQcParam())), null);
            Boolean outOfRange = outOfRange(value, standard);
            if (outOfRange == null) {
                // 本行该列没值 / 标准没设上下限 → 这项不参与判定
                continue;
            }
            judged = true;
            if (outOfRange) {
                allPass = false;
            }
        }

        if (!judged) {
            return STATUS_PENDING;
        }
        return allPass ? STATUS_PASSED : STATUS_FAILED;
    }

    /**
     * 不达标项的描述（给通知单的「不合格原因」用），如
     * {@code raw_base 1.30（要求 ≥2）；probe_capture_ratio 66.41（要求 ≥70）}
     * <p>
     * 描述里用 ≥ / ≤ 而不是 &lt; / &gt;：渲染器开了 autoescape，尖括号会被转义成实体字符印在文档里。
     *
     * @param subbarcode 样本编号
     * @return 不达标项（用「；」分隔）；没有记录 / 没有标准 / 全部达标都返回空串
     */
    @Override
    public String failureDescription(String subbarcode) {
        if (StringUtils.isBlank(subbarcode)) {
            return "";
        }
        BioinfoQc entity = selectBySubbarcode(subbarcode);
        List<QcStandard> standards = loadActiveStandards();
        if (entity == null || CollUtil.isEmpty(standards)) {
            return "";
        }
        Map<String, Object> metrics = BeanUtil.beanToMap(entity, false, true);
        List<String> failures = new ArrayList<>();
        for (QcStandard standard : standards) {
            BigDecimal value = Convert.toBigDecimal(metrics.get(StrUtil.toCamelCase(standard.getQcParam())), null);
            Boolean outOfRange = outOfRange(value, standard);
            if (outOfRange == null || !outOfRange) {
                continue;
            }
            failures.add(standard.getQcItem() + " " + value.toPlainString() + "（要求 " + limitText(standard) + "）");
        }
        return String.join("；", failures);
    }

    /**
     * 该项是否越界；返回 null = 不可比（本行该列没值 / 标准没设上下限），调用方跳过这项
     */
    private Boolean outOfRange(BigDecimal value, QcStandard standard) {
        BigDecimal min = Convert.toBigDecimal(standard.getMinValue(), null);
        BigDecimal max = Convert.toBigDecimal(standard.getMaxValue(), null);
        Boolean result = null;
        if (value != null && (min != null || max != null)) {
            result = (min != null && value.compareTo(min) < 0) || (max != null && value.compareTo(max) > 0);
        }
        return result;
    }

    /** 阈值文案：≥2 / ≤99 / 2~99 */
    private String limitText(QcStandard standard) {
        String min = StringUtils.trimToEmpty(standard.getMinValue());
        String max = StringUtils.trimToEmpty(standard.getMaxValue());
        if (!min.isEmpty() && !max.isEmpty()) {
            return min + "~" + max;
        }
        if (!min.isEmpty()) {
            return "≥" + min;
        }
        return "≤" + max;
    }

    /**
     * 判定用的阈值标准：先取数据所属药企自己配的；该药企没配 → 回落到平台租户（000000）的配置。
     * <p>
     * 现在标准都配在平台租户（质控标准页面默认在 000000 下维护），各药企按需覆写即可。
     * 两处都没有 → 调用方按 pending 处理。
     */
    private List<QcStandard> loadActiveStandards() {
        List<QcStandard> own = selectActiveStandards();
        if (CollUtil.isNotEmpty(own)) {
            return own;
        }
        if (TenantContext.DEFAULT_TENANT_ID.equals(TenantContext.getTenantId())) {
            return own;
        }
        return TenantContext.withTenant(TenantContext.DEFAULT_TENANT_ID, this::selectActiveStandards);
    }

    /** 当前租户里启用中的生信标准 */
    private List<QcStandard> selectActiveStandards() {
        return qcStandardMapper.selectList(Wrappers.<QcStandard>lambdaQuery()
            .eq(QcStandard::getQcCategory, QC_CATEGORY_BIOINFO)
            .eq(QcStandard::getStatus, STANDARD_ACTIVE));
    }

    /**
     * 按样本编号取本行（租户条件由拦截器自动附上，唯一键是 tenant_id + subbarcode）
     */
    private BioinfoQc selectBySubbarcode(String subbarcode) {
        return baseMapper.selectOne(Wrappers.<BioinfoQc>lambdaQuery()
            .eq(BioinfoQc::getSubbarcode, subbarcode)
            .last("LIMIT 1"));
    }

    /**
     * 组装查询条件：声明 Wrapper → 基础条件 → 分支条件 → 排序
     */
    private LambdaQueryWrapper<BioinfoQc> buildQueryWrapper(BioinfoQcBo bo) {
        LambdaQueryWrapper<BioinfoQc> wrapper = Wrappers.lambdaQuery();

        wrapper.like(StringUtils.isNotBlank(bo.getSubbarcode()), BioinfoQc::getSubbarcode, bo.getSubbarcode());
        wrapper.like(StringUtils.isNotBlank(bo.getLibId()), BioinfoQc::getLibId, bo.getLibId());

        if (StringUtils.isNotBlank(bo.getStatus())) {
            wrapper.eq(BioinfoQc::getStatus, bo.getStatus());
        }

        wrapper.orderByDesc(BioinfoQc::getId);
        return wrapper;
    }
}
