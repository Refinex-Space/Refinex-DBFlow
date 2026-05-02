package com.refinex.dbflow.security.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
import org.springframework.util.StringUtils;

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
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/admin", true).permitAll())
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout"))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()));
        return http.build();
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
