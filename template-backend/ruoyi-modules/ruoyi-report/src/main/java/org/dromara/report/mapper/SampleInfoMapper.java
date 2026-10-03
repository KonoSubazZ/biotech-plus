package org.dromara.report.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.report.domain.SampleInfo;
import org.dromara.report.domain.vo.SampleInfoVo;

/**
 * 样本信息 数据层
 * <p>
 * 单表增删改查、简单条件组合、排序、计数一律用继承来的 BaseMapperPlus 能力，
 * 不写自定义 SQL。下面三个方法都是手写能力覆盖不到的场景，实现写在
 * resources/mapper/report/SampleInfoMapper.xml（租户条件由租户拦截器自动附加）。
 *
 * @author <你的名字>
 */
public interface SampleInfoMapper extends BaseMapperPlus<SampleInfo, SampleInfoVo> {

    /**
     * 按样本编号（barcode）查 id，**包含已软删除的行**（导入时判断「曾经删过又再导入」用）。
     * <p>
     * 不能用 MyBatis-Plus 的查询：{@code @TableLogic} 会自动补 {@code del_flag='0'}，
     * 已删行查不到，而唯一键 uk_sample_file_barcode 不含 del_flag，删掉的行仍占着键位。
     *
     * @param barcode 样本编号（源表列 BARCODE）
     * @return 命中的 id；没有则返回 null
     */
    Long selectIdByBarcodeIncludeDeleted(@Param("barcode") String barcode);

    /**
     * 把软删除的行恢复成正常行（导入时命中已删行的情况）。
     *
     * @param id 主键
     * @return 影响行数
     */
    int restoreById(@Param("id") Long id);

    /**
     * 统计引用了该样本编号的质控记录条数（删除保护用）。
     * <p>
     * 跨模块只读查询：ruoyi-report 不依赖 ruoyi-qc，所以走一条只读 SQL，
     * 与 ruoyi-project 的 countQcStandardByProductId 同一口径。
     * 实验室侧的 qc_record 把同一个样本编号叫 subbarcode，值是 sample_file.barcode。
     *
     * @param subbarcode 样本编号
     * @return 引用条数
     */
    Long countQcRecordBySubbarcode(@Param("subbarcode") String subbarcode);
}
