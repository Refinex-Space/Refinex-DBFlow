package com.refinex.dbflow.mcp;

/**
 * MCP 请求认证上下文解析器。
 *
 * @author refinex
 */
public interface McpAuthenticationContextResolver {

    /**
     * 解析当前 MCP 请求认证上下文。
     *
     * @return MCP 请求认证上下文
     */
    McpAuthenticationContext currentContext();
}
