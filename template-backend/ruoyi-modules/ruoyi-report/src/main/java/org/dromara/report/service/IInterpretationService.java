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
import org.dromara.report.domain.vo.FileRedriveVo;
import org.dromara.report.domain.vo.InterpretationRowVo;

import java.util.Map;

/**
 * 报告解读 业务接口
 *
 * @author <你的名字>
 */
public interface IInterpretationService {

    /** 仅供已校验整批重扫结果的重新解读服务调用。 */
    AnalysisReportVo enterAfterRescan(Long analysisId, Long sourceReportId);

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
     * 生信看点完成（筛选位点页签的按钮）：20 生信看点 → 21 生信审核，记看点人与时间。
     * <p>
     * 口径：看点人写 report_bioinfo_viewed_by，之后该样本从「生信看点」页签消失（页签只显示
     * 没人看点的或自己看点的），出现在「生信审核」页签；看点人不能审自己看点的报告。
     *
     * @param analysisId 分析数据ID
     * @param reportId   报告ID
     * @return 流转后的报告状态（21 生信审核）
     */
    Integer finishBioinfoView(Long analysisId, Long reportId);

    /**
     * 生信复核通过（筛选位点页签的按钮）：21 生信审核 → 30 报告待解读。
     * <p>
     * 口径：复核人不能是看点人本人（看点人 ≠ 复核人）；流转到 30 后样本出现在「报告待解读」页签，
     * 该页签全部可见，由解读人认领后转 31「报告解读中」。
     *
     * @param analysisId 分析数据ID
     * @param reportId   报告ID
     * @return 流转后的报告状态（30 报告待解读）
     */
    Integer finishBioinfoReview(Long analysisId, Long reportId);

    /**
     * 重新驱动一个文件（集群对接页签的文件行按钮）：调外部 scanner.py 驱动单个文件，
     * 脚本自己写回文件级/样本级状态，本方法再把最新状态读回来给页面。
     *
     * @param analysisId 分析数据ID
     * @param fileId     文件ID
     * @return 文件最新状态 + 批次级驱动状态 + 脚本说明
     */
    FileRedriveVo redriveFile(Long analysisId, Long fileId);

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
     * <p>
     * 返回 Map（key = 数据库列名）：明细表列多（SNP/Indel 38 列、CR_ALL 38 列），
     * 按需求「除公共字段外全展示」，用 Map 免得每加一列都要改 Java。
     *
     * @param analysisId 分析数据ID
     * @param bo         查询条件（sourceType 必填 / gene / isReported）
     * @param pageQuery  分页参数
     * @return 分页列表（每行一个 Map）
     */
    TableDataInfo<Map<String, Object>> selectVariantList(Long analysisId, InterpretationVariantQueryBo bo,
                                                         PageQuery pageQuery);

    /**
     * 切换位点「入报告」状态。
     *
     * @param bo 入参（analysisId / sourceType / sourceId / isReported / filteredRationale）
     */
    void updateVariantReportStatus(InterpretationVariantStatusBo bo);
}
