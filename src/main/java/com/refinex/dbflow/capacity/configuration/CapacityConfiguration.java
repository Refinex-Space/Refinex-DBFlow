package com.refinex.dbflow.capacity.configuration;

import com.refinex.dbflow.capacity.properties.CapacityProperties;
import com.refinex.dbflow.capacity.support.InMemoryWindowRateLimiter;
import com.refinex.dbflow.capacity.support.SemaphoreBulkheadRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 容量治理配置入口，确保 DBFlow 容量配置属性被 Spring 容器识别。
 *
 * @author refinex
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CapacityProperties.class)
public class CapacityConfiguration {

    /**
     * 创建进程内固定窗口限流器。
     *
     * @return 进程内固定窗口限流器
     */
    @Bean
    public InMemoryWindowRateLimiter inMemoryWindowRateLimiter() {
        return new InMemoryWindowRateLimiter();
    }

    /**
     * 创建信号量并发舱壁注册表。
     *
     * @return 信号量并发舱壁注册表
     */
    @Bean
    public SemaphoreBulkheadRegistry semaphoreBulkheadRegistry() {
        return new SemaphoreBulkheadRegistry();
    }
}
