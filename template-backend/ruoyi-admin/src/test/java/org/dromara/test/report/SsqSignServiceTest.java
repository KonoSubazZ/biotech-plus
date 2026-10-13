package org.dromara.test.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.config.SsqSignProperties;
import org.dromara.report.service.impl.SsqSignServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 上上签送签通道单测：本地桩网关，验证三个场景的报文/坐标、token 缓存与 401 重试。
 * 只跑本类：mvn -Plocal -DskipTests=false -Dtest=SsqSignServiceTest test
 *
 * @author <你的名字>
 */
@Tag("local")
@DisplayName("上上签送签通道")
class SsqSignServiceTest {

    private static final String TOKEN_PATH = "/api/oauth/oauth/token";
    private static final String LOCATE_PATH = "TG9jYXRlQnlNdWx0aUtleXdvcmRz";
    private static final String SEND_PATH = "U2VuZENvbnRyYWN0c1N5bmM=";
    private static final double EPSILON = 1e-9;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private StubHttpGateway gateway;

    private SsqSignServiceImpl service;

    private Path pdf;

    @BeforeEach
    void setUp() throws IOException {
        gateway = new StubHttpGateway();
        gateway.respond(TOKEN_PATH, "{\"access_token\":\"token-1\",\"token_type\":\"bearer\",\"expires_in\":2833}");
        gateway.respond(LOCATE_PATH, "{\"data\":{"
            + "\"报告解读人：\":[{\"x\":0.10,\"y\":0.20,\"pageNumber\":3}],"
            + "\"报告复核人：\":[{\"x\":0.11,\"y\":0.21,\"pageNumber\":3}],"
            + "\"审批人：\":[{\"x\":0.12,\"y\":0.22,\"pageNumber\":3}],"
            + "\"反馈人\":[{\"x\":0.30,\"y\":0.40,\"pageNumber\":1}],"
            + "\"复核人\":[{\"x\":0.31,\"y\":0.41,\"pageNumber\":1}],"
            + "\"反馈时间\":[{\"x\":0.32,\"y\":0.42,\"pageNumber\":1}],"
            + "\"复核时间\":[{\"x\":0.33,\"y\":0.43,\"pageNumber\":1}]}}");
        gateway.respond(SEND_PATH, "{\"code\":\"0\",\"data\":{\"contractId\":\"4209652712486947847\"}}");

        SsqSignProperties properties = new SsqSignProperties();
        properties.setBaseUrl(gateway.baseUrl());
        // properties 没有 Java 默认值（值都在 yml），测试里显式给齐，别让它靠 Spring 才生效
        properties.setClientId("test-client");
        properties.setClientSecret("test-secret");
        properties.setTemplateId("4197207713522448386");
        service = new SsqSignServiceImpl(properties, objectMapper);

        pdf = Files.createTempFile("ssq-sign-", ".pdf");
        Files.writeString(pdf, "%PDF-1.4\n%%EOF", StandardCharsets.US_ASCII);
    }

    @AfterEach
    void tearDown() throws IOException {
        gateway.close();
        Files.deleteIfExists(pdf);
    }

    @Test
    @DisplayName("报告交付：三个签署位 + 企业章（压审批人坐标），日期偏移 0.035/-0.03")
    void reportDelivery() throws IOException {
        String contractId = service.sendReportDelivery(pdf.toString());

        Assertions.assertEquals("4209652712486947847", contractId);
        JsonNode payload = sentPayload();
        Assertions.assertEquals("4197207713522448386", payload.path("templateId").asText());
        Assertions.assertEquals("true", payload.path("signOrderly").asText());
        Assertions.assertEquals("SEND", payload.path("sendAction").asText());
        Assertions.assertEquals(List.of("报告解读人", "报告复核人", "审批人", "本企业"), roleNames(payload));
        JsonNode labels = payload.path("documents").get(0).path("appendingSignLabels");
        Assertions.assertEquals(7, labels.size(), "1 印章 + 3×(签名+日期)");
        JsonNode seal = labels.get(0);
        Assertions.assertEquals("SEAL", seal.path("type").asText());
        Assertions.assertEquals("本企业", seal.path("roleName").asText());
        Assertions.assertEquals(0.12 - 0.1, seal.path("x").asDouble(), EPSILON);
        Assertions.assertEquals(0.22 - 0.1, seal.path("y").asDouble(), EPSILON);
        JsonNode interpreterSign = labels.get(1);
        Assertions.assertEquals("SIGNATURE", interpreterSign.path("type").asText());
        Assertions.assertEquals(0.10, interpreterSign.path("x").asDouble(), EPSILON);
        JsonNode interpreterDate = labels.get(2);
        Assertions.assertEquals("DATE", interpreterDate.path("type").asText());
        Assertions.assertEquals("yyyy-MM-dd", interpreterDate.path("dateFormat").asText());
        Assertions.assertEquals(0.10 + 0.035, interpreterDate.path("x").asDouble(), EPSILON);
        Assertions.assertEquals(0.20 - 0.03, interpreterDate.path("y").asDouble(), EPSILON);
    }

