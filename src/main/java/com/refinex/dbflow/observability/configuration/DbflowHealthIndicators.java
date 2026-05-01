package com.refinex.dbflow.observability.configuration;

import com.refinex.dbflow.observability.dto.HealthComponent;
import com.refinex.dbflow.observability.dto.TargetDatasourceHealth;
import com.refinex.dbflow.observability.service.DbflowHealthService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DBFlow Actuator 健康检查指示器集合。
 *
 * @author refinex
 */
@Configuration(proxyBeanMethods = false)
class DbflowHealthIndicators {

    /**
     * 创建元数据库健康指示器。
     *
     * @param healthService DBFlow 运维健康服务
     * @return 元数据库健康指示器
     */
    @Bean
    HealthIndicator dbflowMetadataDatabaseHealthIndicator(DbflowHealthService healthService) {
        return () -> toHealth(healthService.metadataDatabase());
    }

    /**
     * 创建目标数据源注册表健康指示器。
     *
     * @param healthService DBFlow 运维健康服务
     * @return 目标数据源注册表健康指示器
     */
    @Bean
    HealthIndicator dbflowTargetDatasourceRegistryHealthIndicator(DbflowHealthService healthService) {
        return () -> {
            TargetDatasourceHealth targetHealth = healthService.targetDatasourceRegistry();
            Status status = targetHealth.unhealthyTargets() > 0 ? Status.OUT_OF_SERVICE : Status.UP;
            return Health.status(status)
                    .withDetail("configuredTargets", targetHealth.configuredTargets())
                    .withDetail("healthyTargets", targetHealth.healthyTargets())
                    .withDetail("unhealthyTargets", targetHealth.unhealthyTargets())
                    .build();
        };
    }

    /**
     * 创建 Nacos 健康指示器。
     *
     * @param healthService DBFlow 运维健康服务
     * @return Nacos 健康指示器
     */
    @Bean
    HealthIndicator dbflowNacosHealthIndicator(DbflowHealthService healthService) {
        return () -> toHealth(healthService.nacos());
    }

    /**
     * 创建 MCP endpoint readiness 健康指示器。
     *
     * @param healthService DBFlow 运维健康服务
     * @return MCP endpoint readiness 健康指示器
     */
    @Bean
    HealthIndicator dbflowMcpEndpointReadinessHealthIndicator(DbflowHealthService healthService) {
        return () -> toHealth(healthService.mcpEndpointReadiness());
    }

    /**
     * 将 DBFlow 健康项转换为 Actuator Health。
     *
     * @param component 健康项
     * @return Actuator Health
     */
    private Health toHealth(HealthComponent component) {
        return Health.status(toStatus(component.status()))
                .withDetail("name", component.name())
                .withDetail("component", component.component())
                .withDetail("description", component.description())
                .withDetail("detail", component.detail())
                .build();
    }

    /**
     * 将 DBFlow 状态映射到 Actuator 状态。
     *
     * @param status DBFlow 状态
     * @return Actuator 状态
     */
    private Status toStatus(String status) {
        if ("HEALTHY".equalsIgnoreCase(status) || "DISABLED".equalsIgnoreCase(status)) {
            return Status.UP;
        }
        if ("DOWN".equalsIgnoreCase(status)) {
            return Status.DOWN;
        }
        if ("DEGRADED".equalsIgnoreCase(status)) {
            return Status.OUT_OF_SERVICE;
        }
        return Status.UNKNOWN;
    }

}
