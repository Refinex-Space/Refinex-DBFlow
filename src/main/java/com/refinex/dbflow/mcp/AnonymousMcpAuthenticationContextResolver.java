package com.refinex.dbflow.mcp;

import org.springframework.stereotype.Component;

/**
 * 匿名 MCP 认证上下文解析器，等待后续 Bearer Token HTTP 认证链路替换。
 *
 * @author refinex
 */
@Component
public class AnonymousMcpAuthenticationContextResolver implements McpAuthenticationContextResolver {

    /**
     * 解析当前 MCP 请求认证上下文。
     *
     * @return 匿名 MCP 请求认证上下文
     */
    @Override
    public McpAuthenticationContext currentContext() {
        return McpAuthenticationContext.anonymous();
    }
}
