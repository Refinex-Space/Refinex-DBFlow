package com.refinex.dbflow.security.configuration;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.*;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * 管理端 session 安全配置，独立于后续 MCP Bearer Token 安全链路。
 *
 * @author refinex
 */
@Configuration(proxyBeanMethods = false)
public class AdminSecurityConfiguration {

    /**
     * 管理端安全链顺序。
     */
    private static final int ADMIN_SECURITY_ORDER = 10;

    /**
     * 管理端 API 路径前缀。
     */
    private static final String ADMIN_API_PREFIX = "/admin/api/";

    /**
     * 判断请求是否显式接受 JSON 响应。
     *
     * @param request HTTP 请求
     * @return 是否接受 JSON
     */
    private static boolean acceptsJson(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return StringUtils.hasText(accept) && accept.contains(MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * 创建 BCrypt 密码编码器。
     *
     * @return BCrypt 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 判断请求是否命中管理端 API。
     *
     * @param request HTTP 请求
     * @return 是否管理端 API 请求
     */
    private static boolean isAdminApiRequest(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && requestPath.startsWith(contextPath)) {
            requestPath = requestPath.substring(contextPath.length());
        }
        return requestPath.startsWith(ADMIN_API_PREFIX);
    }

    /**
     * 创建管理端安全过滤链。
     *
     * @param http HTTP 安全构造器
     * @return 管理端安全过滤链
     * @throws Exception Spring Security 构建异常
     */
    @Bean
    @Order(ADMIN_SECURITY_ORDER)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/admin/**", "/admin-next", "/admin-next/**", "/login", "/logout", "/admin-assets/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/login",
                                "/admin-assets/**",
                                "/admin-next",
                                "/admin-next/",
                                "/admin-next/index.html",
                                "/admin-next/assets/**",
                                "/admin-next/favicon*").permitAll()
                        .requestMatchers("/admin/api/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/admin-next/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception.authenticationEntryPoint(new AdminAuthenticationEntryPoint()))
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/admin", true).permitAll())
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout"))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()));
        return http.build();
    }

    /**
     * 管理端未认证响应入口，按请求类型分流 API 与页面。
     */
    private static final class AdminAuthenticationEntryPoint implements AuthenticationEntryPoint {

        /**
         * 表单登录页面入口。
         */
        private final AuthenticationEntryPoint loginEntryPoint = new LoginUrlAuthenticationEntryPoint("/login");

        /**
         * 未认证时为 React API 返回 JSON 401，为传统页面保留登录跳转。
         *
         * @param request       HTTP 请求
         * @param response      HTTP 响应
         * @param authException 认证异常
         * @throws ServletException 委托登录入口异常
         * @throws IOException      写入响应异常
         */
        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response,
                             AuthenticationException authException) throws ServletException, IOException {
            if (!isAdminApiRequest(request) || !acceptsJson(request)) {
                loginEntryPoint.commence(request, response, authException);
                return;
            }
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"authenticated\":false,\"error\":\"UNAUTHENTICATED\"}");
        }
    }

    /**
     * 同时支持 React SPA header token 和 Thymeleaf hidden token 的 CSRF 请求处理器。
     */
    private static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

        /**
         * 使用原始 token 解析 SPA 请求头。
         */
        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();

        /**
         * 使用 XOR token 保护服务端渲染页面中的隐藏字段。
         */
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        /**
         * 处理 CSRF token 暴露。
         *
         * @param request   HTTP 请求
         * @param response  HTTP 响应
         * @param csrfToken 延迟加载的 CSRF token
         */
        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
            this.xor.handle(request, response, csrfToken);
            csrfToken.get();
        }

        /**
         * 解析请求提交的 CSRF token。
         *
         * @param request   HTTP 请求
         * @param csrfToken 服务端保存的 CSRF token
         * @return 请求中的 CSRF token 值
         */
        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            String headerValue = request.getHeader(csrfToken.getHeaderName());
            return (StringUtils.hasText(headerValue) ? this.plain : this.xor)
                    .resolveCsrfTokenValue(request, csrfToken);
        }
    }
}
