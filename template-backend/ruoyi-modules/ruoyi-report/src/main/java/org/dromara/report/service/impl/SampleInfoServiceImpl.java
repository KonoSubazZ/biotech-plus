package org.dromara.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.report.domain.SampleInfo;
import org.dromara.report.domain.bo.SampleInfoBo;
import org.dromara.report.domain.vo.SampleInfoExcelRow;
import org.dromara.report.domain.vo.SampleInfoImportResultVo;
import org.dromara.report.domain.vo.SampleInfoVo;
import org.dromara.report.mapper.SampleInfoMapper;
import org.dromara.report.service.ISampleInfoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 样本信息 业务层处理
 * <p>
 * 样本编号（barcode，源表列 BARCODE）是业务键：新增要查重、导入按它 upsert、
 * 删除时要挡住在质控记录里被引用过的样本（qc_record.subbarcode 指向它）。
 * <p>
 * 风格要点（对齐 template-backend/docs/backend-code-standard.md）：
 * 查询条件集中在 buildQueryWrapper 里「声明 → 基础条件 → 分支条件 → 排序」展开，
 * 不在方法体内堆链式调用；缺数据抛明确异常，不返回裸 null。
 *
 * @author <你的名字>
 */
@RequiredArgsConstructor
@Service
public class SampleInfoServiceImpl implements ISampleInfoService {

    private final SampleInfoMapper baseMapper;

    @Override
    public TableDataInfo<SampleInfoVo> selectPageList(SampleInfoBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SampleInfo> wrapper = buildQueryWrapper(bo);
        Page<SampleInfoVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public SampleInfoVo queryById(Long id) {
        SampleInfoVo vo = baseMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("样本信息不存在：id=" + id);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(SampleInfoBo bo) {
        assertBarcodeUsable(bo.getBarcode(), null);
        SampleInfo entity = MapstructUtils.convert(bo, SampleInfo.class);
        return baseMapper.insert(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(SampleInfoBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("修改样本信息必须提供主键");
        }
        // 先确认记录存在，避免把「改不到」当成「改成功」
        queryById(bo.getId());
        assertBarcodeUsable(bo.getBarcode(), bo.getId());
        SampleInfo entity = MapstructUtils.convert(bo, SampleInfo.class);
        return baseMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要删除的样本信息");
        }
        for (Long id : ids) {
            assertNotReferenced(id);
        }
        baseMapper.deleteByIds(ids);
        return Boolean.TRUE;
    }

    /**
     * 上传 Excel 批量导入。
     * <p>
     * 这里**故意不加事务**：导入是「宽容」的，一行出错不能连累其它行；
     * 若放在一个事务里，被 catch 住的异常仍会把事务标记成 rollback-only，最后整体提交失败。
     * 所以每行走自己的自动提交，失败原因逐行收集返回。
     */
    @Override
    public SampleInfoImportResultVo importExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择要导入的 Excel 文件");
        }
        List<SampleInfoExcelRow> rows = readRows(file);
        if (rows.isEmpty()) {
            throw new ServiceException("Excel 里没有数据行（第 1 行是表头，请从第 2 行开始填）");
        }

        SampleInfoImportResultVo result = new SampleInfoImportResultVo();
        result.setTotal(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            // Excel 第 1 行是表头，所以数据行号从 2 开始
            importOneRow(rows.get(i), i + 2, result);
        }
        result.setFailed(result.getErrors().size());
        return result;
    }

