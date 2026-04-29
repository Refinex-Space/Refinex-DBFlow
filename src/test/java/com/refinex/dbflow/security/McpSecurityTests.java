package com.refinex.dbflow.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.repository.DbfUserRepository;
import com.refinex.dbflow.mcp.DbflowMcpNames;
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
 * MCP endpoint Bearer Token 安全测试。
 *
 * @author refinex
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpSecurityTests {

    /**
     * 测试专用 pepper，模拟安全外部配置。
     */
    private static final String TEST_TOKEN_PEPPER = UUID.randomUUID().toString();

    /**
     * MCP Session Header 名称。
     */
    private static final String MCP_SESSION_HEADER = "Mcp-Session-Id";

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
     * 注册测试专用 MCP Token pepper。
     *
     * @param registry 动态属性注册器
     */
    @DynamicPropertySource
    static void registerTokenPepper(DynamicPropertyRegistry registry) {
        registry.add("dbflow.security.mcp-token.pepper", () -> TEST_TOKEN_PEPPER);
    }

    /**
     * 验证 MCP endpoint 缺少 Bearer Token 时返回 401。
     */
    @Test
    void shouldRejectMcpRequestWithoutBearerToken() {
        ResponseEntity<String> response = post("/mcp", null, null, initializeRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).contains("Bearer");
    }

    /**
     * 验证 MCP endpoint 拒绝非法 Bearer Token。
     */
    @Test
    void shouldRejectInvalidBearerToken() {
        ResponseEntity<String> response = post("/mcp", "dbf_invalid", null, initializeRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * 验证 MCP endpoint 拒绝 query string token。
     */
    @Test
    void shouldRejectQueryStringToken() {
        String bearerToken = issueBearerToken();

        ResponseEntity<String> response = post("/mcp?access_token=dbf_query", bearerToken, null, initializeRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * 验证 MCP endpoint 拒绝已吊销 Token。
     */
    @Test
    void shouldRejectRevokedBearerToken() {
        DbfUser user = createUser();
        McpTokenIssueResult issuedToken = tokenService.issueToken(user.getId(), Instant.now().plus(1, ChronoUnit.DAYS));
        tokenService.revokeActiveToken(user.getId(), Instant.now());

        ResponseEntity<String> response = post("/mcp", issuedToken.plaintextToken(), null, initializeRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * 验证 MCP endpoint 接受合法 Token，且后续请求不能只复用 MCP session。
     */
    @Test
    void shouldAuthenticateEveryMcpRequestWithBearerToken() {
        String bearerToken = issueBearerToken();
        ResponseEntity<String> initializeResponse = post("/mcp", bearerToken, null, initializeRequest());
        String sessionId = initializeResponse.getHeaders().getFirst(MCP_SESSION_HEADER);

        assertThat(initializeResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(sessionId).isNotBlank();

        ResponseEntity<String> unauthenticatedFollowUp = post("/mcp", null, sessionId, toolsListRequest());
        assertThat(unauthenticatedFollowUp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> authenticatedFollowUp = post("/mcp", bearerToken, sessionId, toolsListRequest());
        assertThat(authenticatedFollowUp.getStatusCode().is2xxSuccessful()).isTrue();

        ResponseEntity<String> toolCall = post("/mcp", bearerToken, sessionId, listTargetsToolCallRequest());
        JsonNode toolPayload = parseToolTextPayload(toolCall.getBody());
        assertThat(toolCall.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(toolPayload.path("authentication").path("authenticated").asBoolean()).isTrue();
        assertThat(toolPayload.path("authentication").path("source").asText()).isEqualTo("MCP_BEARER_TOKEN");
        assertThat(toolPayload.path("authentication").path("userId").isNumber()).isTrue();
        assertThat(toolPayload.path("authentication").path("tokenId").isNumber()).isTrue();
        assertThat(toolPayload.path("authentication").path("userAgent").asText()).isEqualTo("dbflow-mcp-security-test");
    }

    /**
     * 发送 MCP HTTP 请求。
     *
     * @param path        请求路径
     * @param bearerToken MCP Bearer Token
     * @param sessionId   MCP Session Id
     * @param body        请求体
     * @return HTTP 响应
     */
    private ResponseEntity<String> post(
            String path,
            String bearerToken,
            String sessionId,
            Map<String, Object> body
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM));
        headers.set(HttpHeaders.USER_AGENT, "dbflow-mcp-security-test");
        headers.set("X-Forwarded-For", "192.0.2.10");
        headers.set("X-Request-Id", "security-test-" + UUID.randomUUID());
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        if (sessionId != null) {
            headers.set(MCP_SESSION_HEADER, sessionId);
        }
        return restTemplate.postForEntity(path, new HttpEntity<>(body, headers), String.class);
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
                        "clientInfo", Map.of("name", "dbflow-security-test", "version", "0.0.1")
                )
        );
    }

    /**
     * 创建 tools/list 请求。
     *
     * @return tools/list 请求体
     */
    private Map<String, Object> toolsListRequest() {
        return Map.of("jsonrpc", "2.0", "id", 2, "method", "tools/list", "params", Map.of());
    }

    /**
     * 创建 dbflow_list_targets tool 调用请求。
     *
     * @return tools/call 请求体
     */
    private Map<String, Object> listTargetsToolCallRequest() {
        return Map.of(
                "jsonrpc", "2.0",
                "id", 3,
                "method", "tools/call",
                "params", Map.of(
                        "name", DbflowMcpNames.TOOL_LIST_TARGETS,
                        "arguments", Map.of()
                )
        );
    }

    /**
     * 解析 MCP tool 文本响应。
     *
     * @param body HTTP 响应体
     * @return tool 文本载荷 JSON
     */
    private JsonNode parseToolTextPayload(String body) {
        JsonNode payload = parsePayload(body);
        String text = payload.path("result").path("content").get(0).path("text").asText();
        try {
            return objectMapper.readTree(text);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("解析 MCP tool 响应失败: " + text, ex);
        }
    }

    /**
     * 解析 MCP HTTP 或 SSE 响应。
     *
     * @param body 响应体
     * @return JSON 节点
     */
    private JsonNode parsePayload(String body) {
        assertThat(body).isNotBlank();
        String json = body.lines()
                .filter(line -> line.startsWith("data:"))
                .map(line -> line.substring("data:".length()))
                .findFirst()
                .orElse(body);
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("解析 MCP 响应失败: " + body, ex);
        }
    }

    /**
     * 颁发测试专用 MCP Bearer Token。
     *
     * @return MCP Bearer Token 明文，仅在本测试请求头内使用
     */
    private String issueBearerToken() {
        DbfUser user = createUser();
        return tokenService.issueToken(user.getId(), Instant.now().plus(1, ChronoUnit.DAYS)).plaintextToken();
    }

    /**
     * 创建测试用户。
     *
     * @return 用户实体
     */
    private DbfUser createUser() {
        String username = "mcp-security-" + UUID.randomUUID();
        return userRepository.save(DbfUser.create(username, username, "password-hash"));
    }
}
