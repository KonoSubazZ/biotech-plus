package org.dromara.report.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.report.domain.bo.InterpretationFileQueryBo;
import org.dromara.report.domain.bo.InterpretationQueryBo;
import org.dromara.report.domain.bo.InterpretationVariantQueryBo;
import org.dromara.report.domain.bo.InterpretationVariantStatusBo;
import org.dromara.report.domain.vo.AnalysisReportVo;
import org.dromara.report.domain.vo.InterpretationContextVo;
import org.dromara.report.domain.vo.InterpretationFileContentVo;
import org.dromara.report.domain.vo.InterpretationFileVo;
import org.dromara.report.domain.vo.InterpretationRowVo;
import org.dromara.report.domain.vo.InterpretationVariantVo;

/**
 * 报告解读 业务接口
 *
 * @author <你的名字>
 */
public interface IInterpretationService {

    /**
     * 分页查询报告解读列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    TableDataInfo<InterpretationRowVo> selectPageList(InterpretationQueryBo bo, PageQuery pageQuery);

    /**
     * 进入解读：该批次已有「解读中」报告则复用，否则按分析批次快照新建一条。
     *
     * @param analysisId 分析数据ID
     * @return 报告头信息（含 reportId / status）
     */
    AnalysisReportVo enter(Long analysisId);

    /**
     * 加载解读页上下文：报告头 + LIMS 信息 + 生成前校验结论。
     *
     * @param reportId   报告ID
     * @param analysisId 分析数据ID（必须与报告归属一致）
     * @return 上下文
     */
    InterpretationContextVo loadContext(Long reportId, Long analysisId);

    /**
     * 分页查询集群对接文件（Tab②）
     *
     * @param analysisId 分析数据ID
     * @param bo         查询条件（文件名 / 文件类型 / 状态）
     * @param pageQuery  分页参数
     * @return 分页列表
     */
    TableDataInfo<InterpretationFileVo> selectFileList(Long analysisId, InterpretationFileQueryBo bo,
                                                      PageQuery pageQuery);

    /**
     * 查看单个文件内容（Tab②）
     *
     * @param fileId     文件ID
     * @param analysisId 分析数据ID（校验归属）
     * @return 文件内容
     */
    InterpretationFileContentVo queryFileContent(Long fileId, Long analysisId);

    /**
     * 分页查询位点（Tab③ 筛选位点）：按 sourceType 查对应明细表，只返回该批次的位点。
     *
     * @param analysisId 分析数据ID
     * @param bo         查询条件（sourceType 必填 / gene / isReported）
     * @param pageQuery  分页参数
     * @return 分页列表
     */
    TableDataInfo<InterpretationVariantVo> selectVariantList(Long analysisId, InterpretationVariantQueryBo bo,
                                                             PageQuery pageQuery);

    /**
     * 切换位点「入报告」状态。
     *
     * @param bo 入参（analysisId / sourceType / sourceId / isReported / filteredRationale）
     */
    void updateVariantReportStatus(InterpretationVariantStatusBo bo);
}
