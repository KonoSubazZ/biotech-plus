package org.dromara.report.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.config.CmsApiProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * iPaaS 网关客户端（CMS 订单样本查询）：只负责 token 与一次 invoke，不碰数据库、不碰业务。
 * <p>
 * 接口地址末段 = {@code Base64("HJERK:CMS:" + 接口名)}（实测解出 {@code HJERK:CMS:pharmaQuery}），
 * 与上上签的 {@code Base64("HZERO:BESTSIGN:" + 接口名)} 同一套网关约定。
 * token 按 {@code expires_in} 缓存（提前 60 秒失效）；HTTP 401 或 {@code accessTokenInvalid}
 * 换一次 token 重试一次。HTTP 用 JDK HttpClient，不引第三方依赖（与 {@code SsqSignServiceImpl} 同口径）。
 *
 * @author <你的名字>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CmsApiClient {

    /** 网关调用前缀 */
    private static final String INVOKE_PREFIX = "/api/hitf/v2p/rest/invoke/";

    /** 换 token 地址 */
    private static final String TOKEN_PATH = "/api/oauth/oauth/token";

    /** 网关接口名前缀（Base64 原文） */
    private static final String GATEWAY_PREFIX = "HZERO:CMS:";

    /** 订单样本查询接口名 */
    private static final String API_PHARMA_QUERY = "pharmaQuery";

    private final CmsApiProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .version(HttpClient.Version.HTTP_1_1)
        .build();

    private volatile String token;
    private volatile long tokenExpireAt;

    /**
     * 调用 CMS 订单样本查询（pharmaQuery）。
     *
     * @param requestInfo 业务参数：sampleID 或 startTime+endTime，另有 currPage / numPerPage
     * @return 网关返回里的 {@code responseInfo} 节点（含 orderSamples / hasMore / returnedCount）
     */
    public JsonNode invokePharmaQuery(Map<String, Object> requestInfo) {
        assertConfigured();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("esbInfo", Map.of("instId", ""));
        body.put("requestInfo", requestInfo);
        JsonNode root = read(postForm(pharmaQueryUrl(), body, true));
        JsonNode responseInfo = root.path("responseInfo");
        if (responseInfo.isMissingNode() || responseInfo.isNull()) {
            throw new ServiceException("CMS 接口返回缺少 responseInfo：" + brief(root.toString()));
        }
        return responseInfo;
    }

    private String pharmaQueryUrl() {
        String encoded = Base64.getEncoder().encodeToString(
            (GATEWAY_PREFIX + API_PHARMA_QUERY).getBytes(StandardCharsets.UTF_8));
        return trimSlash(props.getBaseUrl()) + INVOKE_PREFIX + encoded;
    }

    private void assertConfigured() {
        if (isBlank(props.getBaseUrl()) || isBlank(props.getClientId()) || isBlank(props.getClientSecret())) {
            throw new ServiceException("未配置 cms.api（base-url / client-id / client-secret），无法调用 CMS 接口");
        }
    }

    /**
     * 表单 POST（{@code jsonString=<整段 JSON>}，与 drug_api.py 逐字一致）。
     *
     * @param url      完整地址
     * @param payload  业务体
     * @param retryOnTokenExpired 是否允许因 token 失效重试一次
     * @return 响应体
     */
    private String postForm(String url, Object payload, boolean retryOnTokenExpired) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Bearer " + token())
                .POST(HttpRequest.BodyPublishers.ofString(
                    "jsonString=" + URLEncoder.encode(json, StandardCharsets.UTF_8), StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (retryOnTokenExpired && (resp.statusCode() == 401 || resp.body().contains("accessTokenInvalid"))) {
                log.info("CMS 网关 token 失效，换 token 后重试一次");
                invalidateToken();
                return postForm(url, payload, false);
            }
            if (resp.statusCode() / 100 != 2) {
                throw new ServiceException("CMS 接口 HTTP " + resp.statusCode() + "：" + brief(resp.body()));
            }
            return resp.body();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("调用 CMS 接口失败：" + e.getMessage());
        }
    }

    private synchronized String token() {
        if (token != null && System.currentTimeMillis() < tokenExpireAt) {
            return token;
        }
        try {
            String form = "grant_type=client_credentials"
                + "&client_id=" + URLEncoder.encode(props.getClientId(), StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(props.getClientSecret(), StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder(URI.create(trimSlash(props.getBaseUrl()) + TOKEN_PATH))
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() / 100 != 2) {
                throw new ServiceException("获取 iPaaS token 失败：HTTP " + resp.statusCode() + " " + brief(resp.body()));
            }
            JsonNode node = read(resp.body());
            String accessToken = node.path("access_token").asText();
            if (isBlank(accessToken)) {
                throw new ServiceException("获取 iPaaS token 失败：响应里没有 access_token");
            }
            long expiresIn = node.path("expires_in").asLong(2800);
            token = accessToken;
            tokenExpireAt = System.currentTimeMillis() + Math.max(60, expiresIn - 60) * 1000L;
            log.info("CMS iPaaS token 已刷新，expires_in={}s", expiresIn);
            return token;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("获取 iPaaS token 失败：" + e.getMessage());
        }
    }

    private synchronized void invalidateToken() {
        token = null;
        tokenExpireAt = 0L;
    }

    private JsonNode read(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new ServiceException("CMS 接口返回不是合法 JSON：" + brief(body));
        }
    }

    private static String trimSlash(String url) {
        return url.replaceAll("/+$", "");
    }

    private static String brief(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 300 ? s : s.substring(0, 300) + "...";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
