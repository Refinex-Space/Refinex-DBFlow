package com.refinex.dbflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * MCP Bearer Token 认证过滤器。
 *
 * @author refinex
 */
public class McpBearerTokenAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Bearer 认证前缀。
     */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Query string access token 参数。
     */
    private static final String ACCESS_TOKEN_QUERY = "access_token=";

    /**
     * Query string token 参数。
     */
    private static final String TOKEN_QUERY = "token=";

    /**
     * MCP Token 生命周期服务。
     */
    private final McpTokenService tokenService;

    /**
     * MCP 请求元信息提取器。
     */
    private final McpRequestMetadataExtractor metadataExtractor;

    /**
     * 时钟。
     */
    private final Clock clock;

    /**
     * MCP 安全错误响应写入器。
     */
    private final McpSecurityErrorResponseWriter errorResponseWriter;

    /**
     * 创建 MCP Bearer Token 认证过滤器。
     *
     * @param tokenService      MCP Token 生命周期服务
     * @param metadataExtractor MCP 请求元信息提取器
     */
    public McpBearerTokenAuthenticationFilter(
            McpTokenService tokenService,
            McpRequestMetadataExtractor metadataExtractor,
            McpSecurityErrorResponseWriter errorResponseWriter
    ) {
        this(tokenService, metadataExtractor, errorResponseWriter, Clock.systemUTC());
    }

    /**
     * 创建 MCP Bearer Token 认证过滤器。
     *
     * @param tokenService      MCP Token 生命周期服务
     * @param metadataExtractor MCP 请求元信息提取器
     * @param errorResponseWriter MCP 安全错误响应写入器
     * @param clock             时钟
     */
    McpBearerTokenAuthenticationFilter(
            McpTokenService tokenService,
            McpRequestMetadataExtractor metadataExtractor,
            McpSecurityErrorResponseWriter errorResponseWriter,
            Clock clock
    ) {
        this.tokenService = tokenService;
        this.metadataExtractor = metadataExtractor;
        this.errorResponseWriter = errorResponseWriter;
        this.clock = clock;
    }

    /**
     * 对每个 MCP HTTP 请求执行 Bearer Token 认证。
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
        if (hasQueryStringToken(request)) {
            writeUnauthorized(response, metadata, "invalid_request");
            return;
        }

        Optional<String> bearerToken = resolveBearerToken(request);
        if (bearerToken.isEmpty()) {
            writeUnauthorized(response, metadata, "invalid_token");
            return;
        }

        Optional<McpTokenValidationResult> validationResult =
                tokenService.validateToken(bearerToken.orElseThrow(), Instant.now(clock));
        if (validationResult.isEmpty()) {
            writeUnauthorized(response, metadata, "invalid_token");
            return;
        }

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(McpAuthenticationToken.authenticated(validationResult.orElseThrow(), metadata));
        SecurityContextHolder.setContext(securityContext);
        filterChain.doFilter(request, response);
    }

    /**
     * Streamable HTTP 使用异步分发，异步线程也必须重新建立认证上下文。
     *
     * @return false 表示异步分发不跳过本过滤器
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    /**
     * 判断 query string 是否携带 Token。
     *
     * @param request HTTP 请求
     * @return 携带 query string token 时返回 true
     */
    private boolean hasQueryStringToken(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (!StringUtils.hasText(queryString)) {
            return false;
        }
        String[] parameters = queryString.split("&");
        for (String parameter : parameters) {
            String parameterName = parameter.split("=", 2)[0].toLowerCase();
            if (ACCESS_TOKEN_QUERY.substring(0, ACCESS_TOKEN_QUERY.length() - 1).equals(parameterName)
                    || TOKEN_QUERY.substring(0, TOKEN_QUERY.length() - 1).equals(parameterName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析 Authorization Bearer Token。
     *
     * @param request HTTP 请求
     * @return 明文 Token；缺失或格式非法时为空
     */
    private Optional<String> resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, 7)) {
            return Optional.empty();
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token) || token.contains(" ")) {
            return Optional.empty();
        }
        return Optional.of(token);
    }

    /**
     * 写入 401 响应；响应中不包含 Token 明文。
     *
     * @param response HTTP 响应
     * @param metadata MCP 请求元信息
     * @param error    Bearer error code
     * @throws IOException IO 处理异常
     */
    private void writeUnauthorized(
            HttpServletResponse response,
            McpRequestMetadata metadata,
            String error
    ) throws IOException {
        errorResponseWriter.unauthorized(response, metadata.requestId(), error);
    }
}
