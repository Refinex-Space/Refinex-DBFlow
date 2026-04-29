package com.refinex.dbflow.mcp;

import com.refinex.dbflow.security.McpAuthenticationToken;
import com.refinex.dbflow.security.McpRequestMetadata;
import com.refinex.dbflow.security.McpTokenValidationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 基于 Spring SecurityContext 的 MCP 认证上下文解析器。
 *
 * @author refinex
 */
@Component
public class SecurityContextMcpAuthenticationContextResolver implements McpAuthenticationContextResolver {

    /**
     * 解析当前 MCP 请求认证上下文。
     *
     * @return MCP 请求认证上下文
     */
    @Override
    public McpAuthenticationContext currentContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof McpAuthenticationToken mcpAuthentication) || !authentication.isAuthenticated()) {
            return McpAuthenticationContext.anonymous();
        }
        McpTokenValidationResult validationResult = mcpAuthentication.getValidationResult();
        McpRequestMetadata metadata = requestMetadata(mcpAuthentication);
        return new McpAuthenticationContext(
                true,
                mcpAuthentication.getName(),
                validationResult.userId(),
                validationResult.tokenId(),
                validationResult.tokenPrefix(),
                "MCP_BEARER_TOKEN",
                metadata.clientInfo(),
                metadata.userAgent(),
                metadata.sourceIp(),
                metadata.requestId()
        );
    }

    /**
     * 读取 MCP 请求元信息。
     *
     * @param authentication MCP authentication
     * @return MCP 请求元信息
     */
    private McpRequestMetadata requestMetadata(McpAuthenticationToken authentication) {
        if (authentication.getDetails() instanceof McpRequestMetadata metadata) {
            return metadata;
        }
        return new McpRequestMetadata("unknown", "unknown", "unknown", "unknown");
    }
}
