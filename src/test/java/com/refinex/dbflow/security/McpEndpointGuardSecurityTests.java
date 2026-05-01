package com.refinex.dbflow.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.repository.DbfUserRepository;
import com.refinex.dbflow.security.token.McpTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MCP endpoint Origin、大小和限流入口防护测试。
 *
 * @author refinex
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpEndpointGuardSecurityTests {

    /**
     * 测试专用 Token pepper。
     */
    private static final String TEST_TOKEN_PEPPER = UUID.randomUUID().toString();

    /**
     * 可信测试 Origin。
     */
    private static final String TRUSTED_ORIGIN = "http://trusted.lan:8080";

    /**
     * HTTP 测试客户端。
     */
    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * MCP Token 生命周期服务。
     */
    @Autowired
    private McpTokenService tokenService;

    /**
     * 用户 repository。
     */
    @Autowired
    private DbfUserRepository userRepository;

    /**
     * JSON 序列化器。
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 注册入口防护测试配置。
     *
     * @param registry 动态属性注册器
     */
    @DynamicPropertySource
    static void registerSecurityProperties(DynamicPropertyRegistry registry) {
        registry.add("dbflow.security.mcp-token.pepper", () -> TEST_TOKEN_PEPPER);
        registry.add("dbflow.security.mcp-endpoint.origin.trusted-origins[0]", () -> TRUSTED_ORIGIN);
        registry.add("dbflow.security.mcp-endpoint.request-size.max-bytes", () -> "1024");
        registry.add("dbflow.security.mcp-endpoint.rate-limit.max-requests", () -> "2");
        registry.add("dbflow.security.mcp-endpoint.rate-limit.window", () -> "5m");
    }

    /**
     * 验证非可信 Origin 被拒绝。
     */
    @Test
    void shouldRejectUntrustedOrigin() {
        String token = issueBearerToken();

        ResponseEntity<String> response = post(initializeRequest(), token, "http://evil.lan", "198.51.100.10");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(parsePayload(response.getBody()).path("code").asText()).isEqualTo("ORIGIN_DENIED");
        assertThat(response.getBody()).doesNotContain(token, "Exception", "jdbc:", "password");
    }

    /**
     * 验证可信 Origin 可继续进入 MCP 协议处理。
     */
    @Test
    void shouldAcceptTrustedOrigin() {
        String token = issueBearerToken();

        ResponseEntity<String> response = post(initializeRequest(), token, TRUSTED_ORIGIN, "198.51.100.11");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    /**
     * 验证超过 Content-Length 上限的 MCP 请求被拒绝。
     */
    @Test
    void shouldRejectOversizedRequest() {
        String token = issueBearerToken();
        String oversizedJson = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"payload\":\""
                + "x".repeat(1500) + "\"}}";

        ResponseEntity<String> response = postRaw(oversizedJson, token, TRUSTED_ORIGIN, "198.51.100.12");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(parsePayload(response.getBody()).path("code").asText()).isEqualTo("REQUEST_TOO_LARGE");
        assertThat(response.getBody()).doesNotContain(token, "Exception", "jdbc:", "password");
    }

    /**
     * 验证同一来源 IP 超过固定窗口请求数后被限流。
     */
    @Test
    void shouldRateLimitBySourceIpBeforeAuthentication() {
        String sourceIp = "198.51.100.13";

        ResponseEntity<String> first = post(initializeRequest(), "dbf_invalid_1", TRUSTED_ORIGIN, sourceIp);
        ResponseEntity<String> second = post(initializeRequest(), "dbf_invalid_2", TRUSTED_ORIGIN, sourceIp);
        ResponseEntity<String> third = post(initializeRequest(), "dbf_invalid_3", TRUSTED_ORIGIN, sourceIp);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(third.getStatusCode().value()).isEqualTo(429);
        assertThat(parsePayload(third.getBody()).path("code").asText()).isEqualTo("RATE_LIMITED");
        assertThat(third.getBody()).doesNotContain("dbf_invalid_3", "Exception", "jdbc:", "password");
    }

    /**
     * 发送 JSON MCP 请求。
     *
     * @param body        请求体
     * @param bearerToken Bearer Token
     * @param origin      Origin
     * @param sourceIp    来源 IP
     * @return HTTP 响应
     */
    private ResponseEntity<String> post(
            Map<String, Object> body,
            String bearerToken,
            String origin,
            String sourceIp
    ) {
        return restTemplate.postForEntity("/mcp", new HttpEntity<>(body, headers(bearerToken, origin, sourceIp)),
                String.class);
    }

    /**
     * 发送原始 JSON MCP 请求。
     *
     * @param body        原始请求体
     * @param bearerToken Bearer Token
     * @param origin      Origin
     * @param sourceIp    来源 IP
     * @return HTTP 响应
     */
    private ResponseEntity<String> postRaw(
            String body,
            String bearerToken,
            String origin,
            String sourceIp
    ) {
        return restTemplate.postForEntity("/mcp", new HttpEntity<>(body, headers(bearerToken, origin, sourceIp)),
                String.class);
    }

    /**
     * 创建请求头。
     *
     * @param bearerToken Bearer Token
     * @param origin      Origin
     * @param sourceIp    来源 IP
     * @return 请求头
     */
    private HttpHeaders headers(String bearerToken, String origin, String sourceIp) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM));
        headers.set(HttpHeaders.USER_AGENT, "dbflow-mcp-guard-test");
        headers.set("X-Forwarded-For", sourceIp);
        headers.set("X-Request-Id", "guard-test-" + UUID.randomUUID());
        headers.set(HttpHeaders.ORIGIN, origin);
        headers.setBearerAuth(bearerToken);
        return headers;
    }

    /**
     * 创建 MCP initialize 请求。
     *
     * @return MCP initialize 请求体
     */
    private Map<String, Object> initializeRequest() {
        return Map.of(
                "jsonrpc", "2.0",
                "id", 1,
                "method", "initialize",
                "params", Map.of(
                        "protocolVersion", "2025-03-26",
                        "capabilities", Map.of(),
                        "clientInfo", Map.of("name", "dbflow-guard-test", "version", "0.0.1")
                )
        );
    }

    /**
     * 解析 JSON 响应。
     *
     * @param body 响应体
     * @return JSON 节点
     */
    private JsonNode parsePayload(String body) {
        assertThat(body).isNotBlank();
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("解析 MCP guard 响应失败: " + body, exception);
        }
    }

    /**
     * 颁发测试专用 MCP Bearer Token。
     *
     * @return MCP Bearer Token 明文，仅在本测试请求头内使用
     */
    private String issueBearerToken() {
        DbfUser user = userRepository.save(DbfUser.create(
                "mcp-guard-" + UUID.randomUUID(),
                "MCP Guard",
                "password-hash"
        ));
        return tokenService.issueToken(user.getId(), Instant.now().plus(1, ChronoUnit.DAYS)).plaintextToken();
    }
}
