package com.refinex.dbflow.security;

import com.refinex.dbflow.observability.LogContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP endpoint 入口防护过滤器，负责 Origin、请求大小和基础限流。
 *
 * @author refinex
 */
public class McpEndpointGuardFilter extends OncePerRequestFilter {

    /**
     * 运维日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(McpEndpointGuardFilter.class);

    /**
     * MCP endpoint 安全配置。
     */
    private final McpEndpointSecurityProperties properties;

    /**
     * MCP 请求元信息提取器。
     */
    private final McpRequestMetadataExtractor metadataExtractor;

    /**
     * MCP 安全错误响应写入器。
     */
    private final McpSecurityErrorResponseWriter errorResponseWriter;

    /**
     * 时钟。
     */
    private final Clock clock;

    /**
     * 来源 IP 到固定窗口计数器的映射。
     */
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    /**
     * 创建 MCP endpoint 防护过滤器。
     *
     * @param properties          MCP endpoint 安全配置
     * @param metadataExtractor   MCP 请求元信息提取器
     * @param errorResponseWriter MCP 安全错误响应写入器
     */
    public McpEndpointGuardFilter(
            McpEndpointSecurityProperties properties,
            McpRequestMetadataExtractor metadataExtractor,
            McpSecurityErrorResponseWriter errorResponseWriter
    ) {
        this(properties, metadataExtractor, errorResponseWriter, Clock.systemUTC());
    }

    /**
     * 创建 MCP endpoint 防护过滤器。
     *
     * @param properties          MCP endpoint 安全配置
     * @param metadataExtractor   MCP 请求元信息提取器
     * @param errorResponseWriter MCP 安全错误响应写入器
     * @param clock               时钟
     */
    McpEndpointGuardFilter(
            McpEndpointSecurityProperties properties,
            McpRequestMetadataExtractor metadataExtractor,
            McpSecurityErrorResponseWriter errorResponseWriter,
            Clock clock
    ) {
        this.properties = properties;
        this.metadataExtractor = metadataExtractor;
        this.errorResponseWriter = errorResponseWriter;
        this.clock = clock;
    }

    /**
     * 对 MCP 请求执行入口防护。
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
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        McpRequestMetadata metadata = metadataExtractor.extract(request);
        try (LogContext.Scope ignored = LogContext.withCorrelation(
                metadata.requestId(),
                LogContext.currentTraceIdOrDefault(metadata.requestId()))) {
            LOGGER.info("mcp.request.received method={} sourceIp={} contentLength={} originPresent={}",
                    request.getMethod(), metadata.sourceIp(), request.getContentLengthLong(),
                    StringUtils.hasText(request.getHeader(HttpHeaders.ORIGIN)));
            if (!allowedOrigin(request)) {
                LOGGER.warn("mcp.request.rejected reason=origin-denied sourceIp={} origin={}",
                        metadata.sourceIp(), normalizeOrigin(request.getHeader(HttpHeaders.ORIGIN)));
                errorResponseWriter.forbidden(response, metadata.requestId(), "ORIGIN_DENIED",
                        "Origin 不在 MCP 可信来源列表");
                return;
            }
            if (requestTooLarge(request)) {
                LOGGER.warn("mcp.request.rejected reason=request-too-large sourceIp={} contentLength={} maxBytes={}",
                        metadata.sourceIp(), request.getContentLengthLong(), properties.getRequestSize().getMaxBytes());
                errorResponseWriter.requestTooLarge(response, metadata.requestId(),
                        properties.getRequestSize().getMaxBytes());
                return;
            }
            if (rateLimited(metadata.sourceIp())) {
                LOGGER.warn("mcp.request.rejected reason=rate-limited sourceIp={} maxRequests={} window={}",
                        metadata.sourceIp(), properties.getRateLimit().getMaxRequests(),
                        properties.getRateLimit().getWindow());
                errorResponseWriter.rateLimited(response, metadata.requestId());
                return;
            }
            HttpServletRequest guardedRequest = limitedBodyRequest(request);
            if (guardedRequest == null) {
                LOGGER.warn(
                        "mcp.request.rejected reason=request-too-large sourceIp={} contentLength=chunked maxBytes={}",
                        metadata.sourceIp(), properties.getRequestSize().getMaxBytes());
                errorResponseWriter.requestTooLarge(response, metadata.requestId(),
                        properties.getRequestSize().getMaxBytes());
                return;
            }
            filterChain.doFilter(guardedRequest, response);
        }
    }

    /**
     * Streamable HTTP 使用异步分发，异步线程不重复消耗限流额度。
     *
     * @return true 表示异步分发跳过本过滤器
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    /**
     * 判断请求 Origin 是否允许。
     *
     * @param request HTTP 请求
     * @return 允许时返回 true
     */
    private boolean allowedOrigin(HttpServletRequest request) {
        McpEndpointSecurityProperties.Origin originProperties = properties.getOrigin();
        if (!originProperties.isEnabled()) {
            return true;
        }
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (!StringUtils.hasText(origin)) {
            return true;
        }
        String normalizedOrigin = normalizeOrigin(origin);
        return originProperties.getTrustedOrigins()
                .stream()
                .map(this::normalizeOrigin)
                .anyMatch(trustedOrigin -> "*".equals(trustedOrigin) || trustedOrigin.equals(normalizedOrigin));
    }

