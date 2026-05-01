package com.refinex.dbflow.security.support;

import com.refinex.dbflow.security.request.McpRequestMetadataExtractor;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * MCP HTTP 安全错误响应写入器，统一输出稳定且脱敏的 JSON。
 *
 * @author refinex
 */
@Component
public class McpSecurityErrorResponseWriter {

    /**
     * 写入 401 认证失败响应。
     *
     * @param response    HTTP 响应
     * @param requestId   请求标识
     * @param bearerError Bearer error code
     * @throws IOException IO 处理异常
     */
    public void unauthorized(HttpServletResponse response, String requestId, String bearerError) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"" + safe(bearerError) + "\"");
        write(response, HttpServletResponse.SC_UNAUTHORIZED, requestId, "UNAUTHORIZED", "MCP Bearer Token 缺失或无效");
    }

    /**
     * 写入 403 禁止访问响应。
     *
     * @param response  HTTP 响应
     * @param requestId 请求标识
     * @param code      稳定错误码
     * @param message   面向客户端的错误说明
     * @throws IOException IO 处理异常
     */
    public void forbidden(HttpServletResponse response, String requestId, String code, String message)
            throws IOException {
        write(response, HttpServletResponse.SC_FORBIDDEN, requestId, code, message);
    }

    /**
     * 写入 413 请求过大响应。
     *
     * @param response  HTTP 响应
     * @param requestId 请求标识
     * @param maxBytes  最大请求体字节数
     * @throws IOException IO 处理异常
     */
    public void requestTooLarge(HttpServletResponse response, String requestId, long maxBytes) throws IOException {
        write(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, requestId, "REQUEST_TOO_LARGE",
                "MCP 请求体超过服务端限制: maxBytes=" + maxBytes);
    }

    /**
     * 写入 429 限流响应。
     *
     * @param response  HTTP 响应
     * @param requestId 请求标识
     * @throws IOException IO 处理异常
     */
    public void rateLimited(HttpServletResponse response, String requestId) throws IOException {
        write(response, 429, requestId, "RATE_LIMITED", "MCP 请求过于频繁，请稍后重试");
    }

    /**
     * 写入统一 JSON 响应。
     *
     * @param response  HTTP 响应
     * @param status    HTTP 状态码
     * @param requestId 请求标识
     * @param code      稳定错误码
     * @param message   面向客户端的错误说明
     * @throws IOException IO 处理异常
     */
    private void write(
            HttpServletResponse response,
            int status,
            String requestId,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.setHeader(McpRequestMetadataExtractor.REQUEST_ID_HEADER, safe(requestId));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{"
                + "\"success\":false,"
                + "\"code\":\"" + json(safe(code)) + "\","
                + "\"message\":\"" + json(safe(message)) + "\","
                + "\"requestId\":\"" + json(safe(requestId)) + "\""
                + "}");
    }

    /**
     * 返回安全文本。
     *
     * @param value 原始文本
     * @return 安全文本
     */
    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value.strip();
    }

    /**
     * JSON 字符串转义。
     *
     * @param value 原始文本
     * @return JSON 安全文本
     */
    private String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
