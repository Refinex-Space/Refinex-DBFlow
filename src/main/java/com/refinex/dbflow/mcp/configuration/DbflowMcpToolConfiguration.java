package com.refinex.dbflow.mcp.configuration;

import com.refinex.dbflow.mcp.tool.DbflowMcpSmokeTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * DBFlow MCP Tool 注册配置。
 *
 * @author refinex
 */
@Configuration(proxyBeanMethods = false)
public class DbflowMcpToolConfiguration {

    /**
     * 创建系统时钟。
     *
     * @return UTC 系统时钟
     */
    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }

    /**
     * 注册 DBFlow MCP 工具回调。
     *
     * @param smokeTool MCP 启动探测工具
     * @return MCP 工具回调提供器
     */
    @Bean
    public ToolCallbackProvider dbflowMcpToolCallbackProvider(DbflowMcpSmokeTool smokeTool) {
        return MethodToolCallbackProvider.builder().toolObjects(smokeTool).build();
    }
}
