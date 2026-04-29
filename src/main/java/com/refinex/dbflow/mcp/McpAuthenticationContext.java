package com.refinex.dbflow.mcp;

/**
 * MCP 请求认证上下文。
 *
 * @param authenticated 是否已认证
 * @param principal     认证主体标识
 * @param userId        元数据库用户主键
 * @param tokenId       MCP Token 元数据主键
 * @param tokenPrefix   MCP Token 展示前缀
 * @param source        认证来源
 * @param clientInfo    MCP clientInfo 摘要
 * @param userAgent     HTTP User-Agent
 * @param sourceIp      来源 IP
 * @param requestId     请求标识
 * @author refinex
 */
public record McpAuthenticationContext(
        boolean authenticated,
        String principal,
        Long userId,
        Long tokenId,
        String tokenPrefix,
        String source,
        String clientInfo,
        String userAgent,
        String sourceIp,
        String requestId
) {

    /**
     * 创建匿名认证上下文。
     *
     * @return 匿名认证上下文
     */
    public static McpAuthenticationContext anonymous() {
        return new McpAuthenticationContext(
                false,
                "anonymous",
                null,
                null,
                null,
                "ANONYMOUS",
                "unknown",
                "unknown",
                "unknown",
                "unknown"
        );
    }
}
