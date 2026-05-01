package com.refinex.dbflow.security.configuration;

import com.refinex.dbflow.security.filter.McpBearerTokenAuthenticationFilter;
import com.refinex.dbflow.security.filter.McpEndpointGuardFilter;
import com.refinex.dbflow.security.properties.McpEndpointSecurityProperties;
import com.refinex.dbflow.security.request.McpRequestMetadataExtractor;
import com.refinex.dbflow.security.support.McpSecurityErrorResponseWriter;
import com.refinex.dbflow.security.token.McpTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * MCP endpoint Bearer Token 安全配置。
 *
 * @author refinex
 */
@Configuration(proxyBeanMethods = false)
public class McpSecurityConfiguration {

    /**
     * MCP endpoint 路径。
     */
    private static final String MCP_ENDPOINT = "/mcp";

    /**
     * MCP 安全链顺序，必须早于管理端 session 安全链。
     */
    private static final int MCP_SECURITY_ORDER = 5;

    /**
     * 创建 MCP Bearer Token 安全过滤链。
     *
     * @param http                       HTTP 安全构造器
     * @param tokenService               MCP Token 生命周期服务
     * @param metadataExtractor          MCP 请求元信息提取器
     * @param endpointSecurityProperties MCP endpoint 安全加固配置
     * @param errorResponseWriter        MCP 安全错误响应写入器
     * @return MCP 安全过滤链
     * @throws Exception Spring Security 构建异常
     */
    @Bean
    @Order(MCP_SECURITY_ORDER)
    public SecurityFilterChain mcpSecurityFilterChain(
            HttpSecurity http,
            McpTokenService tokenService,
            McpRequestMetadataExtractor metadataExtractor,
            McpEndpointSecurityProperties endpointSecurityProperties,
            McpSecurityErrorResponseWriter errorResponseWriter
    ) throws Exception {
        McpEndpointGuardFilter endpointGuardFilter = new McpEndpointGuardFilter(
                endpointSecurityProperties,
                metadataExtractor,
                errorResponseWriter
        );
        McpBearerTokenAuthenticationFilter bearerTokenAuthenticationFilter =
                new McpBearerTokenAuthenticationFilter(tokenService, metadataExtractor, errorResponseWriter);
        http.securityMatcher(MCP_ENDPOINT)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) ->
                                errorResponseWriter.unauthorized(response,
                                        metadataExtractor.extract(request).requestId(), "invalid_token"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                errorResponseWriter.forbidden(response,
                                        metadataExtractor.extract(request).requestId(), "FORBIDDEN", "MCP 请求无权访问"))
                )
                .addFilterBefore(endpointGuardFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
