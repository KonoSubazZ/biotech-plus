package org.dromara.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.report.domain.AnalysisReport;
import org.dromara.report.domain.bo.InterpretationQueryBo;
import org.dromara.report.domain.vo.AnalysisReportVo;
import org.dromara.report.domain.vo.AnalysisSnapshotVo;
import org.dromara.report.domain.vo.InterpretationRowVo;
import org.dromara.report.mapper.AnalysisReportMapper;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.service.IInterpretationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 报告解读 业务层处理
 * <p>
 * 本页只承担「列表 + 进入解读」两件事：
 * <ul>
 *   <li>列表 = analysis_data ⟕ 该批次最新一份 analysis_report（一个批次可有多份报告，列表只显示最新那份的状态）。</li>
 *   <li>进入解读 = 复用该批次仍在 INTERPRETING 的报告；没有才新建，且样本/产品信息从 analysis_data 复制，
 *       保证报告行自身可读（设计书要求报告记录带 analysis_date / subbarcode / product 快照）。</li>
 * </ul>
 * 位点筛选、预览、审核、发送属于后续页面的能力，不在这里实现。
 *
 * @author <你的名字>
 */
@RequiredArgsConstructor
@Service
public class InterpretationServiceImpl implements IInterpretationService {

    /** 报告状态：解读中 */
    private static final String STATUS_INTERPRETING = "INTERPRETING";

    private final InterpretationMapper interpretationMapper;
    private final AnalysisReportMapper analysisReportMapper;

    @Override
    public TableDataInfo<InterpretationRowVo> selectPageList(InterpretationQueryBo bo, PageQuery pageQuery) {
        String beginTime = paramAsString(bo, "beginTime");
        String endTime = paramAsString(bo, "endTime");
        Page<InterpretationRowVo> page =
            interpretationMapper.selectInterpretationPage(pageQuery.build(), bo, beginTime, endTime);
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnalysisReportVo enter(Long analysisId) {
        if (analysisId == null) {
            throw new ServiceException("分析数据ID不能为空");
        }
        AnalysisSnapshotVo snapshot = interpretationMapper.selectAnalysisSnapshot(analysisId);
        if (snapshot == null) {
            throw new ServiceException("分析数据不存在或已删除：analysisId=" + analysisId);
        }

        // 同一个批次已有「解读中」的报告就复用它，避免点一次多出一条草稿
        AnalysisReport existing = selectInterpretingReport(analysisId);
        if (existing != null) {
            return analysisReportMapper.selectVoById(existing.getReportId());
        }

        AnalysisReport report = new AnalysisReport();
        report.setAnalysisId(snapshot.getAnalysisId());
        report.setAnalysisDate(snapshot.getAnalysisDate());
        report.setSubbarcode(snapshot.getSubbarcode());
        report.setProduct(snapshot.getProduct());
        report.setProductId(snapshot.getProductId());
        report.setStatus(STATUS_INTERPRETING);
        analysisReportMapper.insert(report);

        AnalysisReportVo vo = analysisReportMapper.selectVoById(report.getReportId());
        if (vo == null) {
            throw new ServiceException("报告记录创建失败：analysisId=" + analysisId);
        }
        return vo;
    }

    /**
     * 取该批次最近一条「解读中」的报告。
     * <p>
     * 只认 INTERPRETING：已提交审核 / 已驳回 / 已发送的报告不允许被「进入解读」复用，
     * 否则会绕过状态机把已流转的报告退回草稿态。
     */
    private AnalysisReport selectInterpretingReport(Long analysisId) {
        LambdaQueryWrapper<AnalysisReport> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(AnalysisReport::getAnalysisId, analysisId);
        wrapper.eq(AnalysisReport::getStatus, STATUS_INTERPRETING);
        wrapper.orderByDesc(AnalysisReport::getReportId);
        wrapper.last("LIMIT 1");
        return analysisReportMapper.selectOne(wrapper);
    }

    /**
     * 取 params 里的字符串型查询参数（日期范围走 params.beginTime / params.endTime）。
     * <p>
     * analysis_date 是 varchar(8) 的 YYYYMMDD：前端 NDatePicker 的 yyyy-MM-dd 已在搜索组件里
     * 转成 yyyyMMdd，这里只做「空值当没传」的清洗，避免 like/between 用空串误匹配。
     */
    private String paramAsString(InterpretationQueryBo bo, String key) {
        Map<String, Object> params = bo.getParams();
        Object value = params == null ? null : params.get(key);
        String text = value == null ? null : String.valueOf(value);
        return StringUtils.isBlank(text) ? null : text;
    }
}
