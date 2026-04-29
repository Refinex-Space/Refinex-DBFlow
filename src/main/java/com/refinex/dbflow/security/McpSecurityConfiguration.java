package com.refinex.dbflow.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
     * @param http              HTTP 安全构造器
     * @param tokenService      MCP Token 生命周期服务
     * @param metadataExtractor MCP 请求元信息提取器
     * @return MCP 安全过滤链
     * @throws Exception Spring Security 构建异常
     */
    @Bean
    @Order(MCP_SECURITY_ORDER)
    public SecurityFilterChain mcpSecurityFilterChain(
            HttpSecurity http,
            McpTokenService tokenService,
            McpRequestMetadataExtractor metadataExtractor
    ) throws Exception {
        McpBearerTokenAuthenticationFilter bearerTokenAuthenticationFilter =
                new McpBearerTokenAuthenticationFilter(tokenService, metadataExtractor);
        http.securityMatcher(MCP_ENDPOINT)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"error\":\"unauthorized\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"error\":\"forbidden\"}");
                        })
                )
                .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