    @Test
    @DisplayName("报告延迟：反馈人/复核人 签名偏移 0.125/-0.025，日期 0.17/-0.01，企业章压反馈时间")
    void reportDelay() throws IOException {
        service.sendReportDelay(pdf.toString());

        JsonNode payload = sentPayload();
        Assertions.assertEquals(List.of("反馈人", "复核人", "本企业"), roleNames(payload));
        JsonNode labels = payload.path("documents").get(0).path("appendingSignLabels");
        Assertions.assertEquals(5, labels.size(), "1 印章 + 2×(签名+日期)");
        Assertions.assertEquals(0.32 + 0.1, labels.get(0).path("x").asDouble(), EPSILON);
        Assertions.assertEquals(0.30 + 0.125, labels.get(1).path("x").asDouble(), EPSILON);
        Assertions.assertEquals(0.40 - 0.025, labels.get(1).path("y").asDouble(), EPSILON);
        Assertions.assertEquals(0.32 + 0.17, labels.get(2).path("x").asDouble(), EPSILON);
        Assertions.assertEquals(0.31 + 0.125, labels.get(3).path("x").asDouble(), EPSILON);
        Assertions.assertEquals(0.33 + 0.17, labels.get(4).path("x").asDouble(), EPSILON);
    }

    @Test
    @DisplayName("质控不合格：签名偏移 0.145（与延迟不同），企业章自动签")
    void qcUnqualified() throws IOException {
        service.sendQcUnqualified(pdf.toString());

        JsonNode payload = sentPayload();
        JsonNode enterprise = payload.path("roles").get(2);
        Assertions.assertEquals("AUTO", enterprise.path("signMode").asText());
        Assertions.assertTrue(enterprise.path("autoSign").asBoolean());
        Assertions.assertEquals("NONE", enterprise.path("noticeConfig").path("verifyMethod").asText());
        JsonNode labels = payload.path("documents").get(0).path("appendingSignLabels");
        Assertions.assertEquals(0.30 + 0.145, labels.get(1).path("x").asDouble(), EPSILON);
        Assertions.assertEquals(0.31 + 0.145, labels.get(3).path("x").asDouble(), EPSILON);
    }

    @Test
    @DisplayName("token 缓存：连续两次送签只换一次 token")
    void tokenCached() {
        service.sendReportDelay(pdf.toString());
        service.sendQcUnqualified(pdf.toString());

        Assertions.assertEquals(1, gateway.count(TOKEN_PATH));
    }

    @Test
    @DisplayName("token 失效（HTTP 401）：自动换新重试一次并成功")
    void retryOnTokenExpired() throws IOException {
        gateway.respond(TOKEN_PATH, "{\"access_token\":\"token-2\",\"expires_in\":2833}");
        gateway.respond(SEND_PATH, request -> gateway.count(SEND_PATH) == 1
            ? "{\"__status401__\"}"
            : "{\"code\":\"0\",\"data\":{\"contractId\":\"777\"}}");

        String contractId = service.sendReportDelay(pdf.toString());

        Assertions.assertEquals("777", contractId);
        Assertions.assertEquals(2, gateway.count(TOKEN_PATH), "第一次 token + 换新一次");
    }

    @Test
    @DisplayName("关键字定位不到：抛异常，不拿空坐标去签")
    void missingKeyword() throws IOException {
        gateway.respond(LOCATE_PATH, "{\"data\":{\"报告解读人：\":[{\"x\":0.1,\"y\":0.2,\"pageNumber\":1}]}}");

        ServiceException error = Assertions.assertThrows(ServiceException.class,
            () -> service.sendReportDelivery(pdf.toString()));
        Assertions.assertTrue(error.getMessage().contains("报告复核人："), error.getMessage());
    }

    @Test
    @DisplayName("非 2xx：抛异常并带上状态码")
    void httpError() {
        gateway.respond(TOKEN_PATH, "{\"error\":\"bad_client\"}");

        ServiceException error = Assertions.assertThrows(ServiceException.class,
            () -> service.sendReportDelivery(pdf.toString()));
        Assertions.assertTrue(error.getMessage().contains("token"), error.getMessage());
    }

    @Test
    @DisplayName("非 PDF 文件：拒发")
    void rejectNonPdf() throws IOException {
        Path docx = Files.createTempFile("ssq-sign-", ".docx");
        Files.writeString(docx, "PK\u0003\u0004", StandardCharsets.US_ASCII);
        try {
            ServiceException error = Assertions.assertThrows(ServiceException.class,
                () -> service.sendReportDelivery(docx.toString()));
            Assertions.assertTrue(error.getMessage().contains("%PDF-"), error.getMessage());
        } finally {
            Files.deleteIfExists(docx);
        }
    }

