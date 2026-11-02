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
import org.dromara.report.domain.CmsSample;
import org.dromara.report.domain.bo.CmsSampleBo;
import org.dromara.report.domain.vo.CmsSampleExcelRow;
import org.dromara.report.domain.vo.CmsSampleImportResultVo;
import org.dromara.report.domain.vo.CmsSampleVo;
import org.dromara.report.mapper.CmsSampleMapper;
import org.dromara.report.service.ICmsSampleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 样本信息 业务层处理（cms_sample）
 * <p>
 * 诺禾编号（subbarcode）是业务唯一键，对应 uk_cms_sample_subbarcode：
 * 新增要查重、导入按它「有则更新、无则新增」。
 *
 * @author <你的名字>
 */
@RequiredArgsConstructor
@Service
public class CmsSampleServiceImpl implements ICmsSampleService {

    private final CmsSampleMapper baseMapper;

    @Override
    public TableDataInfo<CmsSampleVo> selectPageList(CmsSampleBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CmsSample> wrapper = buildQueryWrapper(bo);
        Page<CmsSampleVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public CmsSampleVo queryById(Long id) {
        CmsSampleVo vo = baseMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("样本信息不存在：id=" + id);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(CmsSampleBo bo) {
        assertSubbarcodeUsable(bo.getSubbarcode(), null);
        CmsSample entity = MapstructUtils.convert(bo, CmsSample.class);
        return baseMapper.insert(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(CmsSampleBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("修改样本信息必须提供主键");
        }
        // 先确认记录存在，避免把「改不到」当成「改成功」
        queryById(bo.getId());
        assertSubbarcodeUsable(bo.getSubbarcode(), bo.getId());
        CmsSample entity = MapstructUtils.convert(bo, CmsSample.class);
        return baseMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要删除的样本信息");
        }
        baseMapper.deleteByIds(ids);
        return Boolean.TRUE;
    }

    /**
     * 上传 Excel 批量导入。
     * <p>
     * 故意不加整体事务：一行出错不能连累其它行；每行走自己的自动提交，失败原因逐行收集返回。
     */
    @Override
    public CmsSampleImportResultVo importExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择要导入的 Excel 文件");
        }
        List<CmsSampleExcelRow> rows = readRows(file);
        if (rows.isEmpty()) {
            throw new ServiceException("Excel 里没有数据行（第 1 行是表头，请从第 2 行开始填）");
        }

        CmsSampleImportResultVo result = new CmsSampleImportResultVo();
        result.setTotal(rows.size());

        // 同一订单只能对应一个产品、一个样本名称（双样本组织 = 组织 + 血液对照，两行同产品同名称）
        Map<Integer, String> orderErrors = findOrderConsistencyErrors(rows);

        for (int i = 0; i < rows.size(); i++) {
            // Excel 第 1 行是表头，所以数据行号从 2 开始
            String orderError = orderErrors.get(i);
            if (orderError != null) {
                result.addError("第 " + (i + 2) + " 行：" + orderError);
                continue;
            }
            importOneRow(rows.get(i), i + 2, result);
        }
        result.setFailed(result.getErrors().size());
        return result;
    }

    /**
     * 校验同一订单内的产品与样本名称是否一致。
     * <p>
     * 业务口径：一个订单的样本属于同一个产品；双样本组织（组织 + 血液对照）两行
     * 除样本类型相关字段外，产品编码/产品名称/样本名称必须相同。
     * 返回「数据行下标 -> 错误原因」，导入时整单跳过并逐行报错，避免脏数据落库。
     */
    private Map<Integer, String> findOrderConsistencyErrors(List<CmsSampleExcelRow> rows) {
        Map<String, List<Integer>> rowsByOrder = new LinkedHashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            String orderNo = StringUtils.trim(rows.get(i).getOrderNo());
            if (StringUtils.isNotBlank(orderNo)) {
                rowsByOrder.computeIfAbsent(orderNo, key -> new ArrayList<>()).add(i);
            }
        }

