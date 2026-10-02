package org.dromara.project.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.project.domain.bo.ProductConfigBo;
import org.dromara.project.domain.vo.ProductConfigVo;

import java.util.List;

/**
 * 产品配置 业务层
 * <p>
 * 接口先合并（骨架），实现由各 owner 分别完成 —— 见 ai-rules/03-collaboration.md §2。
 *
 * @author <你的名字>
 */
public interface IProductConfigService {

    /**
     * 分页查询产品配置列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    TableDataInfo<ProductConfigVo> selectPageList(ProductConfigBo bo, PageQuery pageQuery);

    /**
     * 查询全部启用中的产品
     *
     * @return 产品列表
     */
    List<ProductConfigVo> selectActiveList();

    /**
     * 按主键查询详情
     *
     * @param id 主键
     * @return 详情；不存在时抛 ServiceException
     */
    ProductConfigVo queryById(Long id);

    /**
     * 新增
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean insertByBo(ProductConfigBo bo);

    /**
     * 修改
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean updateByBo(ProductConfigBo bo);

    /**
     * 批量删除
     *
     * @param ids 主键集合
     * @return 是否成功
     */
    Boolean deleteByIds(List<Long> ids);
}
