package org.dromara.report.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.report.domain.ReportTemplate;
import org.dromara.report.domain.vo.ReportTemplateVo;

import java.util.List;
import java.util.Map;

/**
 * 报告模板 数据层
 * <p>
 * 单表增删改查用继承来的 BaseMapperPlus 能力；下面都是手写能力覆盖不到的场景，
 * 实现写在 resources/mapper/report/ReportTemplateMapper.xml（租户条件由租户拦截器自动附加）。
 * <p>
 * 「模板 ↔ 产品」关系表 product_template 不做实体，直接用一条条手写 SQL 维护：
 * tinyint(1) 列经 JDBC 会变成布尔，绕开实体映射更省心。
 *
 * @author <你的名字>
 */
public interface ReportTemplateMapper extends BaseMapperPlus<ReportTemplate, ReportTemplateVo> {

    /**
     * 按模板编码查 id，**包含已软删除的行**。
     * <p>
     * 唯一键 uk_report_template_code 不含 del_flag，软删过的行仍占着编码 →
     * 「删了再加同编码」要把旧行恢复，不能直接 insert（否则 Duplicate entry）。
     * MyBatis-Plus 的 {@code @TableLogic} 会给普通查询自动补 del_flag='0'，查不到已删行。
     *
     * @param templateCode 模板编码
     * @return 命中的 template_id；没有则返回 null
     */
    Long selectIdByCodeIncludeDeleted(@Param("templateCode") String templateCode);

    /**
     * 把软删除的模板恢复成正常行。
     *
     * @param templateId 主键
     * @return 影响行数
     */
    int restoreById(@Param("templateId") Long templateId);

    /**
     * 统计引用了该模板的报告条数（删除保护用）。
     *
     * @param templateId 模板主键
     * @return 引用条数
     */
    Long countAnalysisReportByTemplateId(@Param("templateId") Long templateId);

    /**
     * 查模板关联的产品ID（顺序按 sort_order）。
     *
     * @param templateId 模板主键
     * @return 产品ID集合
     */
    List<Long> selectProductIds(@Param("templateId") Long templateId);

    /**
     * 批量查模板关联的产品（列表页回显「关联产品」与编辑抽屉回填用）。
     *
     * @param templateIds 模板主键集合（非空）
     * @return 每行含 templateId / productId / productName
     */
    List<Map<String, Object>> selectProductRelationsByTemplateIds(@Param("templateIds") List<Long> templateIds);

    /**
     * 按模板编码取启用中的模板（预览/生成按编码解析模板用）。
     *
     * @param templateCode 模板编码
     * @return 模板；不存在或已停用返回 null
     */
    /**
     * 取某产品在 product_template 里配置的模板（默认优先，其次 sort_order）。
     *
     * @param productId 产品ID（product_config.id）
     * @return 模板；没有配置返回 null
     */
    ReportTemplateVo selectDefaultByProductId(@Param("productId") Long productId);

    /**
     * 按产品名/编码定位 product_config.id（scanner 只写产品名，product_id 可能为空）。
     *
     * @param name 产品名或产品编码
     * @return 产品ID；定位不到返回 null
     */
    Long selectProductIdByName(@Param("name") String name);

    /**
     * 产品下拉选项（product_config；跨模块只读，不走 Maven 依赖）。
     *
     * @return 每行含 id / name / code / status
     */
    List<Map<String, Object>> selectProductOptions();

    /**
     * 把一份模板下的全部产品关联置为失效（del_flag='1'）。
     * <p>
     * 同步关联时先置失效、再按传入集合逐条「恢复或插入」，避免唯一键冲突
     * （uk_product_template 不含 del_flag）。
     *
     * @param templateId 模板主键
     * @return 影响行数
     */
    int invalidateProductTemplates(@Param("templateId") Long templateId);

    /**
     * 恢复/激活一条产品关联（含被软删的行）。
     *
     * @param templateId 模板主键
     * @param productId  产品主键
     * @param sortOrder  展示顺序
     * @return 影响行数；0 表示该关联从未存在过，需要插入
     */
    int activateProductTemplate(@Param("templateId") Long templateId,
                                @Param("productId") Long productId,
                                @Param("sortOrder") Integer sortOrder);

    /**
     * 新增一条产品关联。
     *
     * @param templateId 模板主键
     * @param productId  产品主键
     * @param sortOrder  展示顺序
     * @return 影响行数
     */
    int insertProductTemplate(@Param("templateId") Long templateId,
                              @Param("productId") Long productId,
                              @Param("sortOrder") Integer sortOrder);
}
