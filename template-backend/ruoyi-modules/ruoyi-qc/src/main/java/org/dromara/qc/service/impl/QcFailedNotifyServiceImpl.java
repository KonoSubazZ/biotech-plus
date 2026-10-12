package org.dromara.qc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.satoken.utils.TenantContext;
import org.dromara.qc.domain.BioinfoQc;
import org.dromara.qc.domain.QcFailedNotify;
import org.dromara.qc.domain.WetLabQc;
import org.dromara.qc.domain.bo.QcFailedNotifyBo;
import org.dromara.qc.domain.vo.QcFailedNotifyPrefillVo;
import org.dromara.qc.domain.vo.QcFailedNotifyResultVo;
import org.dromara.qc.domain.vo.QcNotifySampleVo;
import org.dromara.qc.mapper.BioinfoQcMapper;
import org.dromara.qc.mapper.QcFailedNotifyMapper;
import org.dromara.qc.mapper.WetLabQcMapper;
import org.dromara.qc.service.IBioinfoQcService;
import org.dromara.qc.service.IQcFailedNotifyService;
import org.dromara.qc.service.IWetLabQcService;
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
import java.util.List;
import java.util.Map;

/**
 * 样本质控不合格通知 服务实现
 * <p>
 * 生成链路（与报告模块同构）：表单 → 组装 JSON → 原子写 JSON → 调 Python/docxtpl 渲染 DOCX → 落生成记录。
 * 勾选项在页面是多选框，落库存中文名（顿号分隔），渲染时模板用 {@code {{ box(flag) }}} 出 ☑/□。
 *
 * @author <你的名字>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QcFailedNotifyServiceImpl implements IQcFailedNotifyService {

    /** 通知状态：生成文档后即为「已生成（文档）」；「已发送（邮件）」等邮件功能上线后由发送动作改写 */
    private static final String STATUS_GENERATED = "已生成（文档）";

    /** 质控类型：生信（默认） */
    private static final String CATEGORY_BIOINFO = "bioinfo";
    /** 质控类型：湿实验 */
    private static final String CATEGORY_WET_LAB = "wet_lab";

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final QcFailedNotifyMapper notifyMapper;
    private final BioinfoQcMapper bioinfoQcMapper;
    private final WetLabQcMapper wetLabQcMapper;
    private final IBioinfoQcService bioinfoQcService;
    private final IWetLabQcService wetLabQcService;
    private final ReportDocxRenderer reportDocxRenderer;
    private final OnlyOfficePdfConverter pdfConverter;
    private final ReportRendererProperties rendererProperties;
    private final ObjectMapper objectMapper;

    /** 制品根目录（与报告模块同一个目录，容器里固定挂 /app/reports） */
    @Value("${report.artifact.root:/app/reports}")
    private String artifactRoot;

    /**
     * 通知模板文件（相对模板固定目录 report.renderer.templates-root 的路径，可带子目录）
     * <p>
     * 默认 {@code notify/样本质控不合格通知-v1.docx}；换版本/换文件只改配置（yml 或环境变量 QC_NOTIFY_TEMPLATE），
     * 不用改代码。
     */
    @Value("${qc.notify.template:notify/样本质控不合格通知-v1.docx}")
    private String templateName;

    @Override
    public QcFailedNotifyPrefillVo prefill(String subbarcode, String qcCategory) {
        String code = StringUtils.trim(subbarcode);
        if (StringUtils.isBlank(code)) {
            throw new ServiceException("样本编号不能为空");
        }
        String category = normalizeCategory(qcCategory);
        QcNotifySampleVo sample = TenantContext.withoutTenant(() -> notifyMapper.selectSampleForNotify(code));
        if (sample == null) {
            throw new ServiceException("sample_file 里查不到该样本编号：" + code);
        }
        // 归属药企与两个质控接口同一口径：取样本自身的 sample_file.tenant_id
        // （不走「客户名 → sys_tenant」那条 —— 客户名对不上时会让通知也生成不了）
        String tenantId = TenantContext.withoutTenant(() -> notifyMapper.selectTenantIdBySubbarcode(code));
        if (StringUtils.isBlank(tenantId)) {
            // 药企没解析出来：QC 记录也不会存在，只回填样本字段 + 默认值
            return buildPrefill(sample, category, null, null, null, "");
        }
        return TenantContext.withTenant(tenantId, () -> {
            BioinfoQc bioinfo = latestBioinfoQc(code);
            WetLabQc wetLab = latestWetLabQc(code);
            Long bioinfoQcId = bioinfo == null ? null : bioinfo.getId();
            Long wetLabQcId = wetLab == null ? null : wetLab.getId();
            // 原因与「分析时间」都取该类质控记录的那一份（湿实验页点处理就读湿实验的）
            Date recordTime;
            String reason;
            if (CATEGORY_WET_LAB.equals(category)) {
                recordTime = wetLab == null ? null : wetLab.getCreateTime();
                reason = wetLabQcService.failureDescription(code);
            } else {
                recordTime = bioinfo == null ? null : bioinfo.getCreateTime();
                reason = bioinfoQcService.failureDescription(code);
            }
            return buildPrefill(sample, category, wetLabQcId, bioinfoQcId, format(recordTime), reason);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QcFailedNotifyResultVo generate(QcFailedNotifyBo bo) {
        String code = StringUtils.trim(bo.getSubbarcode());
        String category = normalizeCategory(bo.getQcCategory());
        bo.setQcCategory(category);
        QcNotifySampleVo sample = TenantContext.withoutTenant(() -> notifyMapper.selectSampleForNotify(code));
        if (sample == null) {
            throw new ServiceException("sample_file 里查不到该样本编号：" + code);
        }
        String tenantId = TenantContext.withoutTenant(() -> notifyMapper.selectTenantIdBySubbarcode(code));
        if (StringUtils.isBlank(tenantId)) {
            throw new ServiceException("sample_file 里查不到该样本编号的归属药企（tenant_id 为空），无法生成通知");
        }
        // 通知单归属「样本所属药企」：整段在它的租户作用域里跑，落库的 tenant_id 也由拦截器按它补
        return TenantContext.withTenant(tenantId, () -> renderAndSave(bo, sample));
    }

    /** 质控类型归一：只认 wet_lab / bioinfo，其余（含空）按 bioinfo 处理（老调用方兼容） */
    private String normalizeCategory(String qcCategory) {
        return CATEGORY_WET_LAB.equalsIgnoreCase(StringUtils.trim(qcCategory))
            ? CATEGORY_WET_LAB : CATEGORY_BIOINFO;
    }

    /** 组装 JSON → 渲染 DOCX → 落生成记录；渲染失败要把本次 JSON/DOCX 清掉，不留半成品 */
    private QcFailedNotifyResultVo renderAndSave(QcFailedNotifyBo bo, QcNotifySampleVo sample) {
        LocalDateTime now = LocalDateTime.now();
        String subbarcode = sample.getSubbarcode();
        String fileBase = "qc-failed-notify-" + subbarcode + "-" + TIMESTAMP.format(now);
        Path jsonPath = ReportArtifactPaths.jsonDir(artifactRoot, ReportArtifactPaths.NOTIFY)
            .resolve(fileBase + ".json");
        Path docxPath = ReportArtifactPaths.docxDir(artifactRoot, ReportArtifactPaths.NOTIFY, LocalDate.now())
            .resolve(fileBase + ".docx");
        Map<String, Object> context = QcNotifyDocxContext.build(bo, sample);
        writeJson(jsonPath, context);
        try {
            Map<String, Object> rendered = reportDocxRenderer.render(resolveTemplate(), jsonPath, docxPath);
            ArtifactPermissions.relax(jsonPath);
            ArtifactPermissions.relax(docxPath);
            Path pdfPath = convertToPdfQuietly(docxPath);
            QcFailedNotify record = toRecord(bo, sample, jsonPath, docxPath, pdfPath, now);
            notifyMapper.insert(record);
            writeBackNotifyStatus(record);
            return toResult(record, rendered);
        } catch (RuntimeException e) {
            deleteQuietly(jsonPath);
            deleteQuietly(docxPath);
            throw e;
        }
    }

    /**
     * 静默转 PDF：失败只记日志，不影响通知单生成。
     * <p>
     * PDF 是附加交付物（后续发送/归档要用）；转换失败时 pdf_path 留空，重新生成即可补上，
     * 不因为 Document Server 抖动就把已经渲染好的 DOCX 一起回滚。
     *
     * @param docxPath 已生成的 DOCX 路径
     * @return PDF 路径；转换失败返回 null
     */
    private Path convertToPdfQuietly(Path docxPath) {
        try {
            Path pdfPath = pdfConverter.convert(docxPath,
                ReportArtifactPaths.pdfDir(artifactRoot, ReportArtifactPaths.NOTIFY, LocalDate.now()));
            ArtifactPermissions.relax(pdfPath);
            return pdfPath;
        } catch (RuntimeException e) {
            log.warn("通知单转 PDF 失败（不影响 DOCX 生成）：{}，{}", docxPath, e.getMessage());
            // 转换失败：pdf_path 留空（重新生成可补上），通知单本身继续落库
            return null;
        }
    }

    /** 预填：先按 sample_file 回填，再用最近一条通知记录覆盖（并标记已生成） */
    private QcFailedNotifyPrefillVo buildPrefill(QcNotifySampleVo sample, String qcCategory, Long wetLabQcId,
                                                 Long bioinfoQcId, String analysisTime, String failureReason) {
        QcFailedNotifyPrefillVo vo = new QcFailedNotifyPrefillVo();
        vo.setSampleFileId(sample.getId());
        vo.setSubbarcode(sample.getSubbarcode());
        vo.setStudyNo(sample.getStudyNo());
        vo.setProjectNo(sample.getProjectNo());
        vo.setParticipantNo(sample.getParticipantNo());
        vo.setCenter(sample.getCenter());
        vo.setReceiveDate(sample.getReceiveDate());
        vo.setSampleType(sample.getSampleType());
        vo.setSampleCount(sample.getSampleCount());
        vo.setProduct(sample.getProduct());
        vo.setCustomerName(sample.getCustomerName());
        vo.setQcCategory(qcCategory);
        vo.setWetLabQcId(wetLabQcId);
        vo.setBioinfoQcId(bioinfoQcId);
        vo.setAnalysisTime(analysisTime);
        // 默认值：不合格环节默认勾在该质控所处的阶段（湿实验 = 核酸/文库；生信 = 测序），
        // 原因 = 按质控标准算出来的不达标项，反馈人/时间 = 当前用户/现在
        if (CATEGORY_WET_LAB.equals(qcCategory)) {
            vo.setStageNucleic(true);
            vo.setStageLibrary(true);
        } else {
            vo.setStageSequencing(true);
        }
        vo.setReason(failureReason);
        LoginUser loginUser = LoginHelper.getLoginUser();
        vo.setReporter(loginUser == null ? null : loginUser.getNickname());
        vo.setReportTime(MINUTE.format(LocalDateTime.now()));
        vo.setGenerated(false);
        QcFailedNotifyPrefillVo latest = notifyMapper.selectLatestBySubbarcode(sample.getSubbarcode());
        if (latest != null) {
            applyLatest(vo, latest);
        }
        return vo;
    }

    /**
     * 带上一次生成的内容（重新生成时不用重填）
     * <p>
     * 只带「人工填写」的字段：表头那几项永远以 sample_file 的最新值为准（样本信息后来可能被补全），
     * 多选位从落库的中文名反解回来。
     */
    private void applyLatest(QcFailedNotifyPrefillVo vo, QcFailedNotifyPrefillVo latest) {
        vo.setGenerated(true);
        vo.setLastNotifyId(latest.getLastNotifyId());
        vo.setNotifyNo(latest.getNotifyNo());
        vo.setNotifyStatus(latest.getNotifyStatus());
        vo.setLastGeneratedAt(latest.getLastGeneratedAt());
        vo.setLastDocxName(latest.getLastDocxName());
        vo.setLastPdfName(latest.getLastPdfName());
        vo.setWetLabQcId(latest.getWetLabQcId());
        vo.setBioinfoQcId(latest.getBioinfoQcId());
        // 原因本轮不由页面填：上次记录里为空时，保留后端按质控标准算出的不达标项（生信据此填写）
        if (StringUtils.isNotBlank(latest.getReason())) {
            vo.setReason(latest.getReason());
        }
        vo.setRemedyOtherText(latest.getRemedyOtherText());
        vo.setReporter(latest.getReporter());
        vo.setReportTime(latest.getReportTime());
        vo.setReviewer(latest.getReviewer());
        vo.setReviewTime(latest.getReviewTime());
        applyFlags(vo, latest);
    }

    /** 勾选位反解：落库存的是中文名（顿号分隔），空白说明上次没存 → 保留默认值 */
    private void applyFlags(QcFailedNotifyPrefillVo vo, QcFailedNotifyPrefillVo latest) {
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

    /** 表单 → 生成记录（多选转中文名落库，便于直接看库/检索） */
    private QcFailedNotify toRecord(QcFailedNotifyBo bo, QcNotifySampleVo sample, Path jsonPath, Path docxPath,
                                    Path pdfPath, LocalDateTime now) {
        QcFailedNotify record = new QcFailedNotify();
        record.setSubbarcode(sample.getSubbarcode());
        record.setSampleFileId(sample.getId());
        record.setQcCategory(bo.getQcCategory());
        record.setNotifyNo(sample.getSubbarcode() + "-" + TIMESTAMP.format(now));
        record.setNotifyStatus(STATUS_GENERATED);
        record.setNotifyDate(DAY.format(now));
        record.setAnalysisTime(analysisTime(sample.getSubbarcode()));
        record.setProduct(text(bo.getProduct()));
        record.setCustomerName(sample.getCustomerName());
        record.setSampleType(text(bo.getSampleType()));
        record.setQcStage(QcNotifyDocxContext.joinSelected(bo, QcNotifyDocxContext.STAGES));
        record.setTerminateMeasure(QcNotifyDocxContext.joinSelected(bo, QcNotifyDocxContext.MEASURES));
        record.setRemedy(QcNotifyDocxContext.joinSelected(bo, QcNotifyDocxContext.REMEDIES));
        record.setRemedyOther(text(bo.getRemedyOtherText()));
        record.setReason(text(bo.getReason()));
        record.setReporter(text(bo.getReporter()));
        record.setReportTime(text(bo.getReportTime()));
        record.setReviewer(text(bo.getReviewer()));
        record.setReviewTime(text(bo.getReviewTime()));
        record.setJsonPath(jsonPath.toString());
        record.setRawDocxPath(docxPath.toString());
        record.setPdfPath(pdfPath == null ? null : pdfPath.toString());
        BioinfoQc bioinfoQc = latestBioinfoQc(sample.getSubbarcode());
        record.setBioinfoQcId(bioinfoQc == null ? null : bioinfoQc.getId());
        WetLabQc wetLabQc = latestWetLabQc(sample.getSubbarcode());
        record.setWetLabQcId(wetLabQc == null ? null : wetLabQc.getId());
        return record;
    }

    /**
     * 生成通知后回写质控行的通知状态：这条通知是哪个类型就写哪张表
     * （wet_lab → wet_lab_qc / bioinfo → bioinfo_qc）。
     * <p>
     * 只更新 notify_status 一列，别的列不碰（用 UpdateWrapper 而不是整行覆盖，避免把质检值写脏）。
     * 页面暂不展示这一列，先把状态记下来。
     */
    private void writeBackNotifyStatus(QcFailedNotify record) {
        String subbarcode = record.getSubbarcode();
        if (CATEGORY_WET_LAB.equals(record.getQcCategory())) {
            wetLabQcMapper.update(null, Wrappers.<WetLabQc>lambdaUpdate()
                .set(WetLabQc::getNotifyStatus, record.getNotifyStatus())
                .eq(WetLabQc::getSubbarcode, subbarcode));
            return;
        }
        bioinfoQcMapper.update(null, Wrappers.<BioinfoQc>lambdaUpdate()
            .set(BioinfoQc::getNotifyStatus, record.getNotifyStatus())
            .eq(BioinfoQc::getSubbarcode, subbarcode));
    }

    private QcFailedNotifyResultVo toResult(QcFailedNotify record, Map<String, Object> rendered) {
        QcFailedNotifyResultVo vo = new QcFailedNotifyResultVo();
        vo.setNotifyId(record.getId());
        vo.setNotifyNo(record.getNotifyNo());
        vo.setJsonPath(record.getJsonPath());
        vo.setDocxPath(record.getRawDocxPath());
        vo.setDocxName(Path.of(record.getRawDocxPath()).getFileName().toString());
        String pdfPath = record.getPdfPath();
        vo.setPdfPath(pdfPath);
        if (pdfPath != null) {
            vo.setPdfName(Path.of(pdfPath).getFileName().toString());
        }
        vo.setOutputBytes(rendered.get("outputBytes"));
        vo.setSha256(String.valueOf(rendered.get("sha256")));
        vo.setElapsedMs(rendered.get("elapsedMs"));
        vo.setRendererLog(String.valueOf(rendered.get("rendererLog")));
        return vo;
    }

    /** 取该样本当前生信质控记录（唯一键 tenant_id + subbarcode）；没有返回 null（湿实验阶段就不合格时） */
    private BioinfoQc latestBioinfoQc(String subbarcode) {
        return bioinfoQcMapper.selectOne(new LambdaQueryWrapper<BioinfoQc>()
            .eq(BioinfoQc::getSubbarcode, subbarcode)
            .orderByDesc(BioinfoQc::getId)
            .last("LIMIT 1"));
    }

    /** 取该样本当前湿实验质控记录（宽表，唯一键 tenant_id + subbarcode）；没有返回 null */
    private WetLabQc latestWetLabQc(String subbarcode) {
        return wetLabQcMapper.selectOne(new LambdaQueryWrapper<WetLabQc>()
            .eq(WetLabQc::getSubbarcode, subbarcode)
            .orderByDesc(WetLabQc::getId)
            .last("LIMIT 1"));
    }

    private String analysisTime(String subbarcode) {
        BioinfoQc record = latestBioinfoQc(subbarcode);
        return record == null ? null : format(record.getCreateTime());
    }

    @Override
    public File loadDocx(Long id) {
        QcFailedNotify record = notifyMapper.selectById(id);
        if (record == null || StringUtils.isBlank(record.getRawDocxPath())) {
            throw new ServiceException("通知记录不存在或还没生成文档：id=" + id);
        }
        Path root = Path.of(artifactRoot).toAbsolutePath().normalize();
        Path file = Path.of(record.getRawDocxPath()).toAbsolutePath().normalize();
        if (!file.startsWith(root)) {
            throw new ServiceException("通知文档路径越出制品目录：" + file);
        }
        if (!Files.isRegularFile(file)) {
            throw new ServiceException("通知文档不存在，可能已被清理：" + file);
        }
        return file.toFile();
    }

    @Override
    public File loadPdf(Long id) {
        QcFailedNotify record = notifyMapper.selectById(id);
        if (record == null || StringUtils.isBlank(record.getPdfPath())) {
            throw new ServiceException("通知记录不存在或还没生成 PDF：id=" + id);
        }
        Path root = Path.of(artifactRoot).toAbsolutePath().normalize();
        Path file = Path.of(record.getPdfPath()).toAbsolutePath().normalize();
        if (!file.startsWith(root)) {
            throw new ServiceException("通知 PDF 路径越出制品目录：" + file);
        }
        if (!Files.isRegularFile(file)) {
            throw new ServiceException("通知 PDF 不存在，可能已被清理：" + file);
        }
        return file.toFile();
    }

    /**
     * 模板文件：模板固定目录 + 相对路径（可带子目录，如 {@code notify/xxx.docx}）
     * <p>
     * 找不到直接报错，不猜路径、不兜底；路径必须落在模板目录内（防止配置里写 ../ 越出去）。
     */
    private Path resolveTemplate() {
        String root = rendererProperties.getTemplatesRoot();
        if (StringUtils.isBlank(root)) {
            throw new ServiceException("未配置模板目录（report.renderer.templates-root），无法生成通知文档");
        }
        Path base = Path.of(root).toAbsolutePath().normalize();
        Path file = base.resolve(templateName).normalize();
        if (!file.startsWith(base)) {
            throw new ServiceException("通知模板路径越出模板目录：" + templateName + "（模板目录 " + base + "）");
        }
        if (!Files.isRegularFile(file)) {
            throw new ServiceException("通知模板不存在：" + file + "（把 " + templateName + " 放到该目录）");
        }
        return file;
    }

    /** 原子写 JSON：同目录临时文件 + ATOMIC_MOVE */
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


    private String text(String value) {
        return StringUtils.blankToDefault(value, "");
    }

    private String format(Date date) {
        return date == null ? null : MINUTE.format(LocalDateTime.ofInstant(date.toInstant(),
            ZoneId.systemDefault()));
    }
}