        Map<Integer, String> errors = new HashMap<>();
        rowsByOrder.forEach((orderNo, indexes) -> {
            if (indexes.size() < 2) {
                return;
            }
            Set<String> products = new HashSet<>();
            Set<String> sampleNames = new HashSet<>();
            for (Integer index : indexes) {
                CmsSampleExcelRow row = rows.get(index);
                String productCode = StringUtils.trim(row.getProductCode());
                String productName = StringUtils.trim(row.getProductName());
                String sampleName = StringUtils.trim(row.getSampleName());
                products.add(StringUtils.isNotBlank(productCode) ? productCode : productName);
                sampleNames.add(sampleName);
            }
            if (products.size() > 1 || sampleNames.size() > 1) {
                String reason = "同一订单「" + orderNo + "」的样本产品/样本名称必须一致（产品数 "
                    + products.size() + "、样本名称数 " + sampleNames.size() + "）";
                indexes.forEach(index -> errors.put(index, reason));
            }
        });
        return errors;
    }

    /** 读 Excel（FastExcel 按 CmsSampleExcelRow 的列序号映射并跳过表头行） */
    private List<CmsSampleExcelRow> readRows(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            return ExcelUtil.importExcel(is, CmsSampleExcelRow.class);
        } catch (IOException e) {
            throw new ServiceException("Excel 读取失败：" + e.getMessage());
        } catch (RuntimeException e) {
            throw new ServiceException("Excel 解析失败：" + e.getMessage());
        }
    }

    /** 处理一行：单行出错只记原因，不中断整个导入 */
    private void importOneRow(CmsSampleExcelRow row, int excelRowNo, CmsSampleImportResultVo result) {
        String subbarcode = StringUtils.trim(row.getSubbarcode());
        if (StringUtils.isBlank(subbarcode)) {
            result.addError("第 " + excelRowNo + " 行：诺禾编号不能为空，已跳过");
            return;
        }
        try {
            if (saveOrUpdateBySubbarcode(row, subbarcode)) {
                result.setInserted(result.getInserted() + 1);
            } else {
                result.setUpdated(result.getUpdated() + 1);
            }
        } catch (Exception e) {
            result.addError("第 " + excelRowNo + " 行（" + subbarcode + "）：" + e.getMessage());
        }
    }

    /**
     * 按诺禾编号「有则更新、无则新增」，返回 true 表示新增。
     * <p>
     * 软删除的行仍占着唯一键（唯一键不含 del_flag），所以「曾经删过又再导入」要先把旧行恢复再更新；
     * MyBatis-Plus 的 {@code @TableLogic} 查不到已删行，因此取 id 走手写 SQL。
     */
    private boolean saveOrUpdateBySubbarcode(CmsSampleExcelRow row, String subbarcode) {
        CmsSample entity = MapstructUtils.convert(row, CmsSample.class);
        entity.setSubbarcode(subbarcode);

        CmsSample existing = baseMapper.selectOne(
            Wrappers.<CmsSample>lambdaQuery().eq(CmsSample::getSubbarcode, subbarcode));
        if (existing != null) {
            entity.setId(existing.getId());
            baseMapper.updateById(entity);
            return false;
        }

        Long deletedId = baseMapper.selectIdBySubbarcodeIncludeDeleted(subbarcode);
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
     * 组装查询条件：样本编号 / 订单编号 / 客户名称 / 产品名称 / 样本类型 + 送检日期范围。
     * <p>
     * 日期按字符串比较；起止都补齐时生效，上界补 23:59:59 兼容带时分秒的写法。
     */
    private LambdaQueryWrapper<CmsSample> buildQueryWrapper(CmsSampleBo bo) {
        LambdaQueryWrapper<CmsSample> wrapper = Wrappers.lambdaQuery();

        wrapper.like(StringUtils.isNotBlank(bo.getSubbarcode()), CmsSample::getSubbarcode, bo.getSubbarcode());
        wrapper.like(StringUtils.isNotBlank(bo.getOrderNo()), CmsSample::getOrderNo, bo.getOrderNo());
        wrapper.like(StringUtils.isNotBlank(bo.getCustomerName()), CmsSample::getCustomerName, bo.getCustomerName());
        wrapper.like(StringUtils.isNotBlank(bo.getProductName()), CmsSample::getProductName, bo.getProductName());
        wrapper.like(StringUtils.isNotBlank(bo.getSampleType()), CmsSample::getSampleType, bo.getSampleType());

        Map<String, Object> params = bo.getParams();
        Object beginTime = params == null ? null : params.get("beginTime");
        Object endTime = params == null ? null : params.get("endTime");
        if (beginTime != null && endTime != null) {
            wrapper.ge(CmsSample::getSendDate, beginTime);
            wrapper.le(CmsSample::getSendDate, endTime + " 23:59:59");
        }

        wrapper.orderByDesc(CmsSample::getId);
        return wrapper;
    }

    /**
     * 诺禾编号查重（对应 uk_cms_sample_subbarcode，租户条件由拦截器自动加）。
     * <p>
     * 普通查询加了 del_flag='0'，所以「编号被已删样本占着」要单独查一次给出可读提示，
     * 否则会直接抛数据库的唯一键冲突。
     */
    private void assertSubbarcodeUsable(String subbarcode, Long excludeId) {
        LambdaQueryWrapper<CmsSample> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(CmsSample::getSubbarcode, subbarcode);
        wrapper.ne(excludeId != null, CmsSample::getId, excludeId);
        if (baseMapper.exists(wrapper)) {
            throw new ServiceException("诺禾编号已存在：" + subbarcode);
        }
        if (excludeId == null && baseMapper.selectIdBySubbarcodeIncludeDeleted(subbarcode) != null) {
            throw new ServiceException("诺禾编号曾被删除（仍占用唯一键），导入同编号会自动恢复，请换一个编号：" + subbarcode);
        }
    }
}
