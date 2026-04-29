package com.refinex.dbflow.observability;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 请求标识过滤器。
 *
 * @author refinex
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    /**
     * 请求标识 HTTP 头名称。
     */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    /**
     * 日志 MDC 中保存 request id 的键名。
     */
    public static final String MDC_KEY = "requestId";

    /**
     * 为每个 HTTP 请求建立 request id，并写入响应头和日志上下文。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain Servlet 过滤器链
     * @throws ServletException Servlet 处理异常
     * @throws IOException IO 处理异常
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put(MDC_KEY, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * 解析请求中的 request id；调用方未传入时生成新的随机标识。
     *
     * @param request HTTP 请求
     * @return 可用于本次请求日志串联的 request id
     */
    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId;
    }
}
