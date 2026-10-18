package org.dromara.report.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.report.domain.bo.SampleFlowHandleBo;
import org.dromara.report.domain.bo.SampleFlowQueryBo;
import org.dromara.report.domain.vo.SampleFlowHandleVo;
import org.dromara.report.domain.vo.SampleFlowRowVo;
import org.dromara.report.domain.vo.SampleFlowTabCountVo;
import org.dromara.report.mapper.SampleFlowMapper;
import org.dromara.report.service.IInterpretationService;
import org.dromara.report.service.IReportSignService;
import org.dromara.report.service.ISampleFlowService;
import org.dromara.report.utils.ReportStatus;
import org.springframework.stereotype.Service;
import org.dromara.report.service.impl.revision.ReportOperationLock;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 样本流转 业务层处理。
 * <p>
 * 9 个页签分两类：
 * <ul>
 *   <li><b>跳转类</b>（驱动异常 / 生信看点 / 生信审核 / 报告解读中 / 报告审核中 / 报告发送）：处理 = 打开报告解读的
 *       对应页签继续干活，其中「生信看点」第一次进来要先给该批次建/复用报告记录（初始状态 20 生信看点）。</li>
 *   <li><b>认领类</b>（报告待解读 30→31、报告待审核 40→41）：处理 = 只更新状态，数据随后自动出现在下一个页签。</li>
 * </ul>
 * 页签权限与页签一一对应（report:flow:driver / :bioinfo-view / …）——「有页签权限 = 页签内按钮与接口都能用」，
 * 与报告解读详情页的口径一致；列表接口只查权限，处理接口在做动作前逐次校验所属页签权限。
 *
 * @author <你的名字>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SampleFlowServiceImpl implements ISampleFlowService {

    /** 跳转类页签 → 报告解读详情页的页签名（不在此表里的页签不跳转） */
    private static final Map<String, String> JUMP_TABS = Map.of(
        "driver", "files",
        "bioinfo-view", "variants",
        "bioinfo-review", "variants",
        "interpreting", "preview",
        "reviewing", "review",
        "sending", "send"
    );

    /** 页签展示顺序（与前端页签定义一致，summary 按这个顺序返回） */
    private static final List<String> TAB_ORDER = List.of("driver", "bioinfo-view", "bioinfo-review",
        "pending-interpret", "interpreting", "pending-review", "reviewing", "signing", "sending");

    /** 页签 → 页签权限点（与 sys_menu 的 report:flow:* 逐字一致） */
    private static final Map<String, String> TAB_PERMS = Map.ofEntries(
        Map.entry("driver", "report:flow:driver"),
        Map.entry("bioinfo-view", "report:flow:bioinfo-view"),
        Map.entry("bioinfo-review", "report:flow:bioinfo-review"),
        Map.entry("pending-interpret", "report:flow:pending-interpret"),
        Map.entry("interpreting", "report:flow:interpreting"),
        Map.entry("pending-review", "report:flow:pending-review"),
        Map.entry("reviewing", "report:flow:reviewing"),
        Map.entry("signing", "report:flow:signing"),
        Map.entry("sending", "report:flow:sending")
    );

    private final SampleFlowMapper sampleFlowMapper;
    private final ReportOperationLock operationLock;

    private final IInterpretationService interpretationService;

    /** 报告签名通道（未对接期是假实现） */
    private final IReportSignService reportSignService;

    @Override
    public List<SampleFlowTabCountVo> summary() {
        Long userId = LoginHelper.getUserId();
        List<SampleFlowTabCountVo> list = new ArrayList<>();
        for (String tab : TAB_ORDER) {
            // 没该页签权限的不返回（页签不渲染，数字也不要）
            if (!StpUtil.hasPermission(TAB_PERMS.get(tab))) {
                continue;
            }
            list.add(countTab(tab, userId));
        }
        return list;
    }

    /** 单个页签的两个口径条数（与列表同一份 SQL 条件） */
    private SampleFlowTabCountVo countTab(String tab, Long userId) {
        SampleFlowTabCountVo vo = new SampleFlowTabCountVo();
        vo.setTab(tab);
        SampleFlowQueryBo mine = new SampleFlowQueryBo();
        mine.setTab(tab);
        mine.setShowAll(false);
        vo.setMineCount(sampleFlowMapper.countByTab(mine, userId));
        SampleFlowQueryBo all = new SampleFlowQueryBo();
        all.setTab(tab);
        all.setShowAll(true);
        vo.setAllCount(sampleFlowMapper.countByTab(all, userId));
        return vo;
    }

    @Override
    public TableDataInfo<SampleFlowRowVo> selectPageList(SampleFlowQueryBo bo, PageQuery pageQuery) {
        String tab = bo.getTab();
        if (!TAB_PERMS.containsKey(tab)) {
            throw new ServiceException("页签不存在：" + tab);
        }
        // 口径：页签权限 = 页签内接口与按钮权限。列表也要按页签校验，
        // 否则只有页面权限（report:flow:list）的角色能读到别的环节的数据。
        StpUtil.checkPermission(TAB_PERMS.get(tab));

        Page<SampleFlowRowVo> page = sampleFlowMapper.selectFlowPage(pageQuery.build(), bo, LoginHelper.getUserId());
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SampleFlowHandleVo handle(SampleFlowHandleBo bo) {
        String tab = bo.getTab();
        if (!TAB_PERMS.containsKey(tab)) {
            throw new ServiceException("页签不存在：" + tab);
        }
        // 接口权限同页签：只有被放行该页签的角色才能做这个页签的处理动作
        StpUtil.checkPermission(TAB_PERMS.get(tab));

        Map<String, Object> report = sampleFlowMapper.selectLatestReport(bo.getAnalysisId());
        SampleFlowHandleVo vo = new SampleFlowHandleVo();
        vo.setAnalysisId(bo.getAnalysisId());
        vo.setReportId(toLong(report == null ? null : report.get("reportId")));

        if ("pending-interpret".equals(tab)) {
            return claimInterpret(report, vo);
        }
        if ("pending-review".equals(tab)) {
            return claimReview(report, vo);
        }

        String jumpTab = JUMP_TABS.get(tab);
        if (jumpTab == null) {
            // 1.8 报告签名（报告解读没有签名页签，所以不跳转，就地处理）
            return handleSigning(report, vo);
        }
        if ("bioinfo-view".equals(tab)) {
            // 生信看点：第一次「处理」就为该批次建/复用报告记录（状态 20 生信看点）
            vo.setReportId(interpretationService.enter(bo.getAnalysisId()).getReportId());
            vo.setReportStatus(ReportStatus.BIOINFO_VIEW);
        }
        if (vo.getReportId() == null) {
            if ("driver".equals(tab)) {
                // 驱动异常：还没有报告记录也要能进「集群对接」看文件、按文件重新驱动
                // （详情页支持只带 analysisId 打开，只放行集群对接页签）
                vo.setTargetTab(jumpTab);
                return vo;
            }
            vo.setMessage("该批次还没有报告记录：请先在「集群对接」按文件重新驱动，驱动成功后即可进入生信看点");
            return vo;
        }
        vo.setTargetTab(jumpTab);
        if (vo.getReportStatus() == null) {
            vo.setReportStatus(toInt(report.get("reportStatus")));
        }
        return vo;
    }

    /**
     * 1.8 报告签名页签的处理：
     * <ul>
     *   <li>50 报告签名中 → 不动作，提示等待签名回调（未对接期可用测试回调接口确认）；</li>
     *   <li>52 报告签名失败 → 重新送签（→ 50）。</li>
     * </ul>
     */
    private SampleFlowHandleVo handleSigning(Map<String, Object> report, SampleFlowHandleVo vo) {
        Long reportId = requireReport(report);
        int status = toInt(report.get("reportStatus"));
        if (status == ReportStatus.SIGNING) {
            vo.setReportStatus(ReportStatus.SIGNING);
            vo.setMessage("报告签名中：已送签，等待签名回调（未对接期可由管理员调用测试回调接口确认结果）");
            return vo;
        }
        if (status != ReportStatus.SIGN_FAILED) {
            throw new ServiceException("当前状态（" + ReportStatus.label(status) + "）不需要在签名页签处理：reportId=" + reportId);
        }
        String flowNo = reportSignService.submit(reportId);
        vo.setReportStatus(ReportStatus.SIGNING);
        vo.setMessage("已重新送签（流程号 " + flowNo + "），等待签名回调");
        return vo;
    }

    /** 报告待解读 30 → 31：认领人写 report_interpreted_by，数据随即出现在「报告解读中」页签 */
    private SampleFlowHandleVo claimInterpret(Map<String, Object> report, SampleFlowHandleVo vo) {
        Long reportId = requireReport(report);
        int status = toInt(report.get("reportStatus"));
        if (status != ReportStatus.PENDING_INTERPRET) {
            throw new ServiceException("当前状态（" + ReportStatus.label(status) + "）不允许认领：reportId=" + reportId);
        }
        int affected = sampleFlowMapper.claimInterpret(reportId, LoginHelper.getUserId());
        if (affected == 0) {
            throw new ServiceException("该报告已被他人认领，请刷新列表：reportId=" + reportId);
        }
        vo.setReportStatus(ReportStatus.INTERPRETING);
        vo.setMessage("已认领，报告流转到「报告解读中」");
        return vo;
    }

    /** 报告待审核 40 → 41：认领人写 report_review_claimed_by；自己产出的报告不能认领 */
    private SampleFlowHandleVo claimReview(Map<String, Object> report, SampleFlowHandleVo vo) {
        Long reportId = requireReport(report);
        return operationLock.call(reportId, () -> {
            Map<String, Object> latest = sampleFlowMapper.selectLatestReport(vo.getAnalysisId());
            if (!reportId.equals(requireReport(latest))) {
                throw new ServiceException("报告记录已更新，请刷新列表");
            }
            return claimReviewLocked(latest, vo);
        });
    }

    private SampleFlowHandleVo claimReviewLocked(Map<String, Object> report, SampleFlowHandleVo vo) {
        Long reportId = requireReport(report);
        int status = toInt(report.get("reportStatus"));
        if (status != ReportStatus.PENDING_REVIEW) {
            throw new ServiceException("当前状态（" + ReportStatus.label(status) + "）不允许认领：reportId=" + reportId);
        }
        Long producer = toLong(report.get("reportGeneratedBy"));
        if (producer != null && producer.equals(LoginHelper.getUserId())) {
            throw new ServiceException("不能认领自己产出的报告：reportId=" + reportId);
        }
        int affected = sampleFlowMapper.claimReview(reportId, LoginHelper.getUserId());
        if (affected == 0) {
            throw new ServiceException("该报告已被他人认领，请刷新列表：reportId=" + reportId);
        }
        vo.setReportStatus(ReportStatus.REVIEWING);
        vo.setMessage("已认领，报告流转到「报告审核中」");
        return vo;
    }

    /** 认领类动作必须有报告记录 */
    private Long requireReport(Map<String, Object> report) {
        Long reportId = report == null ? null : toLong(report.get("reportId"));
        if (reportId == null) {
            throw new ServiceException("该批次还没有报告记录，无法认领");
        }
        return reportId;
    }

    private Long toLong(Object value) {
        return value == null ? null : Long.valueOf(String.valueOf(value).trim());
    }

    private int toInt(Object value) {
        if (value == null) {
            throw new ServiceException("报告状态缺失");
        }
        return Integer.parseInt(String.valueOf(value).trim());
    }
}