    /**
     * 判断请求体是否超过配置上限。
     *
     * @param request HTTP 请求
     * @return 超限时返回 true
     */
    private boolean requestTooLarge(HttpServletRequest request) {
        McpEndpointSecurityProperties.RequestSize requestSize = properties.getRequestSize();
        long contentLength = request.getContentLengthLong();
        return requestSize.isEnabled() && contentLength >= 0 && contentLength > requestSize.getMaxBytes();
    }

    /**
     * 读取并缓存受大小限制的请求体。
     *
     * @param request HTTP 请求
     * @return 缓存后的请求；超限时返回 null
     * @throws IOException IO 处理异常
     */
    private HttpServletRequest limitedBodyRequest(HttpServletRequest request) throws IOException {
        McpEndpointSecurityProperties.RequestSize requestSize = properties.getRequestSize();
        if (!requestSize.isEnabled()) {
            return request;
        }
        long maxBytes = requestSize.getMaxBytes();
        ByteArrayOutputStream output = new ByteArrayOutputStream((int) Math.min(maxBytes, 8192L));
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        ServletInputStream inputStream = request.getInputStream();
        while ((read = inputStream.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                return null;
            }
            output.write(buffer, 0, read);
        }
        return new CachedBodyRequest(request, output.toByteArray());
    }

    /**
     * 判断来源 IP 是否触发限流。
     *
     * @param sourceIp 来源 IP
     * @return 触发限流时返回 true
     */
    private boolean rateLimited(String sourceIp) {
        McpEndpointSecurityProperties.RateLimit rateLimit = properties.getRateLimit();
        if (!rateLimit.isEnabled()) {
            return false;
        }
        long now = clock.millis();
        long windowMillis = Math.max(1L, rateLimit.getWindow().toMillis());
        WindowCounter counter = counters.compute(sourceIp, (key, current) -> {
            if (current == null || now >= current.windowStartedAtMillis + windowMillis) {
                return new WindowCounter(now);
            }
            return current;
        });
        return counter.count.incrementAndGet() > rateLimit.getMaxRequests();
    }

    /**
     * 标准化 Origin。
     *
     * @param origin 原始 Origin
     * @return 标准化 Origin
     */
    private String normalizeOrigin(String origin) {
        if (!StringUtils.hasText(origin)) {
            return "";
        }
        String normalized = origin.strip().toLowerCase(Locale.ROOT);
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * 固定窗口计数器。
     *
     * @param windowStartedAtMillis 窗口开始时间
     * @param count                 当前窗口请求数
     */
    private record WindowCounter(long windowStartedAtMillis, AtomicInteger count) {

        /**
         * 创建固定窗口计数器。
         *
         * @param windowStartedAtMillis 窗口开始时间
         */
        private WindowCounter(long windowStartedAtMillis) {
            this(windowStartedAtMillis, new AtomicInteger());
        }
    }

    /**
     * 可重复读取的请求体包装器。
     */
    private static class CachedBodyRequest extends HttpServletRequestWrapper {

        /**
         * 缓存请求体。
         */
        private final byte[] body;

        /**
         * 创建缓存请求体包装器。
         *
         * @param request 原始 HTTP 请求
         * @param body    缓存请求体
         */
        CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body.clone();
        }

        /**
         * 返回缓存请求体输入流。
         *
         * @return Servlet 输入流
         */
        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // 缓存请求体已在过滤器中同步读取完成。
                }

                @Override
                public int read() {
                    return input.read();
                }
            };
        }

        /**
         * 返回缓存请求体字符读取器。
         *
         * @return 字符读取器
         */
        @Override
        public BufferedReader getReader() {
            Charset charset = getCharacterEncoding() == null
                    ? StandardCharsets.UTF_8
                    : Charset.forName(getCharacterEncoding());
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }
}
