package com.refinex.dbflow.security.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
        http.securityMatcher("/admin/**", "/login", "/logout", "/admin-assets/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/admin-assets/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/admin", true).permitAll())
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout"))
                .csrf(Customizer.withDefaults());
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
}