    @Test
    @DisplayName("签署账号口径：个人角色=18034334041/刘尚智；企业角色只带操作账号+企业名，不带 userName")
    void signerAccounts() throws IOException {
        service.sendReportDelivery(pdf.toString());

        JsonNode roles = sentPayload().path("roles");
        // 报告交付：routeOrder 1/3 = 刘尚智，2 = 罗阔
        Assertions.assertEquals("18034334041", roles.get(0).path("userInfo").path("userAccount").asText());
        Assertions.assertEquals("刘尚智", roles.get(0).path("userInfo").path("userName").asText());
        Assertions.assertEquals("18610464338", roles.get(1).path("userInfo").path("userAccount").asText());
        Assertions.assertEquals("罗阔", roles.get(1).path("userInfo").path("userName").asText());
        Assertions.assertEquals("18034334041", roles.get(2).path("userInfo").path("userAccount").asText());
        Assertions.assertEquals("刘尚智", roles.get(2).path("userInfo").path("userName").asText());

        JsonNode enterprise = roles.get(3).path("userInfo");
        Assertions.assertEquals("18610464338", enterprise.path("userAccount").asText());
        Assertions.assertEquals("天津诺禾医学检验所有限公司", enterprise.path("enterpriseName").asText());
        Assertions.assertTrue(enterprise.path("userName").isMissingNode(),
            "企业角色不传 userName —— 上上签后台显示的名字是它按账号解析出来的");

        for (JsonNode role : roles) {
            System.out.println("[报文] roleName=" + role.path("roleName").asText()
                + " userType=" + role.path("userType").asText()
                + " userInfo=" + role.path("userInfo"));
        }
    }

    @Test
    @DisplayName("质控不合格：反馈人=刘尚智/18034334041，复核人=罗阔/18610464338")
    void qcSignerAccounts() throws IOException {
        service.sendQcUnqualified(pdf.toString());

        JsonNode roles = sentPayload().path("roles");
        Assertions.assertEquals("刘尚智", roles.get(0).path("userInfo").path("userName").asText());
        Assertions.assertEquals("18034334041", roles.get(0).path("userInfo").path("userAccount").asText());
        Assertions.assertEquals("罗阔", roles.get(1).path("userInfo").path("userName").asText());
        Assertions.assertEquals("18610464338", roles.get(1).path("userInfo").path("userAccount").asText());
    }

    @Test
    @DisplayName("查询合同状态：走 GET，返回网关给的状态文案")
    void queryStatus() throws IOException {
        gateway.respond("best-signquery-contract-info/", "{\"code\":\"0\",\"data\":{\"status\":\"SIGNING\"}}");
        Assertions.assertEquals("SIGNING", service.queryContractStatus("123"));
    }

    @Test
    @DisplayName("下载签名件：落盘成 PDF（带 %PDF- 头），文件名 = fileBase.pdf")
    void downloadSigned() throws IOException {
        gateway.respondBytes("RG93bmxvYWRGaWxl", "%PDF-1.4\n%%EOF".getBytes(StandardCharsets.US_ASCII));
        Path dir = Files.createTempDirectory("ssq-signed-");
        Path signed = service.downloadSignedPdf("123", dir, "qc-failed-notify-S1-20261013120000");
        Assertions.assertEquals("qc-failed-notify-S1-20261013120000.pdf", signed.getFileName().toString());
        Assertions.assertTrue(Files.readAllBytes(signed).length > 0);
    }

    @Test
    @DisplayName("下载到的不是 PDF：抛错（不做假成功）")
    void downloadNotPdf() {
        gateway.respondBytes("RG93bmxvYWRGaWxl", "PK\u0003\u0004zip".getBytes(StandardCharsets.US_ASCII));
        Path dir = Path.of(System.getProperty("java.io.tmpdir"));
        ServiceException error = Assertions.assertThrows(ServiceException.class,
            () -> service.downloadSignedPdf("123", dir, "x"));
        Assertions.assertTrue(error.getMessage().contains("%PDF-"), error.getMessage());
    }

    /** 取最后一次 SendContractsSync 的请求体 */
    private JsonNode sentPayload() throws IOException {
        String body = gateway.received().stream()
            .filter(record -> record.path().contains(SEND_PATH))
            .reduce((first, second) -> second)
            .orElseThrow(() -> new AssertionError("没有发出送签请求"))
            .body();
        return objectMapper.readTree(body);
    }

    private List<String> roleNames(JsonNode payload) {
        return StreamSupport.stream(payload.path("roles").spliterator(), false)
            .map(role -> role.path("roleName").asText())
            .collect(Collectors.toList());
    }
}
