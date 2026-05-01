package com.refinex.dbflow.security.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Actuator 最小暴露面安全配置。
 *
 * @author refinex
 */
@Configuration(proxyBeanMethods = false)
public class ActuatorSecurityConfiguration {

    /**
     * Actuator 安全链顺序，位于 MCP 与管理端之间。
     */
    private static final int ACTUATOR_SECURITY_ORDER = 8;

    /**
     * 创建 Actuator 安全过滤链。
     *
     * @param http HTTP 安全构造器
     * @return Actuator 安全过滤链
     * @throws Exception Spring Security 构建异常
     */
    @Bean
    @Order(ACTUATOR_SECURITY_ORDER)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/actuator/**")
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/metrics", "/actuator/metrics/**").permitAll()
                        .anyRequest().denyAll()
                );
        return http.build();
    }
}
