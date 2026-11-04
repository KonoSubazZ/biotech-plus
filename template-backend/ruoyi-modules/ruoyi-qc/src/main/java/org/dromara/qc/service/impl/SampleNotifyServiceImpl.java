package org.dromara.qc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.satoken.utils.TenantContext;
import org.dromara.qc.domain.BioinfoQc;
import org.dromara.qc.domain.SampleNotify;
import org.dromara.qc.domain.WetLabQc;
import org.dromara.qc.domain.bo.SampleNotifyBo;
import org.dromara.qc.domain.bo.SampleNotifyQueryBo;
import org.dromara.qc.domain.vo.QcNotifySampleVo;
import org.dromara.qc.domain.vo.SampleNotifyPrefillVo;
import org.dromara.qc.domain.vo.SampleNotifyResultVo;
import org.dromara.qc.domain.vo.SampleNotifyVo;
import org.dromara.qc.mapper.BioinfoQcMapper;
import org.dromara.qc.mapper.SampleNotifyMapper;
import org.dromara.qc.mapper.WetLabQcMapper;
import org.dromara.qc.service.IBioinfoQcService;
import org.dromara.qc.service.ISampleNotifyService;
import org.dromara.qc.service.IWetLabQcService;
import org.dromara.qc.utils.NotifyConstants;
import org.dromara.report.config.ReportRendererProperties;
import org.dromara.report.service.impl.OnlyOfficePdfConverter;
import org.dromara.report.service.impl.ReportDocxRenderer;
import org.dromara.report.utils.ArtifactPermissions;
import org.dromara.report.utils.ReportArtifactPaths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 样本通知台账 服务实现（生成侧）
 * <p>
 * 生成链路与报告模块同构：表单 → 组装 JSON → 原子写 JSON → 调 Python/docxtpl 渲染 DOCX → 转 PDF → 落台账。
 * 一条记录 = 一张通知单；同一天同一样本同一类型只有「未签名且未提交」时允许覆盖。
 *
 * @author <你的名字>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SampleNotifyServiceImpl implements ISampleNotifyService {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DAY8 = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 文件名基名最大长度（留目录余量） */
    private static final int FILE_BASE_MAX = 150;

    private final SampleNotifyMapper notifyMapper;
    private final BioinfoQcMapper bioinfoQcMapper;
    private final WetLabQcMapper wetLabQcMapper;
    private final IBioinfoQcService bioinfoQcService;
    private final IWetLabQcService wetLabQcService;
    private final ReportDocxRenderer reportDocxRenderer;
    private final OnlyOfficePdfConverter pdfConverter;
    private final ReportRendererProperties rendererProperties;
    private final ObjectMapper objectMapper;
    private final NotifyQcStatusWriter qcStatusWriter;

    /** 制品根目录（与报告模块同一个目录，容器里固定挂 /app/reports） */
    @Value("${report.artifact.root:/app/reports}")
    private String artifactRoot;

    /** 延期通知模板（相对模板根目录） */
    @Value("${qc.notify.templates.delay:notify/检测报告延迟通知单.docx}")
    private String delayTemplate;

    /** 三类不合格通知模板（相对模板根目录） */
    @Value("${qc.notify.templates.qc:notify/样本质控不合格通知单.docx}")
    private String qcTemplate;

    @Override
    public SampleNotifyPrefillVo prefill(String subbarcode, String notifyType) {
        String code = StringUtils.trim(subbarcode);
        if (StringUtils.isBlank(code)) {
            throw new ServiceException("样本编号不能为空");
        }
        String type = normalizeType(notifyType);
        QcNotifySampleVo sample = TenantContext.withoutTenant(() -> notifyMapper.selectSampleForNotify(code));
        if (sample == null) {
            throw new ServiceException("cms_sample 里查不到该样本编号：" + code);
        }
        String tenantId = TenantContext.withoutTenant(() -> notifyMapper.selectTenantIdBySubbarcode(code));
        if (StringUtils.isBlank(tenantId)) {
            return buildPrefill(sample, type, null, null, null, null);
        }
        return TenantContext.withTenant(tenantId, () -> {
            BioinfoQc bioinfo = latestBioinfoQc(code);
            WetLabQc wetLab = latestWetLabQc(code);
            Long bioinfoQcId = bioinfo == null ? null : bioinfo.getId();
            Long wetLabQcId = wetLab == null ? null : wetLab.getId();
            String analysisTime = null;
            String reason = null;
            if (NotifyConstants.Type.WET_LAB.equals(type)) {
                analysisTime = format(wetLab == null ? null : wetLab.getCreateTime());
                reason = wetLabQcService.failureDescription(code);
            } else if (NotifyConstants.Type.BIOINFO.equals(type)) {
                analysisTime = format(bioinfo == null ? null : bioinfo.getCreateTime());
                reason = bioinfoQcService.failureDescription(code);
            }
            return buildPrefill(sample, type, wetLabQcId, bioinfoQcId, analysisTime, reason);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SampleNotifyResultVo generate(SampleNotifyBo bo) {
        String type = normalizeType(bo.getNotifyType());
        bo.setNotifyType(type);
        String code = StringUtils.trim(bo.getSubbarcode());
        bo.setSubbarcode(code);
        QcNotifySampleVo sample = TenantContext.withoutTenant(() -> notifyMapper.selectSampleForNotify(code));
        if (sample == null) {
            throw new ServiceException("cms_sample 里查不到该样本编号：" + code);
        }
        String tenantId = TenantContext.withoutTenant(() -> notifyMapper.selectTenantIdBySubbarcode(code));
        if (StringUtils.isBlank(tenantId)) {
            throw new ServiceException("cms_sample 里查不到该样本编号的归属药企（tenant_id 为空），无法生成通知");
        }
        // 通知单归属「样本所属药企」：整段在它的租户作用域里跑，落库的 tenant_id 也由拦截器按它补
        return TenantContext.withTenant(tenantId, () -> renderAndSave(bo, sample));
    }

    @Override
    public TableDataInfo<SampleNotifyVo> selectPageList(SampleNotifyQueryBo bo, PageQuery pageQuery) {
        Page<SampleNotifyVo> page = pageQuery.build();
        List<SampleNotifyVo> rows = notifyMapper.selectPageList(page, bo);
        page.setRecords(rows);
        return TableDataInfo.build(page);
    }

    @Override
    public SampleNotifyVo queryById(Long id) {
        SampleNotifyVo vo = notifyMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("通知记录不存在：id=" + id);
        }
        if (StringUtils.isBlank(vo.getFilePath())) {
            vo.setFilePath(firstNotBlank(vo.getSignedPdfPath(), vo.getPdfPath(), vo.getRawDocxPath()));
        }
        return vo;
    }

    /** 组装 JSON → 渲染 DOCX → 转 PDF → 落库（新增或覆盖同一天的旧记录） */
    private SampleNotifyResultVo renderAndSave(SampleNotifyBo bo, QcNotifySampleVo sample) {
        String type = bo.getNotifyType();
        String subbarcode = sample.getSubbarcode();
        LocalDate today = LocalDate.now();
        String notifyDate = DAY8.format(today);

        SampleNotify existing = notifyMapper.selectByDayAndType(subbarcode, type, notifyDate);
        if (existing != null) {
            requireOverwritable(existing);
        }

        String fileBase = buildFileBase(sample, bo, type, notifyDate);
        Path jsonPath = ReportArtifactPaths.jsonDir(artifactRoot, ReportArtifactPaths.NOTIFY)
            .resolve(fileBase + ".json");
        Path docxPath = ReportArtifactPaths.docxDir(artifactRoot, ReportArtifactPaths.NOTIFY, today)
            .resolve(fileBase + ".docx");
        Map<String, Object> context = NotifyConstants.Type.isDelay(type)
            ? NotifyDocxContext.buildDelay(bo, sample)
            : NotifyDocxContext.buildQc(bo, sample);

        LocalDateTime now = LocalDateTime.now();
        writeJson(jsonPath, context);
        try {
            Map<String, Object> rendered = reportDocxRenderer.render(resolveTemplate(type), jsonPath, docxPath);
            ArtifactPermissions.relax(jsonPath);
            ArtifactPermissions.relax(docxPath);
            Path pdfPath = convertToPdfQuietly(docxPath);
            SampleNotify record = toRecord(bo, sample, jsonPath, docxPath, pdfPath, now,
                existing == null ? subbarcode + "-" + TIMESTAMP.format(now) : existing.getNotifyNo());
            boolean overwritten = existing != null;
            if (overwritten) {
                deleteObsoleteArtifacts(existing, jsonPath, docxPath, pdfPath);
                updateExisting(existing.getId(), record);
                record.setId(existing.getId());
            } else {
                notifyMapper.insert(record);
            }
            qcStatusWriter.write(subbarcode, type, NotifyConstants.Status.GENERATED);
            return toResult(record, rendered, overwritten);
        } catch (RuntimeException e) {
            deleteQuietly(jsonPath);
            deleteQuietly(docxPath);
            throw e;
        }
    }

    /** 同一天已有记录：只有「未签名且未提交」才允许覆盖 */
    private void requireOverwritable(SampleNotify existing) {
        if (NotifyConstants.Status.SENT.equals(existing.getNotifyStatus())) {
            throw new ServiceException("该通知已发送（发送时间 " + format(existing.getSenderTime()) + "），不能重新生成", 409);
        }
        if (NotifyConstants.Status.PENDING.equals(existing.getNotifyStatus())) {
            throw new ServiceException("该通知已提交待处理，不能重新生成", 409);
        }
        if (!NotifyConstants.Sign.NONE.equals(existing.getSignStatus())) {
            throw new ServiceException("该通知已送签/已签署，不能重新生成", 409);
        }
        if (!NotifyConstants.Status.GENERATED.equals(existing.getNotifyStatus())) {
            throw new ServiceException("该通知当前状态不允许重新生成：" + NotifyConstants.Status.label(existing.getNotifyStatus()), 409);
        }
    }

    /** 预填：先按 cms_sample 回填，再用最近一条同类型记录覆盖（并标记已生成） */
    private SampleNotifyPrefillVo buildPrefill(QcNotifySampleVo sample, String type, Long wetLabQcId,
                                               Long bioinfoQcId, String analysisTime, String failureReason) {
        SampleNotifyPrefillVo vo = new SampleNotifyPrefillVo();
        vo.setSampleFileId(sample.getId());
        vo.setSubbarcode(sample.getSubbarcode());
        vo.setSampleName(sample.getSampleName());
        vo.setStudyNo(sample.getStudyNo());
        vo.setProjectNo(sample.getProjectNo());
        vo.setParticipantNo(sample.getParticipantNo());
        vo.setCenter(sample.getCenter());
        vo.setReceiveDate(sample.getReceiveDate());
        vo.setSampleType(sample.getSampleType());
        vo.setSampleCount(sample.getSampleCount());
        vo.setProduct(sample.getProduct());
        // 延期通知：样本编号 / 样本类型 / 样本数量 展示同一订单下的全部样本（用「；」分隔）
        if (NotifyConstants.Type.isDelay(type)) {
            applySiblingSamples(vo, sample);
        }
        vo.setCustomerName(sample.getCustomerName());
        vo.setNotifyType(type);
        vo.setWetLabQcId(wetLabQcId);
        vo.setBioinfoQcId(bioinfoQcId);
        vo.setAnalysisTime(analysisTime);
        // 默认值：不合格环节默认勾在该质控所处的阶段（湿实验 = 核酸/文库；生信 = 测序；样本不合格 = 样本接收）
        if (NotifyConstants.Type.WET_LAB.equals(type)) {
            vo.setStageNucleic(true);
            vo.setStageLibrary(true);
        } else if (NotifyConstants.Type.BIOINFO.equals(type)) {
            vo.setStageSequencing(true);
        } else if (NotifyConstants.Type.SAMPLE.equals(type)) {
            vo.setStageReceive(true);
        }
        vo.setReason(failureReason);
        LoginUser loginUser = LoginHelper.getLoginUser();
        vo.setReporter(loginUser == null ? null : loginUser.getNickname());
        vo.setReportTime(MINUTE.format(LocalDateTime.now()));
        vo.setGenerated(false);
        SampleNotifyPrefillVo latest = notifyMapper.selectLatestByType(sample.getSubbarcode(), type);
        if (latest != null) {
            applyLatest(vo, latest);
        }
        return vo;
    }

    /**
     * 延期通知：把同一订单下的兄弟样本拼进来（当前点的那条排最前，其余按 id 升序）。
     * 只影响表头展示；台账的 subbarcode 仍是单条，文件命名也用单条。
     */
    private void applySiblingSamples(SampleNotifyPrefillVo vo, QcNotifySampleVo sample) {
        if (StringUtils.isBlank(sample.getOrderNo())) {
            return;
        }
        List<QcNotifySampleVo> rows = notifyMapper.selectSiblingsByOrder(sample.getOrderNo());
        if (rows == null || rows.size() <= 1) {
            return;
        }
        List<QcNotifySampleVo> ordered = new ArrayList<>();
        rows.stream()
            .filter(row -> sample.getSubbarcode().equals(row.getSubbarcode()))
            .findFirst()
            .ifPresent(ordered::add);
        rows.stream()
            .filter(row -> !sample.getSubbarcode().equals(row.getSubbarcode()))
            .forEach(ordered::add);
        vo.setSampleNoText(joinField(ordered, QcNotifySampleVo::getSubbarcode));
        vo.setSampleType(joinField(ordered, QcNotifySampleVo::getSampleType));
        vo.setSampleCount(joinField(ordered, QcNotifySampleVo::getSampleCount));
    }

    /** 同一字段多值拼接：去空、去重、保持顺序，分隔符「；」 */
    private String joinField(List<QcNotifySampleVo> rows, Function<QcNotifySampleVo, String> getter) {
        List<String> values = new ArrayList<>();
        for (QcNotifySampleVo row : rows) {
            String value = StringUtils.trim(getter.apply(row));
            if (StringUtils.isNotBlank(value) && !values.contains(value)) {
                values.add(value);
            }
        }
        return String.join("；", values);
    }

    /**
     * 带上一次生成的内容（重新生成时不用重填）
     * <p>
     * 只带「人工填写」的字段：表头那几项永远以 cms_sample 的最新值为准；多选位从落库的中文名反解回来。
     */
    private void applyLatest(SampleNotifyPrefillVo vo, SampleNotifyPrefillVo latest) {
        vo.setGenerated(true);
        vo.setLastNotifyId(latest.getLastNotifyId());
        vo.setNotifyNo(latest.getNotifyNo());
        vo.setNotifyStatus(latest.getNotifyStatus());
        vo.setSignStatus(latest.getSignStatus());
        vo.setLastGeneratedAt(latest.getLastGeneratedAt());
        vo.setLastDocxName(latest.getLastDocxName());
        vo.setLastPdfName(latest.getLastPdfName());
        vo.setSignContractId(latest.getSignContractId());
        vo.setSignedPdfPath(latest.getSignedPdfPath());
        vo.setWetLabQcId(latest.getWetLabQcId());
        vo.setBioinfoQcId(latest.getBioinfoQcId());
        if (StringUtils.isNotBlank(latest.getReason())) {
            vo.setReason(latest.getReason());
        }
        vo.setDelayReason(latest.getDelayReason());
        vo.setExpectedReportDate(latest.getExpectedReportDate());
        vo.setRemedyOtherText(latest.getRemedyOtherText());
        vo.setReporter(latest.getReporter());
        vo.setReportTime(latest.getReportTime());
        vo.setReviewer(latest.getReviewer());
        vo.setReviewTime(latest.getReviewTime());
        applyFlags(vo, latest);
    }

    /** 勾选位反解：落库存的是中文名（顿号分隔），空白说明上次没存 → 保留默认值 */
    private void applyFlags(SampleNotifyPrefillVo vo, SampleNotifyPrefillVo latest) {
        if (StringUtils.isNotBlank(latest.getQcStage())) {
            vo.setStageReceive(latest.getQcStage().contains("样本接收"));
            vo.setStagePathology(latest.getQcStage().contains("病理质控"));
            vo.setStageNucleic(latest.getQcStage().contains("核酸质控"));
            vo.setStageLibrary(latest.getQcStage().contains("文库质控"));
            vo.setStageSequencing(latest.getQcStage().contains("测序质控"));
        }
        if (StringUtils.isNotBlank(latest.getTerminateMeasure())) {
            vo.setTerminateYes(latest.getTerminateMeasure().contains("是"));
            vo.setTerminatePause(latest.getTerminateMeasure().contains("暂停"));
        }
        if (StringUtils.isNotBlank(latest.getRemedy())) {
            vo.setRemedyResample(latest.getRemedy().contains("重新采集"));
            vo.setRemedyWaxBlock(latest.getRemedy().contains("蜡块"));
            vo.setRemedyMoreSlices(latest.getRemedy().contains("切片"));
            vo.setRemedyResend(latest.getRemedy().contains("重新送样"));
            vo.setRemedyOther(latest.getRemedy().contains("其他"));
        }
    }

    /** 表单 → 台账记录 */
    private SampleNotify toRecord(SampleNotifyBo bo, QcNotifySampleVo sample, Path jsonPath, Path docxPath,
                                  Path pdfPath, LocalDateTime now, String notifyNo) {
        String type = bo.getNotifyType();
        boolean delay = NotifyConstants.Type.isDelay(type);
        SampleNotify record = new SampleNotify();
        record.setNotifyNo(notifyNo);
        record.setNotifyType(type);
        record.setNotifyStatus(NotifyConstants.Status.GENERATED);
        record.setSignStatus(NotifyConstants.Sign.NONE);
        record.setNotifyDate(DAY8.format(now));
        record.setSubbarcode(sample.getSubbarcode());
        record.setSampleName(sample.getSampleName());
        record.setSampleFileId(sample.getId());
        record.setWetLabQcId(delay || NotifyConstants.Type.SAMPLE.equals(type) ? null : latestWetLabQcId(sample.getSubbarcode()));
        record.setBioinfoQcId(delay || NotifyConstants.Type.SAMPLE.equals(type) ? null : latestBioinfoQcId(sample.getSubbarcode()));
        record.setStudyNo(text(bo.getStudyNo()));
        record.setProjectNo(text(bo.getProjectNo()));
        record.setParticipantNo(text(bo.getParticipantNo()));
        record.setAnalysisTime(analysisTime(sample.getSubbarcode(), type));
        record.setProduct(text(bo.getProduct()));
        record.setCustomerName(sample.getCustomerName());
        record.setSampleType(text(bo.getSampleType()));
        if (delay) {
            record.setDelayReason(text(bo.getDelayReason()));
            record.setExpectedReportDate(text(bo.getExpectedReportDate()));
        } else {
            record.setQcStage(NotifyDocxContext.joinSelected(bo, NotifyDocxContext.STAGES));
            record.setTerminateMeasure(NotifyDocxContext.joinSelected(bo, NotifyDocxContext.MEASURES));
            record.setRemedy(NotifyDocxContext.joinSelected(bo, NotifyDocxContext.REMEDIES));
            record.setRemedyOther(text(bo.getRemedyOtherText()));
            record.setReason(text(bo.getReason()));
        }
        record.setReporter(text(bo.getReporter()));
        record.setReportTime(text(bo.getReportTime()));
        record.setReviewer(text(bo.getReviewer()));
        record.setReviewTime(text(bo.getReviewTime()));
        record.setJsonPath(jsonPath.toString());
        record.setRawDocxPath(docxPath.toString());
        record.setPdfPath(pdfPath == null ? null : pdfPath.toString());
        record.setFilePath(pdfPath == null ? docxPath.toString() : pdfPath.toString());
        record.setHandlerBy(LoginHelper.getUserId());
        record.setHandlerTime(new Date());
        return record;
    }

    /** 覆盖：显式 set 所有业务列（含 null，避免旧 pdf_path 残留） */
    private void updateExisting(Long id, SampleNotify r) {
        notifyMapper.update(null, Wrappers.<SampleNotify>lambdaUpdate()
            .set(SampleNotify::getNotifyStatus, r.getNotifyStatus())
            .set(SampleNotify::getSignStatus, r.getSignStatus())
            .set(SampleNotify::getNotifyDate, r.getNotifyDate())
            .set(SampleNotify::getSampleName, r.getSampleName())
            .set(SampleNotify::getSampleFileId, r.getSampleFileId())
            .set(SampleNotify::getWetLabQcId, r.getWetLabQcId())
            .set(SampleNotify::getBioinfoQcId, r.getBioinfoQcId())
            .set(SampleNotify::getStudyNo, r.getStudyNo())
            .set(SampleNotify::getProjectNo, r.getProjectNo())
            .set(SampleNotify::getParticipantNo, r.getParticipantNo())
            .set(SampleNotify::getAnalysisTime, r.getAnalysisTime())
            .set(SampleNotify::getProduct, r.getProduct())
            .set(SampleNotify::getCustomerName, r.getCustomerName())
            .set(SampleNotify::getSampleType, r.getSampleType())
            .set(SampleNotify::getQcStage, r.getQcStage())
            .set(SampleNotify::getTerminateMeasure, r.getTerminateMeasure())
            .set(SampleNotify::getRemedy, r.getRemedy())
            .set(SampleNotify::getRemedyOther, r.getRemedyOther())
            .set(SampleNotify::getReason, r.getReason())
            .set(SampleNotify::getDelayReason, r.getDelayReason())
            .set(SampleNotify::getExpectedReportDate, r.getExpectedReportDate())
            .set(SampleNotify::getReporter, r.getReporter())
            .set(SampleNotify::getReportTime, r.getReportTime())
            .set(SampleNotify::getReviewer, r.getReviewer())
            .set(SampleNotify::getReviewTime, r.getReviewTime())
            .set(SampleNotify::getJsonPath, r.getJsonPath())
            .set(SampleNotify::getRawDocxPath, r.getRawDocxPath())
            .set(SampleNotify::getPdfPath, r.getPdfPath())
            .set(SampleNotify::getSignedPdfPath, null)
            .set(SampleNotify::getFilePath, r.getFilePath())
            .set(SampleNotify::getSignContractId, null)
            .set(SampleNotify::getHandlerBy, r.getHandlerBy())
            .set(SampleNotify::getHandlerTime, r.getHandlerTime())
            .set(SampleNotify::getUpdateTime, new Date())
            .eq(SampleNotify::getId, id));
    }

    /** 覆盖时清理不再被新记录引用的旧制品（样本名称变化会导致基名不同） */
    private void deleteObsoleteArtifacts(SampleNotify old, Path jsonPath, Path docxPath, Path pdfPath) {
        Set<String> keep = new HashSet<>();
        keep.add(jsonPath.toString());
        keep.add(docxPath.toString());
        if (pdfPath != null) {
            keep.add(pdfPath.toString());
        }
        List<String> olds = new ArrayList<>();
        olds.add(old.getJsonPath());
        olds.add(old.getRawDocxPath());
        olds.add(old.getPdfPath());
        olds.add(old.getSignedPdfPath());
        for (String path : olds) {
            if (StringUtils.isNotBlank(path) && !keep.contains(path)) {
                deleteQuietly(Path.of(path));
            }
        }
    }

    private SampleNotifyResultVo toResult(SampleNotify record, Map<String, Object> rendered, boolean overwritten) {
        SampleNotifyResultVo vo = new SampleNotifyResultVo();
        vo.setNotifyId(record.getId());
        vo.setNotifyNo(record.getNotifyNo());
        vo.setJsonPath(record.getJsonPath());
        vo.setDocxPath(record.getRawDocxPath());
        vo.setDocxName(Path.of(record.getRawDocxPath()).getFileName().toString());
        vo.setPdfPath(record.getPdfPath());
        if (record.getPdfPath() != null) {
            vo.setPdfName(Path.of(record.getPdfPath()).getFileName().toString());
        }
        vo.setFilePath(record.getFilePath());
        vo.setOverwritten(overwritten);
        vo.setOutputBytes(rendered.get("outputBytes"));
        vo.setSha256(String.valueOf(rendered.get("sha256")));
        vo.setElapsedMs(rendered.get("elapsedMs"));
        vo.setRendererLog(String.valueOf(rendered.get("rendererLog")));
        return vo;
    }

    /** 文件名基名：样本编号-样本名称（参与者编号）-项目编号-单据名-出具日期 */
    private String buildFileBase(QcNotifySampleVo sample, SampleNotifyBo bo, String type, String notifyDate) {
        String base = String.join("-",
            sanitize(sample.getSubbarcode()),
            sanitize(sample.getSampleName()) + "（" + sanitize(bo.getParticipantNo()) + "）",
            sanitize(bo.getProjectNo()),
            NotifyConstants.Type.docTypeName(type),
            notifyDate);
        return base.length() > FILE_BASE_MAX ? base.substring(0, FILE_BASE_MAX) : base;
    }

    /** 去掉文件名里不允许的字符（为空就留空段，按用户口径） */
    private String sanitize(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        return value.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "").trim();
    }

    private Path convertToPdfQuietly(Path docxPath) {
        try {
            Path pdfPath = pdfConverter.convert(docxPath,
                ReportArtifactPaths.pdfDir(artifactRoot, ReportArtifactPaths.NOTIFY, LocalDate.now()));
            ArtifactPermissions.relax(pdfPath);
            return pdfPath;
        } catch (RuntimeException e) {
            log.warn("通知单转 PDF 失败（不影响 DOCX 生成）：{}，{}", docxPath, e.getMessage());
            return null;
        }
    }

    /** 模板：模板固定目录 + 相对路径，必须落在模板目录内 */
    private Path resolveTemplate(String type) {
        String root = rendererProperties.getTemplatesRoot();
        if (StringUtils.isBlank(root)) {
            throw new ServiceException("未配置模板目录（report.renderer.templates-root），无法生成通知文档");
        }
        String template = NotifyConstants.Type.isDelay(type) ? delayTemplate : qcTemplate;
        Path base = Path.of(root).toAbsolutePath().normalize();
        Path file = base.resolve(template).normalize();
        if (!file.startsWith(base)) {
            throw new ServiceException("通知模板路径越出模板目录：" + template + "（模板目录 " + base + "）");
        }
        if (!Files.isRegularFile(file)) {
            throw new ServiceException("通知模板不存在：" + file + "（把 " + template + " 放到该目录）");
        }
        return file;
    }

    private void writeJson(Path target, Map<String, Object> context) {
        try {
            Files.createDirectories(target.getParent());
            Path temp = Files.createTempFile(target.getParent(), "." + target.getFileName(), ".tmp");
            Files.writeString(temp, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context),
                StandardCharsets.UTF_8);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new ServiceException("写通知 JSON 失败：" + target + "，" + e.getMessage());
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("清理通知制品失败：{}，{}", path, e.getMessage());
        }
    }

    private String normalizeType(String notifyType) {
        String type = StringUtils.trim(notifyType);
        if (!NotifyConstants.Type.isValid(type)) {
            throw new ServiceException("通知类型不合法：" + notifyType);
        }
        return type;
    }

    private BioinfoQc latestBioinfoQc(String subbarcode) {
        return bioinfoQcMapper.selectOne(new LambdaQueryWrapper<BioinfoQc>()
            .eq(BioinfoQc::getSubbarcode, subbarcode)
            .orderByDesc(BioinfoQc::getId)
            .last("LIMIT 1"));
    }

    private WetLabQc latestWetLabQc(String subbarcode) {
        return wetLabQcMapper.selectOne(new LambdaQueryWrapper<WetLabQc>()
            .eq(WetLabQc::getSubbarcode, subbarcode)
            .orderByDesc(WetLabQc::getId)
            .last("LIMIT 1"));
    }

    private Long latestBioinfoQcId(String subbarcode) {
        BioinfoQc row = latestBioinfoQc(subbarcode);
        return row == null ? null : row.getId();
    }

    private Long latestWetLabQcId(String subbarcode) {
        WetLabQc row = latestWetLabQc(subbarcode);
        return row == null ? null : row.getId();
    }

    private String analysisTime(String subbarcode, String type) {
        Date time = null;
        if (NotifyConstants.Type.WET_LAB.equals(type)) {
            WetLabQc row = latestWetLabQc(subbarcode);
            time = row == null ? null : row.getCreateTime();
        } else if (NotifyConstants.Type.BIOINFO.equals(type)) {
            BioinfoQc row = latestBioinfoQc(subbarcode);
            time = row == null ? null : row.getCreateTime();
        }
        return format(time);
    }

    private String format(Date date) {
        return date == null ? null : MINUTE.format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
    }

    private String text(String value) {
        return StringUtils.blankToDefault(value, "");
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    // ── 下载 ─────────────────────────────────────────────────────

    @Override
    public File loadDocx(Long id) {
        SampleNotify record = requireNotify(id);
        return loadArtifact(record.getRawDocxPath(), "通知记录不存在或还没生成文档：id=" + id);
    }

    @Override
    public File loadPdf(Long id) {
        SampleNotify record = requireNotify(id);
        return loadArtifact(record.getPdfPath(), "通知记录不存在或还没生成 PDF：id=" + id);
    }

    @Override
    public File loadSignedPdf(Long id) {
        SampleNotify record = requireNotify(id);
        return loadArtifact(record.getSignedPdfPath(), "通知记录不存在或还没有签名后 PDF：id=" + id);
    }

    private File loadArtifact(String path, String message) {
        if (StringUtils.isBlank(path)) {
            throw new ServiceException(message);
        }
        Path root = Path.of(artifactRoot).toAbsolutePath().normalize();
        Path file = Path.of(path).toAbsolutePath().normalize();
        if (!file.startsWith(root)) {
            throw new ServiceException("通知文档路径越出制品目录：" + file);
        }
        if (!Files.isRegularFile(file)) {
            throw new ServiceException("通知文档不存在，可能已被清理：" + file);
        }
        return file.toFile();
    }

    private SampleNotify requireNotify(Long id) {
        SampleNotify record = notifyMapper.selectById(id);
        if (record == null) {
            throw new ServiceException("通知记录不存在：id=" + id);
        }
        return record;
    }
}
