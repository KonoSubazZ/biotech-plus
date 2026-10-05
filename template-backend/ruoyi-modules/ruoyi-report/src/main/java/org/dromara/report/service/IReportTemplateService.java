package org.dromara.report.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.report.domain.bo.ReportTemplateBo;
import org.dromara.report.domain.vo.ReportTemplateVo;

import java.util.List;
import java.util.Map;

/**
 * 报告模板 业务层
 * <p>
 * 模板编码（template_code）是业务键：新增要查重、软删过的编码要能恢复；
 * 「模板 ↔ 产品」多对多关系由本服务统一维护（product_template）；
 * 删除时要挡住被 analysis_report 引用过的模板。
 *
 * @author <你的名字>
 */
public interface IReportTemplateService {

    /**
     * 分页查询报告模板列表（附带关联产品名称）。
     *
     * @param bo        查询条件（模板编码 / 名称 / 报告类型 / 状态）
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    TableDataInfo<ReportTemplateVo> selectPageList(ReportTemplateBo bo, PageQuery pageQuery);

    /**
     * 按主键查询详情（附带关联产品ID）。
     *
     * @param templateId 主键
     * @return 详情；不存在时抛 ServiceException
     */
    ReportTemplateVo queryById(Long templateId);

    /**
     * 新增模板，并同步产品关联。
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean insertByBo(ReportTemplateBo bo);

    /**
     * 修改模板，并同步产品关联（未在列表里的关联会被解除）。
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean updateByBo(ReportTemplateBo bo);

    /**
     * 批量删除（被报告引用时抛 ServiceException）。
     *
     * @param ids 主键集合
     * @return 是否成功
     */
    Boolean deleteByIds(List<Long> ids);

    /**
     * 产品下拉选项（供模板表单的「关联产品」多选使用）。
     *
     * @return 每行含 id / name / code / status
     */
    List<Map<String, Object>> selectProductOptions();
}
