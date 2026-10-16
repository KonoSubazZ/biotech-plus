package org.dromara.report.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.report.domain.bo.SampleFlowQueryBo;
import org.dromara.report.domain.vo.SampleFlowRowVo;

import java.util.Map;

/**
 * 样本流转 Mapper（跨 analysis_data / sample_file / analysis_report / sys_user，SQL 见
 * resources/mapper/report/SampleFlowMapper.xml）
 *
 * @author <你的名字>
 */
public interface SampleFlowMapper {

    /**
     * 分页查询样本流转列表：analysis_data ⟕ sample_file ⟕ 该批次最新一份 analysis_report。
     * <p>
     * 页签状态过滤与「只看自己的 / 只看别人的」可见性过滤都在 XML 里按 tab 分支，
     * 当前用户ID由调用方传入（不用 XML 里调 Sa-Token，SQL 保持纯查询）。
     *
     * @param page          分页参数（由 MyBatis-Plus 分页插件接管）
     * @param query         查询条件（tab / 分析日期 / 样本编号 / 产品 / 显示全部）
     * @param currentUserId 当前登录用户ID（可见性过滤用）
     * @return 分页结果
     */
    Page<SampleFlowRowVo> selectFlowPage(@Param("page") Page<SampleFlowRowVo> page,
                                         @Param("query") SampleFlowQueryBo query,
                                         @Param("currentUserId") Long currentUserId);

    /**
     * 单个页签的条数（与列表共用同一份页签条件，见 XML 的 tabCondition）。
     *
     * @param query         页签查询条件（只用 tab 与 showAll 两个字段）
     * @param currentUserId 当前登录用户（可见性判断）
     * @return 条数
     */
    int countByTab(@Param("query") SampleFlowQueryBo query, @Param("currentUserId") Long currentUserId);

    /**
     * 取该批次最新一份报告的关键字段（处理动作判断用：有没有报告、什么状态、谁产出的、谁认领的）。
     *
     * @param analysisId 分析数据ID
     * @return {reportId, reportStatus, reportGeneratedBy, reportInterpretedBy, reportReviewClaimedBy}；无报告返回 null
     */
    Map<String, Object> selectLatestReport(@Param("analysisId") Long analysisId);

    /**
     * 报告认领（30 报告待解读 → 31 报告解读中）；带上来源状态条件，被他人抢先认领时影响行数为 0。
     *
     * @param reportId 报告ID
     * @param userId   认领人（写 report_interpreted_by）
     * @return 影响行数
     */
    int claimInterpret(@Param("reportId") Long reportId, @Param("userId") Long userId);

    /**
     * 报告复核认领（40 报告待审核 → 41 报告审核中）；同样只允许「待审核」且不是自己产出的报告。
     *
     * @param reportId 报告ID
     * @param userId   认领人（写 report_review_claimed_by）
     * @return 影响行数
     */
    int claimReview(@Param("reportId") Long reportId, @Param("userId") Long userId);
}
