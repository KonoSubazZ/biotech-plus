package org.dromara.report.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.report.domain.CmsSample;
import org.dromara.report.domain.vo.CmsSampleVo;

/**
 * 样本信息 数据层（cms_sample）
 * <p>
 * 单表增删改查、简单条件组合、排序一律用继承来的 BaseMapperPlus 能力。
 * 下面两个方法覆盖手写能力覆盖不到的场景：查「含已软删除行」的 id、恢复软删除行。
 *
 * @author <你的名字>
 */
public interface CmsSampleMapper extends BaseMapperPlus<CmsSample, CmsSampleVo> {

    /**
     * 按诺禾编号（subbarcode）查 id，**包含已软删除的行**（导入时判断「曾经删过又再导入」用）。
     * <p>
     * 唯一键 uk_cms_sample_subbarcode 不含 del_flag，已删行仍占键位；
     * 而 {@code @TableLogic} 会给普通查询自动补 del_flag='0'，查不到已删行。
     *
     * @param subbarcode 诺禾编号
     * @return 命中的 id；没有则返回 null
     */
    Long selectIdBySubbarcodeIncludeDeleted(@Param("subbarcode") String subbarcode);

    /**
     * 把软删除的行恢复成正常行（导入命中已删行的情况）。
     *
     * @param id 主键
     * @return 影响行数
     */
    int restoreById(@Param("id") Long id);
}
