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
import org.dromara.report.domain.vo.InterpretationFileVo;
import org.dromara.report.domain.vo.InterpretationLimsVo;
import org.dromara.report.domain.vo.InterpretationRowVo;
import org.dromara.report.domain.vo.InterpretationVariantVo;
import org.dromara.report.mapper.AnalysisReportMapper;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.mapper.SampleInfoMapper;
import org.dromara.report.service.IInterpretationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 报告解读 业务层处理
 * <p>
 * 本页承担「列表 + 进入解读 + 解读上下文（LIMS）」：
 * <ul>
 *   <li>列表 = analysis_data ⟕ 该批次最新一份 analysis_report（一个批次可有多份报告，列表只显示最新那份的状态）。</li>
 *   <li>进入解读 = 复用该批次仍在 INTERPRETING 的报告；没有才新建，样本/产品信息从 analysis_data 复制。</li>
 *   <li>上下文 = 报告头 + LIMS 信息（读 sample_file，按 barcode = subbarcode）+ 生成前校验结论。</li>
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

    /** 单个文件内容最多返回的字符数（超大文件在库里 LEFT() 截断，避免整个 LONGTEXT 进内存/浏览器） */
    private static final int MAX_FILE_TEXT_LENGTH = 200_000;

    /** 位点类型白名单：与 history_somatic.source_type 的取值口径一致 */
    private static final Set<String> VARIANT_SOURCE_TYPES = Set.of("SNP_INDEL", "CNV", "FUSION", "CR_ALL");

    private final InterpretationMapper interpretationMapper;
    private final AnalysisReportMapper analysisReportMapper;
    private final SampleInfoMapper sampleInfoMapper;

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
    public TableDataInfo<InterpretationVariantVo> selectVariantList(Long analysisId, InterpretationVariantQueryBo bo,
                                                                   PageQuery pageQuery) {
        assertAnalysisExists(analysisId);
        assertSourceType(bo.getSourceType());
        Page<InterpretationVariantVo> page =
            interpretationMapper.selectVariantPage(pageQuery.build(), analysisId, bo);
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
     * LIMS 信息：按 barcode = analysis_data.subbarcode 读 sample_file（本仓已把生产库录单样本表
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
        lims.setBarcode(subbarcode);
        if (StringUtils.isBlank(subbarcode)) {
            lims.setFound(false);
            errors.add("报告缺少样本编号（subbarcode），无法定位 LIMS 信息");
            return lims;
        }

        SampleInfo sample = selectSample(subbarcode);
        lims.setFound(sample != null);
        if (sample == null) {
            errors.add("未找到该样本编号的样本信息（sample_file.barcode）：" + subbarcode);
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
        wrapper.eq(SampleInfo::getBarcode, subbarcode);
        wrapper.orderByDesc(SampleInfo::getId);
        wrapper.last("LIMIT 1");
        return sampleInfoMapper.selectOne(wrapper);
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

    /** 拼接非空片段（样本量 = 数量 + 单位） */
    private String joinNonBlank(String... values) {
        String joined = Stream.of(values).filter(StringUtils::isNotBlank).collect(Collectors.joining());
        return joined.isEmpty() ? null : joined;
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
