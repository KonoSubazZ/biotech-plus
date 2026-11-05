package org.dromara.report.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.report.domain.bo.CmsSampleBo;
import org.dromara.report.domain.vo.CmsSampleImportResultVo;
import org.dromara.report.domain.vo.CmsSampleVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 药企样本 业务层
 * <p>
 * 业务唯一键 = (CMS 订单主键, CMS 样本主键)：新增要查重、导入按它「有则更新、无则新增」。
 *
 * @author <你的名字>
 */
public interface ICmsSampleService {

    /**
     * 分页查询药企样本列表
     *
     * @param bo        查询条件（含 params.beginTime / params.endTime 的送检日期范围）
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    TableDataInfo<CmsSampleVo> selectPageList(CmsSampleBo bo, PageQuery pageQuery);

    /**
     * 按主键查询详情
     *
     * @param id 主键
     * @return 详情；不存在时抛 ServiceException
     */
    CmsSampleVo queryById(Long id);

    /**
     * 新增
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean insertByBo(CmsSampleBo bo);

    /**
     * 修改
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean updateByBo(CmsSampleBo bo);

    /**
     * 批量删除
     *
     * @param ids 主键集合
     * @return 是否成功
     */
    Boolean deleteByIds(List<Long> ids);

    /**
     * 上传 Excel 批量导入药企样本
     *
     * @param file Excel 文件
     * @return 导入结果（新增 / 更新 / 失败 + 每行失败原因）
     */
    CmsSampleImportResultVo importExcel(MultipartFile file);
}
