package org.dromara.report.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.report.domain.bo.InterpretationFileQueryBo;
import org.dromara.report.domain.bo.InterpretationQueryBo;
import org.dromara.report.domain.bo.InterpretationVariantQueryBo;
import org.dromara.report.domain.vo.AnalysisSnapshotVo;
import org.dromara.report.domain.vo.InterpretationFileContentVo;
import org.dromara.report.domain.vo.InterpretationFileVo;
import org.dromara.report.domain.vo.InterpretationRowVo;

import java.util.Map;

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

    /**
     * 分页查询某个分析批次的集群文件（data_file_status）；不返回 file_text 大字段。
     *
     * @param page       分页参数
     * @param analysisId 分析数据ID
     * @param query      查询条件（文件名 / 文件类型 / 状态）
     * @return 分页结果
     */
    Page<InterpretationFileVo> selectFilePage(@Param("page") Page<InterpretationFileVo> page,
                                              @Param("analysisId") Long analysisId,
                                              @Param("query") InterpretationFileQueryBo query);

    /**
     * 取单个文件的文本内容（data_file_status.file_text），超长按 maxLength 截断。
     *
     * @param fileId    文件ID
     * @param analysisId 分析数据ID（校验文件确实属于该批次）
     * @param maxLength 最大返回字符数
     * @return 文件内容；文件不存在或不属于该批次时返回 null
     */
    InterpretationFileContentVo selectFileContent(@Param("fileId") Long fileId,
                                                 @Param("analysisId") Long analysisId,
                                                 @Param("maxLength") int maxLength);

    /**
     * 分页查询某分析批次的位点（按 sourceType 决定查哪张明细表，SQL 见 XML 的 choose 分支）。
     *
     * @param page       分页参数
     * @param analysisId 分析数据ID
     * @param query      查询条件（sourceType 必填 / gene / isReported）
     * @return 分页结果
     */
    Page<Map<String, Object>> selectVariantPage(@Param("page") Page<Map<String, Object>> page,
                                               @Param("analysisId") Long analysisId,
                                               @Param("query") InterpretationVariantQueryBo query);

    /**
     * 切换位点「入报告」状态；UPDATE 带 analysisId 归属校验，返回 0 表示位点不存在或不属于该批次。
     *
     * @param sourceId          位点主键
     * @param sourceType        位点类型
     * @param analysisId        分析数据ID
     * @param isReported        目标状态 0/1
     * @param filteredRationale 过滤理由（可空）
     * @param operatorId        操作人（reviewed_by / update_by）
     * @return 受影响行数
     */
    int updateVariantReportStatus(@Param("sourceId") Long sourceId,
                                 @Param("sourceType") String sourceType,
                                 @Param("analysisId") Long analysisId,
                                 @Param("isReported") Integer isReported,
                                 @Param("filteredRationale") String filteredRationale,
                                 @Param("operatorId") Long operatorId);
}
