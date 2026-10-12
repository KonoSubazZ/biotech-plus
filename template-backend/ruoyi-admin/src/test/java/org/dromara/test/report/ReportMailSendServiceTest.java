package org.dromara.test.report;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.domain.AnalysisReport;
import org.dromara.report.domain.AnalysisReportEmailLog;
import org.dromara.report.domain.ReportMailConfig;
import org.dromara.report.domain.bo.InterpretationSendBo;
import org.dromara.report.mapper.AnalysisReportEmailLogMapper;
import org.dromara.report.mapper.AnalysisReportMapper;
import org.dromara.report.mapper.InterpretationMapper;
import org.dromara.report.service.IReportMailConfigService;
import org.dromara.report.service.impl.ReportMailSender;
import org.dromara.report.service.impl.ReportFileService;
import org.dromara.report.service.impl.ReportMailSendService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 报告发送单测（不连库、不发信：Mapper / 配置服务 / 发信器全部 mock）。
 * <p>
 * 只验服务层口径，不碰任何真实业务数据：
 * <ul>
 *   <li>未审核必须拒绝（且不写审计日志）；</li>
 *   <li>发件邮箱按页面选的 configId 走（不传则用默认邮箱 —— 由配置服务决定）；</li>
 *   <li>发送成功 → 日志 SUCCESS + 报告置 SENT + 记发放人/时间；</li>
 *   <li>发送失败 → 日志 FAILED + 原因入库，且**不改**报告状态，异常继续往上抛。</li>
 * </ul>
 * 只跑本类：mvn -Plocal -DskipTests=false -Dtest=ReportMailSendServiceTest test
 *
 * @author <你的名字>
 */
@Tag("local")
@DisplayName("报告发送服务")
public class ReportMailSendServiceTest {

    private InterpretationMapper interpretationMapper;
    private AnalysisReportMapper analysisReportMapper;
    private AnalysisReportEmailLogMapper emailLogMapper;
    private ReportFileService reportFileService;
    private IReportMailConfigService mailConfigService;
    private ReportMailSender mailSender;
    private ReportMailSendService service;

    @BeforeEach
    void setUp() {
        // LambdaUpdateWrapper 需要实体的 TableInfo（正常由 Spring/MyBatis 启动时注册）；
        // 纯 Mockito 单测没有容器，这里手动注册一次，否则 getSqlSet() 会抛 can not find lambda cache
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
            AnalysisReportEmailLog.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
            AnalysisReport.class);

        interpretationMapper = Mockito.mock(InterpretationMapper.class);
        analysisReportMapper = Mockito.mock(AnalysisReportMapper.class);
        emailLogMapper = Mockito.mock(AnalysisReportEmailLogMapper.class);
        reportFileService = Mockito.mock(ReportFileService.class);
        mailConfigService = Mockito.mock(IReportMailConfigService.class);
        mailSender = Mockito.mock(ReportMailSender.class);
        service = new ReportMailSendService(interpretationMapper, analysisReportMapper, emailLogMapper,
            reportFileService, mailConfigService, mailSender);

