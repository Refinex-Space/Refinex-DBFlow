package com.refinex.dbflow.mcp;

import com.refinex.dbflow.observability.DbflowMetricsService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * DBFlow MCP 启动探测工具，只用于证明 MCP Server 和工具注册链路可用。
 *
 * @author refinex
 */
@Component
public class DbflowMcpSmokeTool {

    /**
     * MCP Server 名称。
     */
    private static final String SERVER_NAME = "refinex-dbflow";

    /**
     * MCP Server 版本。
     */
    private static final String SERVER_VERSION = "0.1.0-SNAPSHOT";

    /**
     * 系统时钟。
     */
    private final Clock clock;

    /**
     * DBFlow 指标服务，部分 slice 测试中允许不存在。
     */
    private final DbflowMetricsService metricsService;

    /**
     * 创建 DBFlow MCP 启动探测工具。
     *
     * @param clock                  系统时钟
     * @param metricsServiceProvider DBFlow 指标服务 provider
     */
    public DbflowMcpSmokeTool(Clock clock, ObjectProvider<DbflowMetricsService> metricsServiceProvider) {
        this.clock = clock;
        this.metricsService = metricsServiceProvider.getIfAvailable();
    }

    /**
     * 返回 MCP Server 的最小健康信息。
     *
     * @return MCP Server 启动探测结果
     */
    @Tool(name = "dbflow_smoke", description = "检查 Refinex-DBFlow MCP Server 是否已启动。")
    public DbflowMcpSmokeResponse smoke() {
        if (metricsService != null) {
            metricsService.recordMcpCall("dbflow_smoke");
        }
        return new DbflowMcpSmokeResponse("UP", SERVER_NAME, SERVER_VERSION, Instant.now(this.clock).toString());
    }

    /**
     * MCP Server 启动探测响应。
     *
     * @param status        服务状态
     * @param serverName    MCP Server 名称
     * @param serverVersion MCP Server 版本
     * @param observedAt    探测时间
     * @author refinex
     */
    public record DbflowMcpSmokeResponse(String status, String serverName, String serverVersion, String observedAt) {
    }
}
