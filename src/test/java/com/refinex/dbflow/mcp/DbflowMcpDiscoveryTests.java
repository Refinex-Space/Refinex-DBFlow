package com.refinex.dbflow.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.repository.DbfUserRepository;
import com.refinex.dbflow.security.McpTokenIssueResult;
import com.refinex.dbflow.security.McpTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DBFlow MCP 暴露面 discovery 测试。
 *
 * @author refinex
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DbflowMcpDiscoveryTests {

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
    private final TestRestTemplate restTemplate;

    /**
     * JSON 序列化器。
     */
    private final ObjectMapper objectMapper;

    /**
     * MCP Token 生命周期服务。
     */
    private final McpTokenService tokenService;

    /**
     * 用户 repository。
     */
    private final DbfUserRepository userRepository;

    /**
     * 创建 DBFlow MCP discovery 测试。
     *
     * @param restTemplate HTTP 测试客户端
     * @param objectMapper JSON 序列化器
     * @param tokenService MCP Token 生命周期服务
     * @param userRepository 用户 repository
     */
    @Autowired
    DbflowMcpDiscoveryTests(
            TestRestTemplate restTemplate,
            ObjectMapper objectMapper,
            McpTokenService tokenService,
            DbfUserRepository userRepository
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

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
     * 验证 tools、resources、resource templates 和 prompts 均可被 MCP 客户端发现。
     */
    @Test
    void shouldDiscoverDbflowMcpSurface() {
        String bearerToken = issueBearerToken();
        String sessionId = initializeSession(bearerToken);

        JsonNode tools = request(bearerToken, sessionId, 2, "tools/list", Map.of()).path("result").path("tools");
        assertThat(names(tools, "name")).contains(
                DbflowMcpNames.TOOL_LIST_TARGETS,
                DbflowMcpNames.TOOL_INSPECT_SCHEMA,
                DbflowMcpNames.TOOL_GET_EFFECTIVE_POLICY,
                DbflowMcpNames.TOOL_EXPLAIN_SQL,
                DbflowMcpNames.TOOL_EXECUTE_SQL,
                DbflowMcpNames.TOOL_CONFIRM_SQL
        );
        JsonNode inspectSchemaTool = findByName(tools, DbflowMcpNames.TOOL_INSPECT_SCHEMA);
        assertThat(inspectSchemaTool.path("description").asText()).contains("schema");
        assertThat(inspectSchemaTool.path("inputSchema").path("properties").has("project")).isTrue();
        assertThat(inspectSchemaTool.path("inputSchema").path("properties").has("env")).isTrue();

        JsonNode resources = request(bearerToken, sessionId, 3, "resources/list", Map.of())
                .path("result")
                .path("resources");
        assertThat(names(resources, "uri")).contains(DbflowMcpNames.RESOURCE_TARGETS);

        JsonNode resourceTemplates = request(bearerToken, sessionId, 4, "resources/templates/list", Map.of())
                .path("result")
                .path("resourceTemplates");
        assertThat(names(resourceTemplates, "uriTemplate")).contains(
                DbflowMcpNames.RESOURCE_SCHEMA,
                DbflowMcpNames.RESOURCE_POLICY
        );

        JsonNode prompts = request(bearerToken, sessionId, 5, "prompts/list", Map.of()).path("result").path("prompts");
        assertThat(names(prompts, "name")).contains(
                DbflowMcpNames.PROMPT_SAFE_MYSQL_CHANGE,
                DbflowMcpNames.PROMPT_EXPLAIN_PLAN_REVIEW
        );
        JsonNode safeChangePrompt = findByName(prompts, DbflowMcpNames.PROMPT_SAFE_MYSQL_CHANGE);
        assertThat(names(safeChangePrompt.path("arguments"), "name")).contains("project", "env", "change_request");
    }

    /**
     * 初始化 MCP Session。
     *
     * @return MCP Session Id
     */
    private String initializeSession(String bearerToken) {
        ResponseEntity<String> response = post(bearerToken, null, Map.of(
                "jsonrpc", "2.0",
                "id", 1,
                "method", "initialize",
                "params", Map.of(
                        "protocolVersion", "2025-03-26",
                        "capabilities", Map.of(),
                        "clientInfo", Map.of("name", "dbflow-discovery-test", "version", "0.0.1")
                )
        ));
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getFirst(MCP_SESSION_HEADER)).isNotBlank();
        JsonNode payload = parsePayload(response.getBody());
        assertThat(payload.path("result").path("serverInfo").path("name").asText()).isEqualTo("refinex-dbflow");
        return response.getHeaders().getFirst(MCP_SESSION_HEADER);
    }

    /**
     * 发送 MCP JSON-RPC 请求。
     *
     * @param bearerToken MCP Bearer Token
     * @param sessionId MCP Session Id
     * @param id        JSON-RPC 请求 Id
     * @param method    JSON-RPC 方法名
     * @param params    JSON-RPC 参数
     * @return JSON-RPC 响应
     */
    private JsonNode request(String bearerToken, String sessionId, int id, String method, Map<String, Object> params) {
        ResponseEntity<String> response = post(bearerToken, sessionId, Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "method", method,
                "params", params
        ));
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return parsePayload(response.getBody());
    }

    /**
     * 发送 MCP HTTP 请求。
     *
     * @param bearerToken MCP Bearer Token
     * @param sessionId MCP Session Id
     * @param body      请求体
     * @return HTTP 响应
     */
    private ResponseEntity<String> post(String bearerToken, String sessionId, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM));
        headers.setBearerAuth(bearerToken);
        if (sessionId != null) {
            headers.set(MCP_SESSION_HEADER, sessionId);
        }
        return restTemplate.postForEntity("/mcp", new HttpEntity<>(body, headers), String.class);
    }

    /**
     * 颁发测试专用 MCP Bearer Token。
     *
     * @return MCP Bearer Token 明文，仅在本测试请求头内使用
     */
    private String issueBearerToken() {
        String username = "mcp-discovery-" + UUID.randomUUID();
        DbfUser user = userRepository.save(DbfUser.create(username, username, "password-hash"));
        McpTokenIssueResult issueResult = tokenService.issueToken(user.getId(), Instant.now().plus(1, ChronoUnit.DAYS));
        return issueResult.plaintextToken();
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
     * 提取节点数组中的字段值。
     *
     * @param nodes 节点数组
     * @param field 字段名
     * @return 字段值列表
     */
    private List<String> names(JsonNode nodes, String field) {
        List<String> values = new ArrayList<>();
        nodes.forEach(node -> values.add(node.path(field).asText()));
        return values;
    }

    /**
     * 按名称查找节点。
     *
     * @param nodes 节点数组
     * @param name  名称
     * @return 匹配节点
     */
    private JsonNode findByName(JsonNode nodes, String name) {
        for (JsonNode node : nodes) {
            if (name.equals(node.path("name").asText())) {
                return node;
            }
        }
        throw new IllegalArgumentException("未发现 MCP 节点: " + name);
    }
}
