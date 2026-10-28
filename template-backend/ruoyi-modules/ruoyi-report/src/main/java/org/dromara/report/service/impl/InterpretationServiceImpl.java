package org.dromara.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.drive.domain.vo.DriveFileVo;
import org.dromara.drive.service.IDriveService;
import org.dromara.report.domain.AnalysisReport;
import org.dromara.report.domain.SampleInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.report.domain.bo.InterpretationFileQueryBo;
import org.dromara.report.domain.bo.InterpretationQueryBo;
import org.dromara.report.domain.bo.InterpretationVariantQueryBo;
import org.dromara.report.domain.bo.InterpretationVariantStatusBo;
import org.dromara.report.domain.vo.AnalysisReportVo;
import org.dromara.report.domain.vo.AnalysisSnapshotVo;
import org.dromara.report.domain.vo.InterpretationContextVo;
import org.dromara.report.domain.vo.InterpretationFileContentVo;
import org.dromara.report.domain.vo.FileRedriveVo;
import org.dromara.report.domain.vo.InterpretationFileVo;
import org.dromara.report.domain.vo.InterpretationLimsVo;
import org.dromara.report.domain.vo.InterpretationRowVo;
import org.dromara.report.mapper.AnalysisReportMapper;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.mapper.ReportTemplateMapper;
import org.dromara.report.mapper.SampleInfoMapper;
import org.dromara.report.service.IInterpretationService;
import org.dromara.report.utils.ReportStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dromara.report.service.impl.revision.ReportOperationLock;

