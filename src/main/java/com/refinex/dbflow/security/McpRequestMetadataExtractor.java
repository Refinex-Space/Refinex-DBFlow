package com.refinex.dbflow.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * MCP HTTP 请求元信息提取器。
 *
 * @author refinex
 */
@Component
public class McpRequestMetadataExtractor {

    /**
     * 请求标识 HTTP 头名称。
     */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    /**
     * MCP clientInfo 透传头名称；JSON-RPC body 中的 clientInfo 后续由 MCP 审计层提取。
     */
    private static final String MCP_CLIENT_INFO_HEADER = "Mcp-Client-Info";

    /**
     * 代理转发来源 IP 头名称。
     */
    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";

    /**
     * 反向代理真实来源 IP 头名称。
     */
    private static final String X_REAL_IP_HEADER = "X-Real-IP";

    /**
     * 从 HTTP 请求提取 MCP 元信息。
     *
     * @param request HTTP 请求
     * @return MCP 请求元信息
     */
    public McpRequestMetadata extract(HttpServletRequest request) {
        return new McpRequestMetadata(
                valueOrUnknown(request.getHeader(MCP_CLIENT_INFO_HEADER)),
                valueOrUnknown(request.getHeader(HttpHeaders.USER_AGENT)),
                resolveSourceIp(request),
                resolveRequestId(request)
        );
    }

    /**
     * 解析来源 IP。
     *
     * @param request HTTP 请求
     * @return 来源 IP
     */
    private String resolveSourceIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR_HEADER);
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        String realIp = request.getHeader(X_REAL_IP_HEADER);
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return valueOrUnknown(request.getRemoteAddr());
    }

    /**
     * 解析请求标识。
     *
     * @param request HTTP 请求
     * @return 请求标识
     */
    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (StringUtils.hasText(requestId)) {
            return requestId.trim();
        }
        return UUID.randomUUID().toString();
    }

    /**
     * 返回非空值或 unknown。
     *
     * @param value 原始值
     * @return 非空值或 unknown
     */
    private String valueOrUnknown(String value) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return "unknown";
    }
}
