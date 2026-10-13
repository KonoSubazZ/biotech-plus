package org.dromara.report.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.config.SsqSignProperties;
import org.dromara.report.service.ISsqSignService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 上上签送签通道实现（三个场景：报告延迟 / 报告交付 / 质控不合格）。
 * <p>
 * 参考 {@code ~/project/ssq_api.py}，链路与参数逐字段对齐（对照表见
 * {@code docs/context/report/report-sign-ssq.md}）。关键取舍：
 * <ol>
 *   <li>接口地址末段是 {@code Base64("HZERO:BESTSIGN:" + 接口名)}（实测解码确认），代码里现算，不抄魔法串；</li>
 *   <li>Token 按 {@code expires_in} 缓存（提前 60 秒失效），并处理网关两种「token 失效」表达：
 *       HTTP 401 与 {@code code=error.permission.accessTokenInvalid}，各换一次新 token 重试一次；</li>
 *   <li>送签前校验文件是 PDF（传错 DOCX 是最常见的调用错误），不存在/非 PDF 直接抛；</li>
 *   <li>关键字定位不到坐标直接抛——位置错了会把章盖到别的行上，宁可不签；</li>
 *   <li>HTTP 用 JDK HttpClient（HTTP/1.1），与 {@code OnlyOfficePdfConverter} 同口径，不引第三方依赖。</li>
 * </ol>
 *
 * @author <你的名字>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SsqSignServiceImpl implements ISsqSignService {

    /** 网关调用前缀 */
    private static final String INVOKE_PREFIX = "/api/hitf/v2p/rest/invoke/";

    /** 网关接口名固定前缀（Base64 原文） */
    private static final String GATEWAY_PREFIX = "HZERO:BESTSIGN:";

    /** 换 token 的地址 */
    private static final String TOKEN_PATH = "/api/oauth/oauth/token";

    /** 定位关键字拿坐标 */
    private static final String API_LOCATE = "LocateByMultiKeywords";

    /** 送签 */
    private static final String API_SEND = "SendContractsSync";

    /** 查询合同状态（GET，路径带合同号） */
    private static final String QUERY_CONTRACT_PATH = "/api/hitf/v2p/rest/invoke/best-signquery-contract-info/";

    /** 下载签名件（POST） */
    private static final String API_DOWNLOAD = "DownloadFile";

    /** 网关表达「token 失效」的业务码 */
    private static final String TOKEN_INVALID_CODE = "error.permission.accessTokenInvalid";

    /** 提前 60 秒换 token，避免边界上撞失效 */
    private static final long TOKEN_REFRESH_AHEAD_MILLIS = 60_000L;

    /**
     * 个人角色签署人：按 {@code routeOrder} 固定（2026-10-13 口径，前期先固定参数跑测试）。
     * 1 = 报告解读人 / 反馈人 → 刘尚智；2 = 报告复核人 / 复核人 → 罗阔；3 = 审批人（报告交付才有）→ 刘尚智。
     * 后期接业务数据时，把这张表换成「按报告/质控记录里的人工角色取账号」即可（见 plan §1-9）。
     */
    private static final Map<String, Signer> PERSON_SIGNERS = Map.of(
        "1", new Signer("18034334041", "刘尚智"),
        "2", new Signer("18610464338", "罗阔"),
        "3", new Signer("18034334041", "刘尚智"));

    /** 企业盖章的操作账号（该角色 needNotice=false，不传 userName） */
    private static final String ENTERPRISE_OPERATOR_ACCOUNT = "18610464338";

    private static final String ENTERPRISE_NAME = "天津诺禾医学检验所有限公司";

    /** 一个个人签署人 */
    private record Signer(String account, String userName) {
    }

    /** 报告交付的定位关键字（文档里带全角冒号） */
    private static final List<String> DELIVERY_KEYWORDS = List.of("报告解读人：", "报告复核人：", "审批人：");

    /** 报告延迟与质控不合格共用一组定位关键字 */
    private static final List<String> NOTIFY_KEYWORDS = List.of("反馈人", "复核人", "反馈时间", "复核时间");

    private final SsqSignProperties properties;

    private final ObjectMapper objectMapper;

    /** token 缓存；读 volatile，刷新 synchronized */
    private volatile CachedToken token;

    /** 显式 HTTP/1.1：与 OnlyOfficePdfConverter 同口径（明文网关不认 h2c） */
    private final HttpClient httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    @Override
    public String sendReportDelay(String pdfPath) {
        Path pdf = requirePdf(pdfPath);
        Map<String, JsonNode> positions = locate(pdf, NOTIFY_KEYWORDS);
        return contractId(submit("报告延迟", pdf, delayPayload(pdf, positions)));
    }

    @Override
    public String sendReportDelivery(String pdfPath) {
        Path pdf = requirePdf(pdfPath);
        Map<String, JsonNode> positions = locate(pdf, DELIVERY_KEYWORDS);
        return contractId(submit("报告交付", pdf, deliveryPayload(pdf, positions)));
    }

    @Override
    public String sendQcUnqualified(String pdfPath) {
        Path pdf = requirePdf(pdfPath);
        Map<String, JsonNode> positions = locate(pdf, NOTIFY_KEYWORDS);
        return contractId(submit("质控不合格", pdf, qcPayload(pdf, positions)));
    }

    @Override
    public String queryContractStatus(String contractId) {
        if (contractId == null || contractId.isBlank()) {
            throw new ServiceException("合同号为空，无法查询签署状态");
        }
        JsonNode response = invokeGet(QUERY_CONTRACT_PATH + contractId, "查询合同状态");
        return contractStatus(response);
    }

    /**
     * 从查询响应里取状态描述。
     * <p>
     * UAT 实测（2026-10-13，合同 4220532292047296513，响应样例见
     * {@code docs/context/report/report-sign-ssq.md} §8.4）：字段是 {@code data.status}，
     * 取值 {@code SENT}（已派发待签）/ {@code COMPLETE}（签署完成）。同响应里还有
     * {@code sendTime} / {@code finishTime} / {@code signers[].status} 等可查签署明细。
     * 取不到就报错：网关换了结构必须立刻看得见，不能当「未完成」悄悄过去。
     */
    private String contractStatus(JsonNode response) {
        String status = response.path("data").path("status").asText("");
        if (status.isBlank()) {
            throw new ServiceException("上上签查询未返回状态（data.status），原始响应："
                + abbreviate(response.toString()));
        }
        return status;
    }

    @Override
    public Path downloadSignedPdf(String contractId, Path targetDir, String fileBase) {
        if (contractId == null || contractId.isBlank()) {
            throw new ServiceException("合同号为空，无法下载签名件");
        }
        checkCredentials();
        String url = baseUrl() + INVOKE_PREFIX + gatewayName(API_DOWNLOAD);
        Map<String, Object> body = new LinkedHashMap<>();
        // operator 文档示例是空账号 + 空企业名（用 token 的 client 身份下载）
        body.put("operator", Map.of("account", "", "enterpriseName", ""));
        body.put("contractIds", List.of(contractId));
        body.put("fileType", "pdf");
        byte[] content = postForBytes(url, toJson(body), accessToken());
        if (content.length == 0) {
            throw new ServiceException("上上签签名件下载为空：contractId=" + contractId + "（可能还没签完）");
        }
        if (content[0] == '{') {
            // 网关用 JSON 报错（例如合同未完成 / 无权限）时这里拿到的是文本，原样带出来
            throw new ServiceException("上上签签名件下载失败：" + abbreviate(new String(content, StandardCharsets.UTF_8)));
        }
        if (!isPdf(content)) {
            throw new ServiceException("下载的不是 PDF（缺少 %PDF- 文件头）：contractId=" + contractId
                + "，前 8 字节=" + abbreviate(new String(content, 0, Math.min(8, content.length), StandardCharsets.US_ASCII)));
        }
        Path target = targetDir.resolve(fileBase + ".pdf");
        try {
            Files.createDirectories(targetDir);
            Path temp = Files.createTempFile(targetDir, "." + fileBase, ".tmp");
            Files.write(temp, content);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new ServiceException("写签名件失败：" + target + "，" + e.getMessage(), e);
        }
        log.info("上上签签名件已落盘：{}（{} 字节）", target, content.length);
        return target;
    }

    /** 送签并返回原始响应（日志里记场景、文件、耗时） */
    private JsonNode submit(String scene, Path pdf, Map<String, Object> payload) {
        long startedAt = System.currentTimeMillis();
        JsonNode response = invoke(API_SEND, payload);
        // 原始响应进日志：合同号字段名在文档里没有示例，靠这条日志确认（见 docs/context/report/report-sign-ssq.md）
        log.info("上上签送签原始响应（场景={}）：{}", scene, abbreviate(response.toString()));
        log.info("上上签送签完成：场景={}，文件={}，耗时 {} 毫秒",
            scene, pdf.getFileName(), System.currentTimeMillis() - startedAt);
        return response;
    }

    /**
     * 合同号。
     * <p>
     * 响应形状 UAT 实测（2026-10-13，两个场景一致）：
     * {@code {"code":"0","message":"请求成功","data":{"contractId":"4220441872676125698"}}}。
     * 取不到就抛错并回显原文——合同号是调用方唯一的回执，不允许糊过去。
     */
    private String contractId(JsonNode response) {
        String contractId = response.path("data").path("contractId").asText("");
        if (contractId.isBlank()) {
            throw new ServiceException("上上签未返回合同号（data.contractId），原始响应：" + abbreviate(response.toString()));
        }
        return contractId;
    }

    /** 调网关接口：POST JSON；token 失效换一次新 token 重试一次 */
    private JsonNode invoke(String apiName, Map<String, Object> payload) {
        checkCredentials();
        String url = baseUrl() + INVOKE_PREFIX + gatewayName(apiName);
        String body = toJson(payload);
        GatewayResponse response = exchange(url, body, accessToken());
        if (tokenExpired(response)) {
            log.info("上上签 token 失效，换新后重试：{}", apiName);
            // 强制换：网关说失效就以网关为准（本地缓存可能还没到点）
            response = exchange(url, body, refreshToken(true));
        }
        return requireSuccess(apiName, response);
    }

    /** 地址末段 = Base64("HZERO:BESTSIGN:" + 接口名) */
    private String gatewayName(String apiName) {
        return Base64.getEncoder().encodeToString((GATEWAY_PREFIX + apiName).getBytes(StandardCharsets.UTF_8));
    }

    /** 调网关 GET 接口（查询类）：token 失效换一次新 token 重试一次 */
    private JsonNode invokeGet(String path, String apiName) {
        checkCredentials();
        String url = baseUrl() + path;
        GatewayResponse response = exchangeGet(url, accessToken());
        if (tokenExpired(response)) {
            log.info("上上签 token 失效，换新后重试：{}", apiName);
            response = exchangeGet(url, refreshToken(true));
        }
        return requireSuccess(apiName, response);
    }

    /** 发一次 GET（与 exchange 同构） */
    private GatewayResponse exchangeGet(String url, String token) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .header("Authorization", "Bearer " + token)
            .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .GET()
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            // 查询接口不截断：状态字段/取值要能一眼看到（2026-10-13 口径；送签类响应仍走 abbreviate）
            log.info("上上签查询原始响应（{}）：{}", url, response.body());
            return new GatewayResponse(response.statusCode(), parse(response.body()), response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("上上签请求被中断：" + url, e);
        } catch (IOException e) {
            throw new ServiceException("访问上上签网关失败（" + url + "）：" + e.getMessage(), e);
        }
    }

    /** 发一次请求（含状态码与响应原文，便于判 401 与报错时回显） */
    private GatewayResponse exchange(String url, String body, String token) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + token)
            .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new GatewayResponse(response.statusCode(), parse(response.body()), response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("上上签请求被中断：" + url, e);
        } catch (IOException e) {
            throw new ServiceException("访问上上签网关失败（" + url + "）：" + e.getMessage(), e);
        }
    }

    /** token 失效的两种表达：HTTP 401 或业务码 accessTokenInvalid */
    private boolean tokenExpired(GatewayResponse response) {
        if (response.status() == 401) {
            return true;
        }
        JsonNode body = response.body();
        return body != null && TOKEN_INVALID_CODE.equals(body.path("code").asText(""));
    }

    /** 发一次请求并要二进制体（下载用；token 失效同样强制换一次重试） */
    private byte[] postForBytes(String url, String body, String token) {
        byte[] content = exchangeBytes(url, body, token);
        if (content == null) {
            content = exchangeBytes(url, body, refreshToken(true));
        }
        return content;
    }

    /** 返回 null 表示 token 失效（上层换新重试），其余错误直接抛 */
    private byte[] exchangeBytes(String url, String body, String token) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + token)
            .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (tokenExpired(response.statusCode(), response.body())) {
                return null;
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ServiceException("上上签接口失败（HTTP " + response.statusCode() + "）：下载签名件，响应："
                    + abbreviate(new String(response.body(), StandardCharsets.UTF_8)));
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("上上签请求被中断：" + url, e);
        } catch (IOException e) {
            throw new ServiceException("访问上上签网关失败（" + url + "）：" + e.getMessage(), e);
        }
    }

    /** 判 token 失效（二进制响应版：状态码或错误 JSON 里的业务码） */
    private boolean tokenExpired(int status, byte[] body) {
        if (status == 401) {
            return true;
        }
        if (body == null || body.length == 0 || body[0] != '{') {
            return false;
        }
        JsonNode json = parse(new String(body, StandardCharsets.UTF_8));
        return json != null && TOKEN_INVALID_CODE.equals(json.path("code").asText(""));
    }

    /** PDF 头校验（%PDF-） */
    private boolean isPdf(byte[] content) {
        byte[] header = "%PDF-".getBytes(StandardCharsets.US_ASCII);
        if (content.length < header.length) {
            return false;
        }
        for (int i = 0; i < header.length; i++) {
            if (content[i] != header[i]) {
                return false;
            }
        }
        return true;
    }

    /** 成功判定：HTTP 2xx，且响应 code 不是 error.*（文档未给错误码表，先按这条保守判） */
    private JsonNode requireSuccess(String apiName, GatewayResponse response) {
        if (response.status() < 200 || response.status() >= 300) {
            throw new ServiceException("上上签接口失败（HTTP " + response.status() + "）：" + apiName
                + "，响应：" + abbreviate(response.raw()));
        }
        JsonNode body = response.body();
        if (body == null) {
            throw new ServiceException("上上签接口返回不是 JSON：" + apiName + "，响应：" + abbreviate(response.raw()));
        }
        String code = body.path("code").asText("");
        if (code.startsWith("error.")) {
            throw new ServiceException("上上签接口报错：" + apiName + "，code=" + code
                + "，响应：" + abbreviate(response.raw()));
        }
        return body;
    }

    /** 取可用 token：缓存没过期直接用，否则换新的 */
    private String accessToken() {
        CachedToken current = token;
        if (current != null && current.expiresAtMillis() > System.currentTimeMillis()) {
            return current.value();
        }
        return refreshToken(false);
    }

    /**
     * 换新 token（synchronized，避免并发重复取）。
     *
     * @param force true = 无视本地缓存有效期强制换。网关判 token 失效（401 / {@code accessTokenInvalid}）时必须走这条：
     *              本地缓存是「按 expires_in 估算」的，网关提前吊销、两边时钟有偏差时缓存还在有效期内，
     *              不强制换就会拿同一个已失效 token 再发一次（白重试一次、报错信息也误导）。
     */
    private synchronized String refreshToken(boolean force) {
        CachedToken current = token;
        if (!force && current != null && current.expiresAtMillis() > System.currentTimeMillis()) {
            return current.value();
        }
        String url = baseUrl() + TOKEN_PATH;
        String form = "grant_type=client_credentials"
            + "&client_id=" + URLEncoder.encode(properties.getClientId(), StandardCharsets.UTF_8)
            + "&client_secret=" + URLEncoder.encode(properties.getClientSecret(), StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode body = parse(response.body());
            if (response.statusCode() != 200 || body == null || body.path("access_token").asText("").isBlank()) {
                throw new ServiceException("获取上上签 token 失败（HTTP " + response.statusCode() + "）："
                    + abbreviate(response.body()) + "；请确认 ssq.client-id / ssq.client-secret 正确且该 client 已授权");
            }
            long expiresInMillis = body.path("expires_in").asLong(2833L) * 1000L;
            CachedToken fresh = new CachedToken(body.path("access_token").asText(),
                System.currentTimeMillis() + Math.max(expiresInMillis - TOKEN_REFRESH_AHEAD_MILLIS, 1000L));
            token = fresh;
            log.info("上上签 token 已刷新：baseUrl={}，clientId={}，{} 秒后过期",
                properties.getBaseUrl(), properties.getClientId(), expiresInMillis / 1000);
            return fresh.value();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("获取上上签 token 被中断：" + url, e);
        } catch (IOException e) {
            throw new ServiceException("连不上上上签 token 接口（" + url + "）：" + e.getMessage(), e);
        }
    }

    /**
     * 报告交付：解读人 / 复核人 / 审批人 三方签名 + 本企业印章。
     * 偏移取自文档示例：印章压在「审批人：」左上（-0.1 / -0.1）；签名位对齐关键字（不偏移），
     * 日期在签名右侧 +0.035、上方 0.03。
     */
    private Map<String, Object> deliveryPayload(Path pdf, Map<String, JsonNode> positions) {
        List<Map<String, Object>> labels = new ArrayList<>();
        labels.add(sealLabel(positions.get("审批人："), -0.1, -0.1));
        for (String role : List.of("报告解读人", "报告复核人", "审批人")) {
            JsonNode position = positions.get(role + "：");
            labels.add(signLabel(position, 0, 0, role));
            labels.add(dateLabel(position, 0.035, -0.03, role));
        }
        List<Map<String, Object>> roles = List.of(
            personRole("1", "报告解读人"),
            personRole("2", "报告复核人"),
            personRole("3", "审批人"),
            enterpriseRole("4", false, false));
        return sendPayload(pdf, roles, labels);
    }

    /** 报告延迟：反馈人 / 复核人 签名 + 本企业印章（偏移取自文档示例） */
    private Map<String, Object> delayPayload(Path pdf, Map<String, JsonNode> positions) {
        List<Map<String, Object>> labels = new ArrayList<>();
        labels.add(sealLabel(positions.get("反馈时间"), 0.1, -0.1));
        labels.add(signLabel(positions.get("反馈人"), 0.125, -0.025, "反馈人"));
        labels.add(dateLabel(positions.get("反馈时间"), 0.17, -0.01, "反馈人"));
        labels.add(signLabel(positions.get("复核人"), 0.125, -0.025, "复核人"));
        labels.add(dateLabel(positions.get("复核时间"), 0.17, -0.01, "复核人"));
        List<Map<String, Object>> roles = List.of(
            personRole("1", "反馈人"),
            personRole("2", "复核人"),
            enterpriseRole("3", false, true));
        return sendPayload(pdf, roles, labels);
    }

    /**
     * 生信质控不合格：反馈人 / 复核人 签名 + 本企业<b>自动</b>盖章。
     * 与报告延迟的两处差异（文档如此，勿统一）：签名位偏移 0.145（不是 0.125）；企业章 signMode=AUTO + autoSign=true。
     */
    private Map<String, Object> qcPayload(Path pdf, Map<String, JsonNode> positions) {
        List<Map<String, Object>> labels = new ArrayList<>();
        labels.add(sealLabel(positions.get("反馈时间"), 0.1, -0.1));
        labels.add(signLabel(positions.get("反馈人"), 0.145, -0.025, "反馈人"));
        labels.add(dateLabel(positions.get("反馈时间"), 0.17, -0.01, "反馈人"));
        labels.add(signLabel(positions.get("复核人"), 0.145, -0.025, "复核人"));
        labels.add(dateLabel(positions.get("复核时间"), 0.17, -0.01, "复核人"));
        List<Map<String, Object>> roles = List.of(
            personRole("1", "反馈人"),
            personRole("2", "复核人"),
            enterpriseRole("3", true, true));
        return sendPayload(pdf, roles, labels);
    }

    /** 送签报文（三个场景共用外壳） */
    private Map<String, Object> sendPayload(Path pdf, List<Map<String, Object>> roles, List<Map<String, Object>> labels) {
        requireConfigured(properties.getTemplateId(), "ssq.template-id");
        String fileName = pdf.getFileName().toString();
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("fileName", fileName);
        document.put("content", base64(pdf));
        document.put("contractConfig", Map.of("contractTitle", fileName));
        document.put("appendingSignLabels", labels);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("templateId", properties.getTemplateId());
        payload.put("roles", roles);
        // 文档给的是字符串 "true"，不是布尔
        payload.put("signOrderly", "true");
        payload.put("documents", List.of(document));
        payload.put("sendAction", "SEND");
        return payload;
    }

    /** 个人签署角色（需要通知） */
    private Map<String, Object> personRole(String routeOrder, String roleName) {
        Signer signer = PERSON_SIGNERS.get(routeOrder);
        if (signer == null) {
            throw new ServiceException("未配置签署顺序 " + routeOrder + "（" + roleName + "）的固定签署人，"
                + "见 docs/context/report/report-sign-ssq.md");
        }
        Map<String, Object> role = new LinkedHashMap<>();
        role.put("routeOrder", routeOrder);
        role.put("receiverType", "SIGNER");
        role.put("userType", "PERSON");
        role.put("roleName", roleName);
        role.put("noticeConfig", Map.of("needNotice", true));
        role.put("userInfo", Map.of("userAccount", signer.account(), "userName", signer.userName()));
        return role;
    }

    /**
     * 本企业盖章角色。
     *
     * @param routeOrder      签章顺序
     * @param autoSign        true = 自动签（质控不合格用）
     * @param verifyMethodNone true = noticeConfig 带 verifyMethod=NONE（报告延迟 / 质控不合格用；
     *                         报告交付的文档示例里没有这个字段）
     */
    private Map<String, Object> enterpriseRole(String routeOrder, boolean autoSign, boolean verifyMethodNone) {
        Map<String, Object> role = new LinkedHashMap<>();
        role.put("routeOrder", routeOrder);
        role.put("receiverType", "SIGNER");
        role.put("userType", "ENTERPRISE");
        role.put("roleName", "本企业");
        if (autoSign) {
            role.put("signMode", "AUTO");
            role.put("autoSign", true);
        }
        Map<String, Object> noticeConfig = new LinkedHashMap<>();
        noticeConfig.put("needNotice", false);
        if (verifyMethodNone) {
            noticeConfig.put("verifyMethod", "NONE");
        }
        role.put("noticeConfig", noticeConfig);
        role.put("userInfo", Map.of("userAccount", ENTERPRISE_OPERATOR_ACCOUNT, "enterpriseName", ENTERPRISE_NAME));
        return role;
    }

    /** 签名位（type=SIGNATURE） */
    private Map<String, Object> signLabel(JsonNode position, double dx, double dy, String roleName) {
        return label(position, dx, dy, roleName, "SIGNATURE", null);
    }

    /** 日期位（type=DATE，yyyy-MM-dd） */
    private Map<String, Object> dateLabel(JsonNode position, double dx, double dy, String roleName) {
        return label(position, dx, dy, roleName, "DATE", "yyyy-MM-dd");
    }

    /** 本企业印章（type=SEAL） */
    private Map<String, Object> sealLabel(JsonNode position, double dx, double dy) {
        return label(position, dx, dy, "本企业", "SEAL", null);
    }

    /** 造一个签署位：x/y = 关键字坐标 + 偏移（比例值，PDF 坐标系） */
    private Map<String, Object> label(JsonNode position, double dx, double dy,
                                      String roleName, String type, String dateFormat) {
        Map<String, Object> label = new LinkedHashMap<>();
        label.put("x", position.path("x").asDouble() + dx);
        label.put("y", position.path("y").asDouble() + dy);
        label.put("pageNumber", position.path("pageNumber").asInt());
        label.put("roleName", roleName);
        label.put("type", type);
        if (dateFormat != null) {
            label.put("dateFormat", dateFormat);
        }
        return label;
    }

    /**
     * 定位关键字拿坐标（LocateByMultiKeywords）。
     * 每个关键字都必须有坐标，缺一个就抛：坐标错了会把章盖到别的行上，宁可不签。
     */
    private Map<String, JsonNode> locate(Path pdf, List<String> keywords) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("fileName", pdf.getFileName().toString());
        document.put("content", base64(pdf));
        document.put("order", "0");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("keywords", keywords);
        payload.put("documents", List.of(document));
        JsonNode data = invoke(API_LOCATE, payload).path("data");
        Map<String, JsonNode> positions = new LinkedHashMap<>();
        for (String keyword : keywords) {
            JsonNode list = data.path(keyword);
            if (!list.isArray() || list.isEmpty()) {
                throw new ServiceException("上上签未定位到关键字「" + keyword + "」：" + pdf.getFileName()
                    + "，响应：" + abbreviate(data.toString()));
            }
            positions.put(keyword, list.get(0));
        }
        return positions;
    }

    /** 送签文件必须是真实存在的 PDF：传错 DOCX 不发出去 */
    private Path requirePdf(String pdfPath) {
        if (pdfPath == null || pdfPath.isBlank()) {
            throw new ServiceException("送签文件路径为空");
        }
        Path pdf = Paths.get(pdfPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(pdf)) {
            throw new ServiceException("送签文件不存在：" + pdf);
        }
        byte[] head = new byte[5];
        try (InputStream in = Files.newInputStream(pdf)) {
            int read = in.read(head);
            if (read < head.length || !"%PDF-".equals(new String(head, StandardCharsets.US_ASCII))) {
                throw new ServiceException("送签文件不是 PDF（缺少 %PDF- 文件头）：" + pdf);
            }
        } catch (IOException e) {
            throw new ServiceException("读取送签文件失败：" + pdf + "，" + e.getMessage(), e);
        }
        return pdf;
    }

    /** 送签文档要求 base64 全文（文档入参口径） */
    private String base64(Path pdf) {
        try {
            return Base64.getEncoder().encodeToString(Files.readAllBytes(pdf));
        } catch (IOException e) {
            throw new ServiceException("读取送签文件失败：" + pdf + "，" + e.getMessage(), e);
        }
    }

    /** 网关基址；未配置就快速失败（prod 值确认前留空，宁可报错也不要连错环境） */
    private String baseUrl() {
        String baseUrl = properties.getBaseUrl();
        requireConfigured(baseUrl, "ssq.base-url");
        return baseUrl.replaceAll("/+$", "");
    }

    /** 凭据项必须在调网关前齐：避免把「配了一半」的配置发出去 */
    private void checkCredentials() {
        requireConfigured(properties.getClientId(), "ssq.client-id");
        requireConfigured(properties.getClientSecret(), "ssq.client-secret");
    }

    /** 环境差异项为空 → 直接报「未配置」，不兜底、不猜（prod yml 里现在就是空的，联调前必然踩到） */
    private void requireConfigured(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new ServiceException("未配置 " + key
                + "：按环境写在 application-{dev|prod}.yml（见 docs/context/report/report-sign-ssq.md）");
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ServiceException("构造上上签请求体失败：" + e.getMessage(), e);
        }
    }

    /** 解析响应体；不是 JSON（例如网关报错页）返回 null，由上层按 HTTP 状态报错 */
    private JsonNode parse(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /** 报文摘要：折叠空白 + 截断 200 字符，日志/异常里够用 */
    private String abbreviate(String text) {
        String flat = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        return flat.length() <= 200 ? flat : flat.substring(0, 200) + "…";
    }

    /** 缓存的 token */
    private record CachedToken(String value, long expiresAtMillis) {
    }

    /** 一次网关响应（状态码 + 解析后的 JSON 或 null + 原始报文） */
    private record GatewayResponse(int status, JsonNode body, String raw) {
    }
}
