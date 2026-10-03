package org.dromara.report.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.report.domain.bo.InterpretationQueryBo;
import org.dromara.report.domain.vo.AnalysisSnapshotVo;
import org.dromara.report.domain.vo.InterpretationRowVo;

/**
 * 报告解读列表 / 批次快照 Mapper（跨表查询，SQL 见 resources/mapper/report/InterpretationMapper.xml）
 *
 * @author <你的名字>
 */
public interface InterpretationMapper {

    /**
     * 分页查询「报告解读」列表：analysis_data ⟕ 该批次最新一份 analysis_report。
     *
     * @param page       分页参数（由 MyBatis-Plus 分页插件接管）
     * @param query      查询条件（样本编号 / 产品 / 报告状态）
     * @param beginTime  分析日期起（yyyyMMdd，可空）
     * @param endTime    分析日期止（yyyyMMdd，可空）
     * @return 分页结果
     */
    Page<InterpretationRowVo> selectInterpretationPage(@Param("page") Page<InterpretationRowVo> page,
                                                      @Param("query") InterpretationQueryBo query,
                                                      @Param("beginTime") String beginTime,
                                                      @Param("endTime") String endTime);

    /**
     * 取一个分析批次的样本/产品快照，「进入解读」建报告记录时用。
     *
     * @param analysisId 分析数据ID
     * @return 快照；批次不存在时返回 null
     */
    AnalysisSnapshotVo selectAnalysisSnapshot(@Param("analysisId") Long analysisId);
}
