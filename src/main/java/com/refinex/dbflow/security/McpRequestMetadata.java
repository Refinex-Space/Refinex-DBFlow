package com.refinex.dbflow.security;

/**
 * MCP HTTP 请求元信息。
 *
 * @param clientInfo MCP clientInfo 摘要
 * @param userAgent  HTTP User-Agent
 * @param sourceIp   来源 IP
 * @param requestId  请求标识
 * @author refinex
 */
public record McpRequestMetadata(
        String clientInfo,
        String userAgent,
        String sourceIp,
        String requestId
) {
}
