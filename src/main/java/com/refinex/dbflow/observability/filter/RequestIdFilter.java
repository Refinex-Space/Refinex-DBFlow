package com.refinex.dbflow.observability.filter;

import com.refinex.dbflow.observability.support.LogContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

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
     * 链路标识 HTTP 头名称。
     */
    public static final String TRACE_ID_HEADER = LogContext.TRACE_ID_HEADER;

    /**
     * 日志 MDC 中保存 request id 的键名。
     */
    public static final String MDC_KEY = LogContext.REQUEST_ID_KEY;

    /**
     * 日志 MDC 中保存 trace id 的键名。
     */
    public static final String TRACE_MDC_KEY = LogContext.TRACE_ID_KEY;

    /**
     * 为每个 HTTP 请求建立 request id，并写入响应头和日志上下文。
     *
     * @param request     HTTP 请求
     * @param response    HTTP 响应
     * @param filterChain Servlet 过滤器链
     * @throws ServletException Servlet 处理异常
     * @throws IOException      IO 处理异常
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        String traceId = resolveTraceId(request, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try (LogContext.Scope ignored = LogContext.withCorrelation(requestId, traceId)) {
            filterChain.doFilter(request, response);
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

    /**
     * 解析请求中的 trace id；未传入时沿用 request id。
     *
     * @param request   HTTP 请求
     * @param requestId 请求标识
     * @return 可用于跨服务日志串联的 trace id
     */
    private String resolveTraceId(HttpServletRequest request, String requestId) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            return requestId;
        }
        return traceId.trim();
    }
}