        Mockito.when(mailConfigService.loadForSend(Mockito.any())).thenReturn(config());
    }

    /** 一条可用的发件邮箱配置（loadForSend 已把空列补过默认值，这里直接给全） */
    private ReportMailConfig config() {
        ReportMailConfig config = new ReportMailConfig();
        config.setId(1L);
        config.setEnabled("1");
        config.setSmtpHost("smtp.exmail.qq.com");
        config.setSmtpPort(465);
        config.setSslEnable("1");
        config.setFromEmail("novomedicine-report@novogene.com");
        config.setFromName("诺禾致源报告");
        config.setPassword("secret");
        return config;
    }

    private Map<String, Object> report(String status) {
        Map<String, Object> row = new HashMap<>();
        row.put("reportId", 4);
        row.put("analysisId", 1);
        row.put("subbarcode", "YKHS260031036-1A");
        row.put("status", status);
        return row;
    }

    private InterpretationSendBo bo() {
        InterpretationSendBo bo = new InterpretationSendBo();
        bo.setAnalysisId(1L);
        bo.setReportId(4L);
        bo.setTo("a@example.com;b@example.com");
        bo.setCc("");
        bo.setSubject("请查收诺禾致源的检测报告，姓名：张三-YKHS260031036-1A");
        bo.setContent("尊敬的客户：\r\n您好！");
        bo.setAttachReport(false);
        return bo;
    }

    @Test
    @DisplayName("未审核的报告必须拒绝，且不写审计日志、不发信")
    void notApprovedIsRejected() {
        Mockito.when(interpretationMapper.selectReportRow(4L)).thenReturn(report("PENDING_REVIEW"));

        ServiceException error = Assertions.assertThrows(ServiceException.class, () -> service.send(bo()));

        Assertions.assertEquals(400, error.getCode());
        Assertions.assertTrue(error.getMessage().contains("已审核"), error.getMessage());
        Mockito.verifyNoInteractions(emailLogMapper);
        Mockito.verifyNoInteractions(mailSender);
        Mockito.verifyNoInteractions(analysisReportMapper);
    }

    @Test
    @DisplayName("页面选了发件邮箱就按它的 configId 发")
    void usesSelectedSenderConfig() {
        Mockito.when(interpretationMapper.selectReportRow(4L)).thenReturn(report("APPROVED"));
        InterpretationSendBo bo = bo();
        bo.setConfigId(7L);

        service.send(bo);

        Mockito.verify(mailConfigService).loadForSend(7L);
        Mockito.verify(mailSender).send(Mockito.any(), Mockito.eq("a@example.com;b@example.com"), Mockito.any(),
            Mockito.anyString(), Mockito.anyString(), Mockito.any(File[].class));
    }

    @Test
    @DisplayName("发件邮箱配置有问题（未配置/停用/缺密码）时不写日志、不发信")
    void invalidSenderConfigIsRejectedBeforeLogging() {
        Mockito.when(interpretationMapper.selectReportRow(4L)).thenReturn(report("APPROVED"));
        Mockito.when(mailConfigService.loadForSend(Mockito.any()))
            .thenThrow(new ServiceException("发件邮箱未配置密码", 400));

        ServiceException error = Assertions.assertThrows(ServiceException.class, () -> service.send(bo()));

        Assertions.assertEquals(400, error.getCode());
        Mockito.verifyNoInteractions(emailLogMapper);
        Mockito.verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("发送成功：日志 SENDING→SUCCESS，报告置 SENT 并记发放人")
    void successMarksLogAndReportSent() {
        Mockito.when(interpretationMapper.selectReportRow(4L)).thenReturn(report("APPROVED"));
        Mockito.when(mailSender.send(
            Mockito.any(), Mockito.anyString(), Mockito.any(),
            Mockito.anyString(), Mockito.anyString(), Mockito.any(File[].class)
        )).thenReturn("<msg-id@exmail>");

        Map<String, Object> result = service.send(bo());

        Assertions.assertEquals(2, result.get("recipientCount"));
        Assertions.assertEquals("<msg-id@exmail>", result.get("messageId"));
        Assertions.assertEquals("novomedicine-report@novogene.com", result.get("fromEmail"));

        ArgumentCaptor<AnalysisReportEmailLog> inserted = ArgumentCaptor.forClass(AnalysisReportEmailLog.class);
        Mockito.verify(emailLogMapper).insert(inserted.capture());
        Assertions.assertEquals("SENDING", inserted.getValue().getStatus());
        Assertions.assertEquals("a@example.com;b@example.com", inserted.getValue().getRecipients());
        Assertions.assertEquals(2L, inserted.getValue().getRecipientCount());

        String logSet = captureSet(emailLogMapper, AnalysisReportEmailLog.class);
        Assertions.assertTrue(logSet.contains("SUCCESS"), logSet);
        Assertions.assertTrue(logSet.contains("provider_message_id"), logSet);

        String reportSet = captureSet(analysisReportMapper, AnalysisReport.class);
        Assertions.assertTrue(reportSet.contains("SENT"), reportSet);
        Assertions.assertTrue(reportSet.contains("report_sent_by"), reportSet);
        Assertions.assertTrue(reportSet.contains("report_sent_at"), reportSet);
    }

    @Test
    @DisplayName("发送失败：日志 FAILED + 原因入库，报告状态不变，异常继续抛")
    void sendFailureMarksLogFailed() {
        Mockito.when(interpretationMapper.selectReportRow(4L)).thenReturn(report("APPROVED"));
        Mockito.when(mailSender.send(
            Mockito.any(), Mockito.anyString(), Mockito.any(),
            Mockito.anyString(), Mockito.anyString(), Mockito.any(File[].class)
        )).thenThrow(new ServiceException("邮件发送失败：连接超时"));

        ServiceException error = Assertions.assertThrows(ServiceException.class, () -> service.send(bo()));
        Assertions.assertTrue(error.getMessage().contains("连接超时"), error.getMessage());

        String logSet = captureSet(emailLogMapper, AnalysisReportEmailLog.class);
        Assertions.assertTrue(logSet.contains("FAILED"), logSet);
        Assertions.assertTrue(logSet.contains("failure_reason"), logSet);
        Mockito.verify(analysisReportMapper, Mockito.never()).update(Mockito.any(), Mockito.any());
    }

    /** 抓 update 的 LambdaUpdateWrapper，把 SET 子句与参数值拼成字符串，便于断言「写了哪些列/什么值」 */
    @SuppressWarnings("unchecked")
    private <T> String captureSet(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper, Class<T> type) {
        ArgumentCaptor<Wrapper<T>> captor = ArgumentCaptor.forClass(Wrapper.class);
        Mockito.verify(mapper).update(Mockito.isNull(), captor.capture());
        LambdaUpdateWrapper<T> wrapper = (LambdaUpdateWrapper<T>) captor.getValue();
        return String.valueOf(wrapper.getSqlSet()) + " " + wrapper.getParamNameValuePairs().values();
    }
}