    /** 读 Excel（FastExcel 会按 SampleInfoExcelRow 的列序号映射并跳过表头行） */
    private List<SampleInfoExcelRow> readRows(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            return ExcelUtil.importExcel(is, SampleInfoExcelRow.class);
        } catch (IOException e) {
            throw new ServiceException("Excel 读取失败：" + e.getMessage());
        } catch (RuntimeException e) {
            throw new ServiceException("Excel 解析失败：" + e.getMessage());
        }
    }

    /** 处理一行：单行出错只记原因，不中断整个导入 */
    private void importOneRow(SampleInfoExcelRow row, int excelRowNo, SampleInfoImportResultVo result) {
        String barcode = StringUtils.trim(row.getBarcode());
        if (StringUtils.isBlank(barcode)) {
            result.addError("第 " + excelRowNo + " 行：样本编号（BARCODE 列）为空，已跳过");
            return;
        }
        try {
            if (saveOrUpdateByBarcode(row, barcode)) {
                result.setInserted(result.getInserted() + 1);
            } else {
                result.setUpdated(result.getUpdated() + 1);
            }
        } catch (Exception e) {
            result.addError("第 " + excelRowNo + " 行（" + barcode + "）：" + e.getMessage());
        }
    }

    /**
     * 按样本编号「有则更新、无则新增」，返回 true 表示新增。
     * <p>
     * 122 个字段由 mapstruct 一次性搬（SampleInfoExcelRow 标了 @AutoMapper(target = SampleInfo.class)），
     * 不手写 setter；只把业务键 trim 一下。
     * <p>
     * 软删除的样本仍占着唯一键 uk_sample_file_barcode（唯一键不含 del_flag，见 04-db-schema §4.6），
     * 所以「曾经删过又再导入」要把旧行恢复再更新，不能直接 insert（否则 Duplicate entry）。
     * MyBatis-Plus 的 {@code @TableLogic} 会给普通查询自动补 del_flag='0'，查不到已删行，
     * 因此「含已删行」的取 id 走手写 SQL。
     */
    private boolean saveOrUpdateByBarcode(SampleInfoExcelRow row, String barcode) {
        SampleInfo entity = MapstructUtils.convert(row, SampleInfo.class);
        entity.setBarcode(barcode);

        SampleInfo existing = baseMapper.selectOne(
            Wrappers.<SampleInfo>lambdaQuery().eq(SampleInfo::getBarcode, barcode));
        if (existing != null) {
            entity.setId(existing.getId());
            baseMapper.updateById(entity);
            return false;
        }

        Long deletedId = baseMapper.selectIdByBarcodeIncludeDeleted(barcode);
        if (deletedId != null) {
            baseMapper.restoreById(deletedId);
            entity.setId(deletedId);
            baseMapper.updateById(entity);
            return false;
        }

        baseMapper.insert(entity);
        return true;
    }

    /**
     * 组装查询条件：声明 Wrapper → 基础条件 → 客户（两列合一）→ 日期范围 → 排序。
     * <p>
     * 搜索栏 6 项 → 列：样本编号→barcode、姓名→patient_name、客户→customer_name 或 custom_desc、
     * 录单癌种→cancer_type、录单产品→erp_test_name、日期→enter_date（委托日期）。
     * <p>
     * 客户为什么查两列：源表里 CUSTOMERNAME 与 CUSTOMEDESC（客户名称）在不同数据里各写一半
     * （生产数据里公司名常落在 customer_name 为空的 CUSTOMEDESC 上），只查一列会搜不到。
     * <p>
     * 日期用字符串比较：enter_date 在源表里既有 {@code yyyy-MM-dd} 也有 {@code yyyy-MM-dd HH:mm:ss}，
     * 起止补成 {@code >= begin} 且 {@code <= end + " 23:59:59"}，两种格式都能正确落在区间内。
     */
    private LambdaQueryWrapper<SampleInfo> buildQueryWrapper(SampleInfoBo bo) {
        LambdaQueryWrapper<SampleInfo> wrapper = Wrappers.lambdaQuery();

        wrapper.like(StringUtils.isNotBlank(bo.getBarcode()), SampleInfo::getBarcode, bo.getBarcode());
        wrapper.like(StringUtils.isNotBlank(bo.getPatientName()), SampleInfo::getPatientName, bo.getPatientName());
        wrapper.like(StringUtils.isNotBlank(bo.getCancerType()), SampleInfo::getCancerType, bo.getCancerType());
        wrapper.like(StringUtils.isNotBlank(bo.getErpTestName()), SampleInfo::getErpTestName, bo.getErpTestName());

        String customerKeyword = bo.getCustomerName();
        if (StringUtils.isNotBlank(customerKeyword)) {
            wrapper.and(nested -> nested.like(SampleInfo::getCustomerName, customerKeyword)
                .or().like(SampleInfo::getCustomDesc, customerKeyword));
        }

        Map<String, Object> params = bo.getParams();
        Object beginTime = params == null ? null : params.get("beginTime");
        Object endTime = params == null ? null : params.get("endTime");
        if (beginTime != null && endTime != null) {
            wrapper.ge(SampleInfo::getEnterDate, beginTime);
            wrapper.le(SampleInfo::getEnterDate, endTime + " 23:59:59");
        }

        wrapper.orderByDesc(SampleInfo::getId);
        return wrapper;
    }

    /**
     * 样本编号查重（对应表上的 uk_sample_file_barcode，租户条件由拦截器自动加）。
     * <p>
     * 普通查询加了 del_flag='0'，所以「编号被已删样本占着」要单独查一次给出可读提示，
     * 否则会直接抛数据库的唯一键冲突。
     */
    private void assertBarcodeUsable(String barcode, Long excludeId) {
        LambdaQueryWrapper<SampleInfo> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SampleInfo::getBarcode, barcode);
        wrapper.ne(excludeId != null, SampleInfo::getId, excludeId);
        if (baseMapper.exists(wrapper)) {
            throw new ServiceException("样本编号已存在：" + barcode);
        }
        if (excludeId == null && baseMapper.selectIdByBarcodeIncludeDeleted(barcode) != null) {
            throw new ServiceException("样本编号曾被删除（仍占用唯一键），导入同编号会自动恢复，请换一个编号：" + barcode);
        }
    }

    /**
     * 删除前的引用校验：样本编号被质控记录引用时禁止删除 ——
     * 质控记录（qc_record.subbarcode）与质控自动判定都挂在这个编号上，删了会留下悬空引用。
     */
    private void assertNotReferenced(Long id) {
        SampleInfoVo sample = queryById(id);
        Long usedByQcRecord = baseMapper.countQcRecordBySubbarcode(sample.getBarcode());
        if (usedByQcRecord != null && usedByQcRecord > 0) {
            throw new ServiceException("该样本已被 " + usedByQcRecord + " 条质控记录引用，禁止删除", 409);
        }
    }
}
