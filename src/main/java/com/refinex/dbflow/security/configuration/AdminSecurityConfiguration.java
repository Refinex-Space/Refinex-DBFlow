package com.refinex.dbflow.security.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.refinex.dbflow.admin.service.AdminSessionViewService;
import com.refinex.dbflow.common.ApiResult;
import com.refinex.dbflow.common.ErrorCode;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.csrf.*;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
     * XHR 请求头名称。
     */
    private static final String REQUESTED_WITH_HEADER = "X-Requested-With";

    /**
     * XHR 请求头值。
     */
    private static final String XML_HTTP_REQUEST = "XMLHttpRequest";

    /**
     * 判断请求是否显式接受 JSON 响应。
     *
     * @param request HTTP 请求
     * @return 是否接受 JSON
     */
    private static boolean acceptsJson(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return (StringUtils.hasText(accept) && accept.contains(MediaType.APPLICATION_JSON_VALUE))
                || XML_HTTP_REQUEST.equalsIgnoreCase(request.getHeader(REQUESTED_WITH_HEADER));
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
     * 写出 JSON 响应。
     *
     * @param response     HTTP 响应
     * @param objectMapper JSON 序列化器
     * @param status       HTTP 状态码
     * @param body         响应体
     * @throws IOException 写入响应异常
     */
    private static void writeJson(HttpServletResponse response, ObjectMapper objectMapper, HttpStatus status,
                                  Object body) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    /**
     * 创建管理端安全过滤链。
     *
     * @param http               HTTP 安全构造器
     * @param objectMapper       JSON 序列化器
     * @param sessionViewService 管理端当前 session 响应服务
     * @return 管理端安全过滤链
     * @throws Exception Spring Security 构建异常
     */
    @Bean
    @Order(ADMIN_SECURITY_ORDER)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http, ObjectMapper objectMapper,
                                                        AdminSessionViewService sessionViewService) throws Exception {
        http.securityMatcher("/admin", "/admin/**", "/admin-legacy", "/admin-legacy/**", "/login", "/logout",
                        "/admin-assets/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/login",
                                "/admin-assets/**",
                                "/admin/assets/**",
                                "/admin/favicon*").permitAll()
                        .requestMatchers("/admin/api/**").hasRole("ADMIN")
                        .requestMatchers("/admin-legacy", "/admin-legacy/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/admin").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception.authenticationEntryPoint(new AdminAuthenticationEntryPoint()))
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(new JsonAwareAuthenticationSuccessHandler(objectMapper, sessionViewService))
                        .failureHandler(new JsonAwareAuthenticationFailureHandler(objectMapper))
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(new JsonAwareLogoutSuccessHandler(objectMapper)))
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
     * JSON 感知的登录成功处理器。
     */
    private static final class JsonAwareAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

        /**
         * 非 JSON 登录成功委托处理器。
         */
        private final AuthenticationSuccessHandler redirectHandler;

        /**
         * JSON 序列化器。
         */
        private final ObjectMapper objectMapper;

        /**
         * 管理端当前 session 响应服务。
         */
        private final AdminSessionViewService sessionViewService;

        /**
         * 创建 JSON 感知的登录成功处理器。
         *
         * @param objectMapper       JSON 序列化器
         * @param sessionViewService 管理端当前 session 响应服务
         */
        private JsonAwareAuthenticationSuccessHandler(ObjectMapper objectMapper,
                                                      AdminSessionViewService sessionViewService) {
            SimpleUrlAuthenticationSuccessHandler handler = new SimpleUrlAuthenticationSuccessHandler("/admin");
            handler.setAlwaysUseDefaultTargetUrl(true);
            this.redirectHandler = handler;
            this.objectMapper = objectMapper;
            this.sessionViewService = sessionViewService;
        }

        /**
         * 处理登录成功响应。
         *
         * @param request        HTTP 请求
         * @param response       HTTP 响应
         * @param authentication 当前认证信息
         * @throws IOException      写入响应异常
         * @throws ServletException 委托处理异常
         */
        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                            Authentication authentication) throws IOException, ServletException {
            if (!acceptsJson(request)) {
                redirectHandler.onAuthenticationSuccess(request, response, authentication);
                return;
            }
            writeJson(response, objectMapper, HttpStatus.OK, ApiResult.ok(sessionViewService.current(authentication)));
        }
    }

    /**
     * JSON 感知的登录失败处理器。
     */
    private static final class JsonAwareAuthenticationFailureHandler implements AuthenticationFailureHandler {

        /**
         * 非 JSON 登录失败委托处理器。
         */
        private final AuthenticationFailureHandler redirectHandler =
                new SimpleUrlAuthenticationFailureHandler("/login?error");

        /**
         * JSON 序列化器。
         */
        private final ObjectMapper objectMapper;

        /**
         * 创建 JSON 感知的登录失败处理器。
         *
         * @param objectMapper JSON 序列化器
         */
        private JsonAwareAuthenticationFailureHandler(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        /**
         * 处理登录失败响应。
         *
         * @param request   HTTP 请求
         * @param response  HTTP 响应
         * @param exception 认证异常
         * @throws IOException      写入响应异常
         * @throws ServletException 委托处理异常
         */
        @Override
        public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                            AuthenticationException exception) throws IOException, ServletException {
            if (!acceptsJson(request)) {
                redirectHandler.onAuthenticationFailure(request, response, exception);
                return;
            }
            writeJson(response, objectMapper, HttpStatus.UNAUTHORIZED, ApiResult.failed(ErrorCode.UNAUTHENTICATED));
        }
    }

    /**
     * JSON 感知的退出成功处理器。
     */
    private static final class JsonAwareLogoutSuccessHandler implements LogoutSuccessHandler {

        /**
         * 非 JSON 退出成功委托处理器。
         */
        private final LogoutSuccessHandler redirectHandler;

        /**
         * JSON 序列化器。
         */
        private final ObjectMapper objectMapper;

        /**
         * 创建 JSON 感知的退出成功处理器。
         *
         * @param objectMapper JSON 序列化器
         */
        private JsonAwareLogoutSuccessHandler(ObjectMapper objectMapper) {
            SimpleUrlLogoutSuccessHandler handler = new SimpleUrlLogoutSuccessHandler();
            handler.setDefaultTargetUrl("/login?logout");
            this.redirectHandler = handler;
            this.objectMapper = objectMapper;
        }

        /**
         * 处理退出成功响应。
         *
         * @param request        HTTP 请求
         * @param response       HTTP 响应
         * @param authentication 当前认证信息
         * @throws IOException      写入响应异常
         * @throws ServletException 委托处理异常
         */
        @Override
        public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                    Authentication authentication) throws IOException, ServletException {
            if (!acceptsJson(request)) {
                redirectHandler.onLogoutSuccess(request, response, authentication);
                return;
            }
            writeJson(response, objectMapper, HttpStatus.OK, ApiResult.ok(Map.of("authenticated", false)));
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