/**
 * 报告解读 业务层处理
 * <p>
 * 本页承担「列表 + 进入解读 + 解读上下文（LIMS）」：
 * <ul>
 *   <li>列表 = analysis_data ⟕ 该批次最新一份 analysis_report（一个批次可有多份报告，列表只显示最新那份的状态）。</li>
 *   <li>进入解读 = 复用该批次「未完成」（report_status ≠ 62）的报告；没有才新建（状态 20 生信看点），
 *       样本/产品信息从 analysis_data 复制。</li>
 *   <li>上下文 = 报告头 + LIMS 信息（读 sample_file，按 subbarcode = subbarcode）+ 生成前校验结论。</li>
 * </ul>
 * 位点筛选、预览、审核、发送属于后续页面的能力，不在这里实现。
 *
 * @author <你的名字>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class InterpretationServiceImpl implements IInterpretationService {

    /** 单个文件内容最多返回的字符数（超大文件在库里 LEFT() 截断，避免整个 LONGTEXT 进内存/浏览器） */
    private static final int MAX_FILE_TEXT_LENGTH = 200_000;

    /** 位点类型白名单：与 history_somatic.source_type 的取值口径一致 */
    private static final Set<String> VARIANT_SOURCE_TYPES = Set.of("SNP_INDEL", "CNV", "FUSION", "CR_ALL");

    private final InterpretationMapper interpretationMapper;
    private final AnalysisReportMapper analysisReportMapper;
    private final SampleInfoMapper sampleInfoMapper;
    private final ReportTemplateMapper reportTemplateMapper;

    /** 驱动入库服务（重新驱动单个文件；能力在 ruoyi-drive） */
    private final IDriveService driveService;
    private final ReportOperationLock operationLock;

    @Override
    public TableDataInfo<InterpretationRowVo> selectPageList(InterpretationQueryBo bo, PageQuery pageQuery) {
        Page<InterpretationRowVo> page = interpretationMapper.selectInterpretationPage(pageQuery.build(), bo);
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnalysisReportVo enter(Long analysisId) {
        return enter(analysisId, false);
    }

    /**
     * 进入解读；readonly=true 时只返回该批次已有报告，不新建/不补快照/不校验流转状态
     * （已完成报告「查看解读记录」用）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnalysisReportVo enter(Long analysisId, boolean readonly) {
        if (analysisId == null) {
            throw new ServiceException("分析数据ID不能为空");
        }
        if (readonly) {
            AnalysisReport existing = selectReusableReport(analysisId);
            if (existing == null) {
                throw new ServiceException("该批次还没有报告记录，无法查看解读：analysisId=" + analysisId);
            }
            return analysisReportMapper.selectVoById(existing.getReportId());
        }
        return operationLock.call(-analysisId, () -> enterLocked(analysisId, null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimBioinfoView(Long analysisId, Long reportId) {
        claimBioinfo(analysisId, reportId, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimBioinfoReview(Long analysisId, Long reportId) {
        claimBioinfo(analysisId, reportId, false);
    }

    /**
     * 生信环节认领：只占人（report_bioinfo_viewed_by / reviewed_by），不改状态、不写时间。
     * 状态不对、已被他人占住、复核自己看点的报告都直接拒绝。
     */
    private void claimBioinfo(Long analysisId, Long reportId, boolean view) {
        if (analysisId == null || reportId == null) {
            throw new ServiceException("analysisId 与 reportId 不能为空");
        }
        Map<String, Object> report = interpretationMapper.selectReportRow(reportId);
        if (report == null || !String.valueOf(analysisId).equals(String.valueOf(report.get("analysisId")))) {
            throw new ServiceException("报告不存在或不属于该分析批次：reportId=" + reportId
                + "，analysisId=" + analysisId);
        }
        int status = Integer.parseInt(String.valueOf(report.get("status")));
        int expected = view ? ReportStatus.BIOINFO_VIEW : ReportStatus.BIOINFO_REVIEW;
        if (status != expected) {
            throw new ServiceException("当前状态（" + ReportStatus.label(status) + "）不能认领：reportId=" + reportId);
        }
        if (!view) {
            String viewer = toText(report.get("reportBioinfoViewedBy"));
            if (viewer != null && viewer.equals(String.valueOf(LoginHelper.getUserId()))) {
                throw new ServiceException("不能复核自己看点的报告：reportId=" + reportId);
            }
        }
        int claimed = view
            ? interpretationMapper.claimBioinfoView(reportId, LoginHelper.getUserId())
            : interpretationMapper.claimBioinfoReview(reportId, LoginHelper.getUserId());
        if (claimed == 0) {
            throw new ServiceException("该报告已被他人认领，请刷新列表：reportId=" + reportId);
        }
        log.info("生信环节认领：reportId={}，环节={}，认领人={}", reportId, view ? "生信看点" : "生信复核",
            LoginHelper.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnalysisReportVo enterAfterRescan(Long analysisId, Long sourceReportId) {
        return operationLock.call(-analysisId, () -> enterLocked(analysisId, sourceReportId));
    }

    private AnalysisReportVo enterLocked(Long analysisId, Long sourceReportId) {
        if (analysisId == null) {
            throw new ServiceException("分析数据ID不能为空");
        }
        AnalysisSnapshotVo snapshot = interpretationMapper.selectAnalysisSnapshot(analysisId);
        if (snapshot == null) {
            throw new ServiceException("分析数据不存在或已删除：analysisId=" + analysisId);
        }

        // 同一个批次只维护一份报告：只要它还「未完成」（report_status ≠ 62）就复用它，
        // 只有报告已发送成功（完成）后再点「解读」才新建（用户 2026-10-16 口径）
        AnalysisReport existing = selectReusableReport(analysisId);
        if (existing != null && !Integer.valueOf(ReportStatus.COMPLETED).equals(existing.getReportStatus())) {
            // 老报告（本功能上线前建的）癌种/产品ID 可能为空：点「解读」时补齐，不覆盖已有值
            ensureReportSnapshot(existing);
            return analysisReportMapper.selectVoById(existing.getReportId());
        }
        if (existing != null && (sourceReportId == null || !sourceReportId.equals(existing.getReportId()))) {
            throw new ServiceException("报告已发送，请使用「重新解读」完成整批重扫后创建新报告");
        }
        // 重新解读会新建一个分析批次（不同 analysis_id），所以这里不能用「本批次已有报告」判断来源是否存在，
        // 直接按 reportId 查来源报告（用户 2026-10-21：上传/重新解读新建批次）
        AnalysisReportVo source = sourceReportId == null ? null : analysisReportMapper.selectVoById(sourceReportId);
        if (sourceReportId != null && source == null) {
            throw new ServiceException("重新解读来源报告不存在：reportId=" + sourceReportId);
        }

        AnalysisReport report = new AnalysisReport();
        report.setSourceReportId(sourceReportId);
        if (source != null) {
            // 模板沿用来来源报告（新批次此时还没有报告记录，不能拿 existing）
            report.setTemplateId(source.getTemplateId());
            report.setTemplate(source.getTemplate());
        }
        report.setAnalysisId(snapshot.getAnalysisId());
        report.setAnalysisBatchDate(snapshot.getAnalysisBatchDate());
        report.setAnalysisDate(snapshot.getAnalysisDate());
        report.setSubbarcode(snapshot.getSubbarcode());
        report.setProductBio(snapshot.getProduct());
        report.setProductId(resolveProductId(snapshot.getProductId(), snapshot.getProduct()));
        // 录单癌种 + 项目编号：进入解读时就带上（LIMS sample_file，按样本编号取）
        SampleInfo lims = selectSample(snapshot.getSubbarcode());
        report.setCancerType(lims == null ? null : lims.getCancerType());
        // 项目编号是报告必填字段（用户 2026-10-22）：样本没维护项目号就不让建报告，避免出现空项目
        String projectNo = lims == null ? null : lims.getProjectNo();
        if (StringUtils.isBlank(projectNo)) {
            throw new ServiceException("样本缺少项目编号，请先在「样本信息」里补 project_no 再进入解读：subbarcode="
                + snapshot.getSubbarcode());
        }
        report.setProjectNo(projectNo.trim());
        report.setReportStatus(ReportStatus.BIOINFO_VIEW);
        analysisReportMapper.insert(report);

        AnalysisReportVo vo = analysisReportMapper.selectVoById(report.getReportId());
        if (vo == null) {
            throw new ServiceException("报告记录创建失败：analysisId=" + analysisId);
        }
        return vo;
    }

    @Override
    public InterpretationContextVo loadContext(Long reportId, Long analysisId) {
        if (reportId == null || analysisId == null) {
            throw new ServiceException("reportId 与 analysisId 不能为空");
        }
        AnalysisReportVo report = analysisReportMapper.selectVoById(reportId);
        if (report == null) {
            throw new ServiceException("报告不存在或已删除：reportId=" + reportId);
        }
        if (!analysisId.equals(report.getAnalysisId())) {
            throw new ServiceException("报告与分析批次不匹配：reportId=" + reportId + "，analysisId=" + analysisId);
        }

        InterpretationContextVo context = new InterpretationContextVo();
        context.setReport(report);
        context.setLims(loadLims(report.getSubbarcode(), context.getErrors(), context.getWarnings()));
        context.setCanGenerate(context.getErrors().isEmpty());
        return context;
    }

    @Override
    public TableDataInfo<InterpretationFileVo> selectFileList(Long analysisId, InterpretationFileQueryBo bo,
                                                              PageQuery pageQuery) {
        assertAnalysisExists(analysisId);
        Page<InterpretationFileVo> page =
            interpretationMapper.selectFilePage(pageQuery.build(), analysisId, bo);
        return TableDataInfo.build(page);
    }

    @Override
    public InterpretationFileContentVo queryFileContent(Long fileId, Long analysisId) {
        if (fileId == null || analysisId == null) {
            throw new ServiceException("fileId 与 analysisId 不能为空");
        }
        InterpretationFileContentVo content =
            interpretationMapper.selectFileContent(fileId, analysisId, MAX_FILE_TEXT_LENGTH);
        if (content == null) {
            throw new ServiceException("文件不存在或不属于该分析批次：fileId=" + fileId + "，analysisId=" + analysisId);
        }
        long textLength = content.getTextLength() == null ? 0L : content.getTextLength();
        content.setTruncated(textLength > MAX_FILE_TEXT_LENGTH);
        return content;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer finishBioinfoView(Long analysisId, Long reportId) {
        if (analysisId == null || reportId == null) {
            throw new ServiceException("analysisId 与 reportId 不能为空");
        }
        Map<String, Object> report = interpretationMapper.selectReportRow(reportId);
        if (report == null || !String.valueOf(analysisId).equals(String.valueOf(report.get("analysisId")))) {
            throw new ServiceException("报告不存在或不属于该分析批次：reportId=" + reportId
                + "，analysisId=" + analysisId);
        }
        int status = Integer.parseInt(String.valueOf(report.get("status")));
        if (status != ReportStatus.BIOINFO_VIEW) {
            throw new ServiceException("当前状态（" + ReportStatus.label(status) + "）不允许看点完成：reportId=" + reportId);
        }

        // 认领口径（2026-10-27 定）：点「处理」先占人；这里兜底再占一次（幂等），被他人占住则拒绝。
        if (interpretationMapper.claimBioinfoView(reportId, LoginHelper.getUserId()) == 0) {
            throw new ServiceException("该报告已由他人认领（生信看点），请刷新后查看：reportId=" + reportId);
        }
        int affected = interpretationMapper.updateBioinfoViewed(reportId, LoginHelper.getUserId());
        if (affected == 0) {
            throw new ServiceException("报告状态已变化，请刷新后重试：reportId=" + reportId);
        }
        log.info("生信看点完成：reportId={}，analysisId={}，看点人={}", reportId, analysisId, LoginHelper.getUserId());
        return ReportStatus.BIOINFO_REVIEW;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer finishBioinfoReview(Long analysisId, Long reportId) {
        if (analysisId == null || reportId == null) {
            throw new ServiceException("analysisId 与 reportId 不能为空");
        }
        Map<String, Object> report = interpretationMapper.selectReportRow(reportId);
        if (report == null || !String.valueOf(analysisId).equals(String.valueOf(report.get("analysisId")))) {
            throw new ServiceException("报告不存在或不属于该分析批次：reportId=" + reportId
                + "，analysisId=" + analysisId);
        }
        int status = Integer.parseInt(String.valueOf(report.get("status")));
        if (status != ReportStatus.BIOINFO_REVIEW) {
            throw new ServiceException("当前状态（" + ReportStatus.label(status) + "）不允许生信复核：reportId=" + reportId);
        }
        String viewer = toText(report.get("reportBioinfoViewedBy"));
        if (viewer != null && viewer.equals(String.valueOf(LoginHelper.getUserId()))) {
            throw new ServiceException("不能复核自己看点的报告：reportId=" + reportId);
        }

        // 认领口径（2026-10-27 定）：点「处理」先占人；这里兜底再占一次（幂等），被他人占住则拒绝。
        if (interpretationMapper.claimBioinfoReview(reportId, LoginHelper.getUserId()) == 0) {
            throw new ServiceException("该报告已由他人认领（生信复核），请刷新后查看：reportId=" + reportId);
        }
        int affected = interpretationMapper.updateBioinfoReviewed(reportId, LoginHelper.getUserId());
        if (affected == 0) {
            throw new ServiceException("报告状态已变化，请刷新后重试：reportId=" + reportId);
        }
        log.info("生信复核通过：reportId={}，analysisId={}，复核人={}", reportId, analysisId, LoginHelper.getUserId());
        return ReportStatus.PENDING_INTERPRET;
    }

    @Override
    public FileRedriveVo redriveFile(Long analysisId, Long fileId) {
        if (analysisId == null || fileId == null) {
            throw new ServiceException("analysisId 与 fileId 不能为空");
        }
        // 驱动口径全部在 ruoyi-drive：同批次重跑 → 老行 del_flag=1 + 新行新 file_id，解析与落库都由它负责
        DriveFileVo drive = driveService.driveFile(analysisId, fileId);
        log.info("重新驱动完成：旧fileId={}，新fileId={}，status={}，driveStatus={}",
            fileId, drive.getFileId(), drive.getStatus(), drive.getDriveStatus());

        FileRedriveVo vo = new FileRedriveVo();
        vo.setFileId(drive.getFileId());
        vo.setFileName(drive.getFileName());
        vo.setFileStatus(drive.getStatus());
        vo.setDriveStatus(drive.getDriveStatus());
        vo.setMessage(drive.getMessage());
        return vo;
    }

    @Override
    public TableDataInfo<Map<String, Object>> selectVariantList(Long analysisId, InterpretationVariantQueryBo bo,
                                                               PageQuery pageQuery) {
        assertAnalysisExists(analysisId);
        assertSourceType(bo.getSourceType());
        Page<Map<String, Object>> page = interpretationMapper.selectVariantPage(pageQuery.build(), analysisId, bo);
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVariantReportStatus(InterpretationVariantStatusBo bo) {
        assertAnalysisExists(bo.getAnalysisId());
        assertSourceType(bo.getSourceType());
        Integer isReported = bo.getIsReported();
        if (isReported == null || (isReported != 0 && isReported != 1)) {
            throw new ServiceException("入报告状态只能是 0（否）或 1（是）");
        }
        // UPDATE 里带了 analysis_id 归属校验：影响行数为 0 说明位点不存在或不属于该批次
        int affected = interpretationMapper.updateVariantReportStatus(bo.getSourceId(), bo.getSourceType(),
            bo.getAnalysisId(), isReported, bo.getFilteredRationale(), LoginHelper.getUserId());
        if (affected <= 0) {
            throw new ServiceException("位点不存在或不属于该分析批次：sourceId=" + bo.getSourceId()
                + "，sourceType=" + bo.getSourceType());
        }
    }

    /** 位点类型必须在白名单内，避免把 sourceType 当表名拼进 SQL */
    private void assertSourceType(String sourceType) {
        if (!VARIANT_SOURCE_TYPES.contains(sourceType)) {
            throw new ServiceException("不支持的位点类型：" + sourceType);
        }
    }

    /** 分析批次必须存在（列表/内容都以 analysisId 为范围，避免越权看别的批次文件） */
    private void assertAnalysisExists(Long analysisId) {
        if (analysisId == null) {
            throw new ServiceException("分析数据ID不能为空");
        }
        if (interpretationMapper.selectAnalysisSnapshot(analysisId) == null) {
            throw new ServiceException("分析数据不存在或已删除：analysisId=" + analysisId);
        }
    }

    /**
     * LIMS 信息：按 subbarcode = analysis_data.subbarcode 读 sample_file（本仓已把生产库录单样本表
     * myapp_webcrmsample 的 122 列全量落成这张表），再按设计书 5.2 的字段映射挑出报告需要的列。
     * <p>
     * 校验口径（设计书 5.2「关键校验」）：
     * <ul>
     *   <li>没有 LIMS 记录 → errors（阻止生成），并在页面上明说找不到哪个编号；</li>
     *   <li>patientName / cancerType / specimenType / testingProgram 缺失 → errors；</li>
     *   <li>其余非关键字段缺失 → 只进 warnings，且值保持 null（不替换成「/」或「-」）。</li>
     * </ul>
     */
    private InterpretationLimsVo loadLims(String subbarcode, List<String> errors, List<String> warnings) {
        InterpretationLimsVo lims = new InterpretationLimsVo();
        lims.setSubbarcode(subbarcode);
        if (StringUtils.isBlank(subbarcode)) {
            lims.setFound(false);
            errors.add("报告缺少样本编号（subbarcode），无法定位 LIMS 信息");
            return lims;
        }

        SampleInfo sample = selectSample(subbarcode);
        lims.setFound(sample != null);
        if (sample == null) {
            errors.add("未找到该样本编号的样本信息（sample_file.subbarcode）：" + subbarcode);
            return lims;
        }

        fillLims(lims, sample);
        validateLims(lims, errors, warnings);
        return lims;
    }

    /** sample_file → LIMS 字段映射（设计书 5.2 的字段对照表） */
    private void fillLims(InterpretationLimsVo lims, SampleInfo sample) {
        lims.setPatientId(sample.getPcode());
        lims.setPatientName(sample.getPatientName());
        lims.setGender(sample.getSex());
        lims.setBirthday(sample.getBirthDay());
        lims.setAge(sample.getAge());
        lims.setCancerType(sample.getCancerType());
        lims.setPathologicalType(sample.getPathologicalType());
        lims.setClinicalStage(sample.getClinicalStages());
        lims.setClinicalRemark(sample.getClinicalRemark());
        // 医院：源表里公司名一半在 customer_name、一半在 custom_desc（还有 corp_desc）
        lims.setHospitalName(firstNonBlank(sample.getCustomerName(), sample.getCustomDesc(), sample.getCorpDesc()));
        lims.setDoctorName(sample.getDoctorName());
        lims.setSpecimenType(sample.getSampleType());
        lims.setSpecimenQuantity(joinNonBlank(sample.getSpecimenNum(), sample.getUnit()));
        lims.setSampleSource(sample.getSampleSource());
        lims.setFromOrgan(sample.getFromOrgan());
        lims.setSampleCollectedAt(firstNonBlank(sample.getSampleTime(), sample.getCollectDate()));
        lims.setSampleReceivedAt(sample.getGetSpecDate());
        lims.setCommissionedAt(sample.getEnterDate());
        lims.setTestingProgram(sample.getErpTestName());
        lims.setSampleRemark(sample.getSampleRemark());
        lims.setLaboratoryName(sample.getLaboratoryName());
        lims.setReportReceiver(sample.getReportReceiver());
        lims.setEmailAddress(sample.getEmailAddress());
        lims.setPatientInfoEmail(sample.getPatientInfoEmail());
        lims.setDoctorEmail(sample.getAdmissionDoctorEmail());
    }

    /** 校验：必填缺失进 errors（阻止生成），非关键缺失进 warnings */
    private void validateLims(InterpretationLimsVo lims, List<String> errors, List<String> warnings) {
        require(lims.getPatientName(), "患者姓名", errors);
        require(lims.getCancerType(), "录单癌种", errors);
        require(lims.getSpecimenType(), "样本类型", errors);
        require(lims.getTestingProgram(), "录单产品", errors);

        warnOnMissing(lims::getGender, "性别", warnings);
        warnOnMissing(lims::getBirthday, "出生日期", warnings);
        warnOnMissing(lims::getAge, "年龄", warnings);
        warnOnMissing(lims::getHospitalName, "医院/送检单位", warnings);
        warnOnMissing(lims::getDoctorName, "送检医生", warnings);
        warnOnMissing(lims::getSpecimenQuantity, "样本量", warnings);
        warnOnMissing(lims::getSampleCollectedAt, "采样日期", warnings);
        warnOnMissing(lims::getSampleReceivedAt, "收样日期", warnings);
        warnOnMissing(lims::getCommissionedAt, "委托日期", warnings);
        warnOnMissing(lims::getReportReceiver, "报告接收人", warnings);
    }

    /** 按样本编号取一条样本信息；同编号多条时取最新一条（编号在租户内唯一，这里只是防御） */
    private SampleInfo selectSample(String subbarcode) {
        LambdaQueryWrapper<SampleInfo> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SampleInfo::getSubbarcode, subbarcode);
        wrapper.orderByDesc(SampleInfo::getId);
        wrapper.last("LIMIT 1");
        return sampleInfoMapper.selectOne(wrapper);
    }

    /** 报告产品ID：批次快照里为空时按产品名/编码解析 product_config（scanner 只写产品名，product_id 常为空） */
    private Long resolveProductId(Long productId, String productName) {
        if (productId != null) {
            return productId;
        }
        return StringUtils.isNotBlank(productName) ? reportTemplateMapper.selectProductIdByName(productName) : null;
    }

    /**
     * 补齐报告业务表的快照字段（cancer_type 录单癌种 / product_id 产品ID）。
     * <p>
     * 为空才填、不覆盖已有值：老报告（本功能上线前建的）点一次「解读」即可补齐，
     * 保证 analysis_report 作为报告业务表字段自洽。
     */
    private void ensureReportSnapshot(AnalysisReport existing) {
        boolean cancerBlank = StringUtils.isBlank(existing.getCancerType());
        boolean productBlank = existing.getProductId() == null;
        boolean projectBlank = StringUtils.isBlank(existing.getProjectNo());
        boolean batchBlank = StringUtils.isBlank(existing.getAnalysisBatchDate());
        if (!cancerBlank && !productBlank && !projectBlank && !batchBlank) {
            return;
        }
        AnalysisReport patch = new AnalysisReport();
        patch.setReportId(existing.getReportId());
        if (cancerBlank || projectBlank) {
            SampleInfo lims = selectSample(existing.getSubbarcode());
            if (lims != null) {
                if (cancerBlank) {
                    patch.setCancerType(lims.getCancerType());
                }
                if (projectBlank && StringUtils.isNotBlank(lims.getProjectNo())) {
                    patch.setProjectNo(lims.getProjectNo());
                }
            }
        }
        if (productBlank) {
            patch.setProductId(reportTemplateMapper.selectProductIdByName(existing.getProductBio()));
        }
        if (batchBlank) {
            AnalysisSnapshotVo snapshot = interpretationMapper.selectAnalysisSnapshot(existing.getAnalysisId());
            if (snapshot != null && StringUtils.isNotBlank(snapshot.getAnalysisBatchDate())) {
                patch.setAnalysisBatchDate(snapshot.getAnalysisBatchDate());
            }
        }
        if (patch.getCancerType() != null || patch.getProductId() != null
            || patch.getProjectNo() != null || patch.getAnalysisBatchDate() != null) {
            analysisReportMapper.updateById(patch);
        }
    }

    private void require(String value, String label, List<String> errors) {
        if (StringUtils.isBlank(value)) {
            errors.add("缺少必填的" + label);
        }
    }

    private void warnOnMissing(Supplier<String> getter, String label, List<String> warnings) {
        if (StringUtils.isBlank(getter.get())) {
            warnings.add("未填写" + label);
        }
    }

    /** 取第一个非空值（医院的三个候选列用）；全空时返回空 Optional，由调用方决定展示 */
    private String firstNonBlank(String... values) {
        return Stream.of(values).filter(StringUtils::isNotBlank).findFirst().orElse(null);
    }

    /** 对象转文本（查库回来的列可能是各种类型，null 保持 null） */
    private String toText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /** 拼接非空片段（样本量 = 数量 + 单位） */
    private String joinNonBlank(String... values) {
        String joined = Stream.of(values).filter(StringUtils::isNotBlank).collect(Collectors.joining());
        return joined.isEmpty() ? null : joined;
    }

    /**
     * 取该批次最近一条「未完成」的报告（一个批次一份报告，覆盖式重出）。
     * <p>
     * 只排除终态（62 报告发送成功）：已完成之后再点「解读」才允许新建第二条；其余状态
     * （生信看点/审核、待解读、解读中、生成失败、待审核、审核中、签名、待发送、发送失败）
     * 一律复用同一条记录。
     */
    private AnalysisReport selectReusableReport(Long analysisId) {
        LambdaQueryWrapper<AnalysisReport> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(AnalysisReport::getAnalysisId, analysisId);
        wrapper.orderByDesc(AnalysisReport::getReportId);
        wrapper.last("LIMIT 1");
        return analysisReportMapper.selectOne(wrapper);
    }
}
