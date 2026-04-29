package com.refinex.dbflow.mcp;

import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DBFlow MCP Streamable HTTP Server 配置与工具注册测试。
 *
 * @author refinex
 */
@SpringBootTest
class DbflowMcpServerTests {

    /**
     * MCP smoke tool 名称。
     */
    private static final String SMOKE_TOOL_NAME = "dbflow_smoke";

    /**
     * MCP Server 通用配置属性。
     */
    private final McpServerProperties serverProperties;

    /**
     * MCP Streamable HTTP 配置属性。
     */
    private final McpServerStreamableHttpProperties streamableHttpProperties;

    /**
     * MCP WebMVC Streamable HTTP transport provider。
     */
    private final WebMvcStreamableServerTransportProvider transportProvider;

    /**
     * DBFlow MCP 工具回调提供器。
     */
    private final ToolCallbackProvider toolCallbackProvider;

    /**
     * DBFlow MCP tool skeleton。
     */
    private final DbflowMcpTools dbflowMcpTools;

    /**
     * 创建 DBFlow MCP Server 测试。
     *
     * @param serverProperties         MCP Server 通用配置属性
     * @param streamableHttpProperties MCP Streamable HTTP 配置属性
     * @param transportProvider        MCP WebMVC Streamable HTTP transport provider
     * @param toolCallbackProvider     DBFlow MCP 工具回调提供器
     * @param dbflowMcpTools           DBFlow MCP tool skeleton
     */
    @Autowired
    DbflowMcpServerTests(McpServerProperties serverProperties,
                         McpServerStreamableHttpProperties streamableHttpProperties,
                         WebMvcStreamableServerTransportProvider transportProvider,
                         @Qualifier("dbflowMcpToolCallbackProvider") ToolCallbackProvider toolCallbackProvider,
                         DbflowMcpTools dbflowMcpTools) {
        this.serverProperties = serverProperties;
        this.streamableHttpProperties = streamableHttpProperties;
        this.transportProvider = transportProvider;
        this.toolCallbackProvider = toolCallbackProvider;
        this.dbflowMcpTools = dbflowMcpTools;
    }

    /**
     * 验证 MCP Server 使用 Streamable HTTP 协议和显式 capabilities 配置。
     */
    @Test
    void shouldBindStreamableHttpServerProperties() {
        assertThat(this.serverProperties.getName()).isEqualTo("refinex-dbflow");
        assertThat(this.serverProperties.getVersion()).isEqualTo("0.1.0-SNAPSHOT");
        assertThat(this.serverProperties.getType()).isEqualTo(McpServerProperties.ApiType.SYNC);
        assertThat(this.serverProperties.getProtocol()).isEqualTo(McpServerProperties.ServerProtocol.STREAMABLE);
        assertThat(this.serverProperties.getCapabilities().isTool()).isTrue();
        assertThat(this.serverProperties.getCapabilities().isResource()).isTrue();
        assertThat(this.serverProperties.getCapabilities().isPrompt()).isTrue();
        assertThat(this.streamableHttpProperties.getMcpEndpoint()).isEqualTo("/mcp");
        assertThat(this.transportProvider).isNotNull();
    }

    /**
     * 验证最小 MCP smoke tool 已注册并可调用。
     */
    @Test
    void shouldRegisterAndCallSmokeTool() {
        ToolCallback smokeTool = findSmokeTool();

        assertThat(smokeTool.getToolDefinition().description()).contains("Refinex-DBFlow MCP Server");
        assertThat(smokeTool.call("{}")).contains("UP", "refinex-dbflow", "0.1.0-SNAPSHOT");
    }

    /**
     * 验证每个 DBFlow tool skeleton 都经过认证和授权边界。
     */
    @Test
    void shouldApplyAuthenticationAndAuthorizationBoundaryForSkeletonTools() {
        assertAuthenticationRequired(this.dbflowMcpTools.listTargets());
        assertAuthenticationRequired(this.dbflowMcpTools.inspectSchema("demo", "dev", "app", null, null));
        assertAuthenticationRequired(this.dbflowMcpTools.getEffectivePolicy("demo", "dev", null, null, "SELECT"));
        assertAuthenticationRequired(this.dbflowMcpTools.explainSql("demo", "dev", "select 1", null));
        assertAuthenticationRequired(this.dbflowMcpTools.executeSql("demo", "dev", "select 1", null, true, "test"));
        assertAuthenticationRequired(this.dbflowMcpTools.confirmSql(
                "demo",
                "dev",
                "challenge-1",
                "TRUNCATE TABLE app.orders",
                "test"
        ));
    }

    /**
     * 查找 MCP smoke tool 回调。
     *
     * @return MCP smoke tool 回调
     */
    private ToolCallback findSmokeTool() {
        ToolCallback[] callbacks = this.toolCallbackProvider.getToolCallbacks();
        assertThat(callbacks)
                .extracting(callback -> callback.getToolDefinition().name())
                .contains(SMOKE_TOOL_NAME);
        return Arrays.stream(callbacks)
                .filter(callback -> SMOKE_TOOL_NAME.equals(callback.getToolDefinition().name()))
                .findFirst()
                .orElseThrow();
    }

    /**
     * 断言 skeleton 响应需要认证。
     *
     * @param response skeleton 响应
     */
    private void assertAuthenticationRequired(DbflowMcpSkeletonResponse response) {
        assertThat(response.authentication().authenticated()).isFalse();
        assertThat(response.authorization().checked()).isTrue();
        assertThat(response.authorization().allowed()).isFalse();
        assertThat(response.authorization().reason()).isEqualTo("AUTHENTICATION_REQUIRED");
    }
}
