package org.dromara.qc.service.impl;

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
import org.dromara.qc.domain.QcStandard;
import org.dromara.qc.domain.WetLabQc;
import org.dromara.qc.domain.bo.WetLabQcBo;
import org.dromara.qc.domain.vo.WetLabQcReceiveResultVo;
import org.dromara.qc.domain.vo.WetLabQcVo;
import org.dromara.qc.mapper.QcStandardMapper;
import org.dromara.qc.mapper.WetLabQcMapper;
import org.dromara.qc.service.IWetLabQcService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 湿实验质控 业务层处理
 * <p>
 * 表是**宽表：一行一个样本，每个质控项一列**（列名 = qc_standard.qc_param），形态对齐生信质控。
 * 数据来源只有一个：上游通过 {@code POST /qc/wetLabQc/receive} 推送（见 {@link #receive}），
 * 落库租户取样本自身的 sample_file.tenant_id；阈值留在 qc_standard，落库时按 qc_param 取来判定
 * {@code status}（passed 合格 / failed 不合格 / pending 待判定）。人工修改（{@link #updateByBo}）后同样重判。
 *
 * @author <你的名字>
 */
@RequiredArgsConstructor
@Service
public class WetLabQcServiceImpl implements IWetLabQcService {

    /** 质控状态：全部判定项达标 */
    private static final String STATUS_PASSED = "passed";
    /** 质控状态：至少一项越界 */
    private static final String STATUS_FAILED = "failed";
    /** 质控状态：没有可用阈值、或本行没有可比数值（含等级类质控项） */
    private static final String STATUS_PENDING = "pending";
    /** 判定所读的质控标准类别 */
    private static final String QC_CATEGORY_WET_LAB = "wet_lab";
    /** 只读启用中的标准 */
    private static final String STANDARD_ACTIVE = "active";

    private final WetLabQcMapper baseMapper;
    private final QcStandardMapper qcStandardMapper;

    @Override
    public TableDataInfo<WetLabQcVo> selectPageList(WetLabQcBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WetLabQc> wrapper = buildQueryWrapper(bo);
        Page<WetLabQcVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public WetLabQcVo queryById(Long id) {
        WetLabQcVo vo = baseMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("湿实验质控记录不存在：id=" + id);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(WetLabQcBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("修改湿实验质控记录必须提供主键");
        }
        WetLabQc existing = baseMapper.selectById(bo.getId());
        if (existing == null) {
            throw new ServiceException("湿实验质控记录不存在：id=" + bo.getId());
        }
        // 只覆盖请求里带了的字段（忽略 null），再按合并后的值重新判定状态。
        // 样本编号是接口 upsert 的业务键，不参与修改（改了就对不上上游）
        WetLabQc submitted = MapstructUtils.convert(bo, WetLabQc.class);
        submitted.setSubbarcode(null);
        BeanUtil.copyProperties(submitted, existing, CopyOptions.create().setIgnoreNullValue(true));
        existing.setStatus(judgeStatus(existing));
        return baseMapper.updateById(existing) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要删除的湿实验质控记录");
        }
        baseMapper.deleteByIds(ids);
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WetLabQcReceiveResultVo receive(List<WetLabQcBo> items) {
        WetLabQcReceiveResultVo summary = new WetLabQcReceiveResultVo();
        summary.setReceived(items.size());

        int inserted = 0;
        int updated = 0;
        List<WetLabQcReceiveResultVo.SkippedRow> skippedRows = new ArrayList<>();

        for (WetLabQcBo item : items) {
            String subbarcode = StringUtils.trim(item.getSubbarcode());
            if (StringUtils.isBlank(subbarcode)) {
                skippedRows.add(new WetLabQcReceiveResultVo.SkippedRow(null, "subbarcode 为空"));
                continue;
            }
            // 归属药企：取样本自身的 sample_file.tenant_id。这个查询必须静默 —— 此刻还没有该药企的
            // 租户作用域，样本也可能属于别的药企；解析不出来就按行拒收，不静默丢数据、也不给默认租户。
            String tenantId = TenantContext.withoutTenant(
                () -> baseMapper.selectTenantIdBySubbarcode(subbarcode));
            if (StringUtils.isBlank(tenantId)) {
                skippedRows.add(new WetLabQcReceiveResultVo.SkippedRow(subbarcode,
                    "sample_file 里查不到该样本编号（或其 tenant_id 为空）"));
                continue;
            }
            // 归属药企由服务端解析，调用方指定不了：withTenant 后租户拦截器会按该药企读写
            SaveOutcome outcome = TenantContext.withTenant(tenantId, () -> saveOne(item, subbarcode));
            if (outcome.reason() != null) {
                skippedRows.add(new WetLabQcReceiveResultVo.SkippedRow(subbarcode, outcome.reason()));
                continue;
            }
            if (outcome.inserted()) {
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

    @Override
    public String failureDescription(String subbarcode) {
        if (StringUtils.isBlank(subbarcode)) {
            return "";
        }
        WetLabQc entity = selectBySubbarcode(subbarcode);
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
     * 单行落库：按「样本编号」有则合并更新、无则新增（唯一键 uk_wet_lab_qc_subbarcode）。
     * <p>
     * 调用方负责把它放在样本所属药企的租户作用域里，写进去的行就落在该药企名下。
     * 合并口径：updateStrategy = NOT_NULL，请求里没给值的列保留库里原值（只写给了值的列）；
     * status 一律由服务端按合并后的值重判。
     *
     * @param item       推送行
     * @param subbarcode 样本编号（已去空格）
     * @return 落库结果（新增/覆盖）
     */
    private SaveOutcome saveOne(WetLabQcBo item, String subbarcode) {
        WetLabQc row = MapstructUtils.convert(item, WetLabQc.class);
        row.setSubbarcode(subbarcode);
        row.setId(null);

        WetLabQc existing = baseMapper.selectRowIncludingDeleted(subbarcode);
        if (existing != null && !"0".equals(existing.getDelFlag())) {
            // 唯一键不含 del_flag：被删过的行还占着这个键，先物理清掉再插（新行的审计字段才是干净的）
            baseMapper.purgeById(existing.getId());
            existing = null;
        }
        if (existing == null) {
            row.setStatus(judgeStatus(row));
            baseMapper.insert(row);
            return new SaveOutcome(true, null);
        }
        // 覆盖：只写请求里带了的列，其余列保留库里原值 —— 判定要按「合并后」的整行算
        existing.setStatus(null);
        BeanUtil.copyProperties(row, existing, CopyOptions.create().setIgnoreNullValue(true));
        existing.setStatus(judgeStatus(existing));
        baseMapper.updateById(existing);
        return new SaveOutcome(false, null);
    }

    /**
     * 阈值判定：读 qc_standard 里 qc_category='wet_lab' 且 status='active' 的项，
     * 按「标准项的质控参数 qc_param（英文原始字段名）== 表列名」取本行同名列的值，逐项比 min / max（NULL 表示该侧不设限）。
     * <p>
     * 任一项越界 → failed；全部达标 → passed；一条可比项都没有 → pending。
     * 阈值在「质控标准」页面维护，改完对后续推送/修改生效（历史行不回溯重算）。
     * 等级类质控项（如 dna_degradation 的 A/B/C/D）阈值不是数值 → 自动按「不可比」跳过，不参与判定。
     */
    private String judgeStatus(WetLabQc entity) {
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
                // 本行该列没值 / 标准没设上下限 / 阈值不是数值（等级项）→ 这项不参与判定
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
     * 该项是否越界；返回 null = 不可比（本行该列没值 / 标准没设上下限 / 阈值不是数值），调用方跳过这项
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

    /** 当前租户里启用中的湿实验标准 */
    private List<QcStandard> selectActiveStandards() {
        return qcStandardMapper.selectList(Wrappers.<QcStandard>lambdaQuery()
            .eq(QcStandard::getQcCategory, QC_CATEGORY_WET_LAB)
            .eq(QcStandard::getStatus, STANDARD_ACTIVE));
    }

    /** 按样本编号取本行（租户条件由拦截器自动附上，唯一键是 tenant_id + subbarcode） */
    private WetLabQc selectBySubbarcode(String subbarcode) {
        return baseMapper.selectOne(Wrappers.<WetLabQc>lambdaQuery()
            .eq(WetLabQc::getSubbarcode, subbarcode)
            .last("LIMIT 1"));
    }

    /**
     * 组装查询条件：声明 Wrapper → 基础条件 → 分支条件 → 排序
     */
    private LambdaQueryWrapper<WetLabQc> buildQueryWrapper(WetLabQcBo bo) {
        LambdaQueryWrapper<WetLabQc> wrapper = Wrappers.lambdaQuery();

        wrapper.like(StringUtils.isNotBlank(bo.getSubbarcode()), WetLabQc::getSubbarcode, bo.getSubbarcode());

        if (StringUtils.isNotBlank(bo.getStatus())) {
            wrapper.eq(WetLabQc::getStatus, bo.getStatus());
        }

        wrapper.orderByDesc(WetLabQc::getId);
        return wrapper;
    }

    /**
     * 单行落库结果：reason 非空表示这行被拒收；inserted 区分新增 / 覆盖更新。
     * 当前只有「样本查不到」会被拒收，先保留这个形状便于以后加规则。
     */
    private record SaveOutcome(boolean inserted, String reason) {
    }
}
