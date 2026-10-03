package org.dromara.report.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.report.domain.bo.InterpretationQueryBo;
import org.dromara.report.domain.vo.AnalysisReportVo;
import org.dromara.report.domain.vo.InterpretationRowVo;

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
}
