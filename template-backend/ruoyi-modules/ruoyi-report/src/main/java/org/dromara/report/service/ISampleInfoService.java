package org.dromara.report.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.report.domain.bo.SampleInfoBo;
import org.dromara.report.domain.vo.SampleInfoImportResultVo;
import org.dromara.report.domain.vo.SampleInfoVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 样本信息 业务层
 * <p>
 * 样本编号（subbarcode）是业务键：新增要查重、导入按它「有则更新、无则新增」，
 * 删除时要挡住在质控记录里被引用过的样本。
 *
 * @author <你的名字>
 */
public interface ISampleInfoService {

    /**
     * 分页查询样本信息列表
     *
     * @param bo        查询条件（含 params.beginTime / params.endTime 的委托日期范围）
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    TableDataInfo<SampleInfoVo> selectPageList(SampleInfoBo bo, PageQuery pageQuery);

    /**
     * 按主键查询详情
     *
     * @param id 主键
     * @return 详情；不存在时抛 ServiceException
     */
    SampleInfoVo queryById(Long id);

    /**
     * 新增
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean insertByBo(SampleInfoBo bo);

    /**
     * 修改
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean updateByBo(SampleInfoBo bo);

    /**
     * 批量删除（被质控记录引用时抛 ServiceException）
     *
     * @param ids 主键集合
     * @return 是否成功
     */
    Boolean deleteByIds(List<Long> ids);

    /**
     * 上传 Excel 批量导入样本信息
     *
     * @param file Excel 文件
     * @return 导入结果（新增 / 更新 / 失败 + 每行失败原因）
     */
    SampleInfoImportResultVo importExcel(MultipartFile file);
}
