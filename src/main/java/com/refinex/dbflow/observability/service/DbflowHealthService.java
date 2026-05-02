package com.refinex.dbflow.observability.service;

import com.refinex.dbflow.access.dto.ConfiguredEnvironmentView;
import com.refinex.dbflow.access.service.ProjectEnvironmentCatalogService;
import com.refinex.dbflow.capacity.model.McpToolClass;
import com.refinex.dbflow.capacity.properties.CapacityProperties;
import com.refinex.dbflow.capacity.service.SystemPressureService;
import com.refinex.dbflow.common.util.SensitiveTextSanitizer;
import com.refinex.dbflow.executor.datasource.ProjectEnvironmentDataSourceRegistry;
import com.refinex.dbflow.observability.dto.HealthComponent;
import com.refinex.dbflow.observability.dto.HealthSnapshot;
import com.refinex.dbflow.observability.dto.TargetDatasourceHealth;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * DBFlow 运维健康服务，供管理端页面和 Actuator health indicator 复用同一份健康判断。
 *
 * @author refinex
 */
@Service
public class DbflowHealthService {

    /**
     * 元数据库数据源。
     */
    private final DataSource metadataDataSource;

    /**
     * 项目环境目录服务。
     */
    private final ProjectEnvironmentCatalogService catalogService;

    /**
     * 目标库数据源注册表。
     */
    private final ProjectEnvironmentDataSourceRegistry targetRegistry;

    /**
     * 容量治理配置。
     */
    private final CapacityProperties capacityProperties;

    /**
     * 系统压力判断服务。
     */
    private final SystemPressureService pressureService;

    /**
     * Spring 环境属性。
     */
    private final Environment springEnvironment;

    /**
     * 创建 DBFlow 运维健康服务。
     *
     * @param metadataDataSource 元数据库数据源
     * @param catalogService     项目环境目录服务
     * @param targetRegistry     目标库数据源注册表
     * @param capacityProperties 容量治理配置
     * @param pressureService    系统压力判断服务
     * @param springEnvironment  Spring 环境属性
     */
    public DbflowHealthService(
            DataSource metadataDataSource,
            ProjectEnvironmentCatalogService catalogService,
            ProjectEnvironmentDataSourceRegistry targetRegistry,
            CapacityProperties capacityProperties,
            SystemPressureService pressureService,
            Environment springEnvironment
    ) {
        this.metadataDataSource = Objects.requireNonNull(metadataDataSource);
        this.catalogService = Objects.requireNonNull(catalogService);
        this.targetRegistry = Objects.requireNonNull(targetRegistry);
        this.capacityProperties = Objects.requireNonNull(capacityProperties);
        this.pressureService = Objects.requireNonNull(pressureService);
        this.springEnvironment = Objects.requireNonNull(springEnvironment);
    }

    /**
     * 创建完整健康快照。
     *
     * @return 健康快照
     */
    public HealthSnapshot snapshot() {
        List<HealthComponent> items = new ArrayList<>();
        items.add(applicationHealth());
        items.add(mcpEndpointReadiness());
        items.add(metadataDatabase());
        items.add(nacos());
        TargetDatasourceHealth targetHealth = targetDatasourceRegistry();
        items.add(capacityHealth(targetHealth.components()));
        items.addAll(targetHealth.components());
        long unhealthy = items.stream().filter(HealthComponent::unhealthy).count();
        String overall = unhealthy == 0 ? "HEALTHY" : "DEGRADED";
        return new HealthSnapshot(overall, toneForHealth(overall), items.size(), unhealthy, List.copyOf(items));
    }

    /**
     * 创建应用进程健康项。
     *
     * @return 健康项
     */
    public HealthComponent applicationHealth() {
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMb = runtime.maxMemory() / 1024 / 1024;
        return new HealthComponent("应用进程", "runtime", "HEALTHY", "Spring Boot 管理端进程可用",
                "JVM memory used=" + usedMb + "MB max=" + maxMb + "MB", "ok");
    }

    /**
     * 创建 MCP endpoint readiness 健康项。
     *
     * @return 健康项
     */
    public HealthComponent mcpEndpointReadiness() {
        boolean enabled = springEnvironment.getProperty("spring.ai.mcp.server.enabled", Boolean.class, true);
        String endpoint = springEnvironment.getProperty(
                "spring.ai.mcp.server.streamable-http.mcp-endpoint", "/mcp");
        String version = springEnvironment.getProperty("spring.ai.mcp.server.version", "unknown");
        String status = enabled ? "HEALTHY" : "DISABLED";
        return new HealthComponent("MCP Streamable HTTP", "mcp", status,
                enabled ? endpoint + " endpoint 已配置" : "MCP server 当前禁用",
                "version=" + version + " endpoint=" + endpoint, toneForHealth(status));
    }

    /**
     * 创建元数据库健康项。
     *
     * @return 健康项
     */
    public HealthComponent metadataDatabase() {
        try (Connection connection = metadataDataSource.getConnection()) {
            boolean valid = connection.isValid(1);
            DatabaseMetaData metaData = connection.getMetaData();
            String status = valid ? "HEALTHY" : "DEGRADED";
            return new HealthComponent("元数据库", "metadata-db", status,
                    metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion(),
                    "schema managed by Flyway; credentials hidden", toneForHealth(status));
        } catch (Exception exception) {
            return new HealthComponent("元数据库", "metadata-db", "DOWN", "元数据库连接失败",
                    sanitize(exception.getMessage()), "bad");
        }
    }

    /**
     * 创建 Nacos 健康项。
     *
     * @return 健康项
     */
    public HealthComponent nacos() {
        boolean configEnabled = springEnvironment.getProperty("spring.cloud.nacos.config.enabled", Boolean.class, false);
        boolean discoveryEnabled = springEnvironment.getProperty("spring.cloud.nacos.discovery.enabled", Boolean.class, false);
        String status = configEnabled || discoveryEnabled ? "HEALTHY" : "DISABLED";
        String namespace = springEnvironment.getProperty("spring.cloud.nacos.config.namespace", "default");
        return new HealthComponent("Nacos", "nacos", status,
                "config=" + enabledText(configEnabled) + " discovery=" + enabledText(discoveryEnabled),
                "namespace=" + namespace + "; credentials hidden", toneForHealth(status));
    }

    /**
     * 创建容量治理健康项。
     *
     * @param targetComponents 目标连接池健康项
     * @return 健康项
     */
    public HealthComponent capacityHealth(List<HealthComponent> targetComponents) {
        if (!capacityProperties.isEnabled()) {
            return new HealthComponent("容量治理", "capacity", "DISABLED",
                    "容量治理已关闭", "dbflow.capacity.enabled=false", "neutral");
        }
        boolean localPressure = pressureService.currentLocalPressure();
        long busyTargets = targetComponents.stream()
                .filter(component -> "target-pool".equals(component.component()))
                .filter(component -> "BUSY".equalsIgnoreCase(component.status()))
                .count();
        String status = localPressure || busyTargets > 0 ? "BUSY" : "HEALTHY";
        String detail = "localPressure=" + localPressure
                + " targetBusy=" + busyTargets
                + " globalMaxConcurrent=" + capacityProperties.getBulkhead().getGlobalMaxConcurrent()
                + " executeMaxConcurrent="
                + capacityProperties.getBulkhead().getClasses()
                .get(McpToolClass.EXECUTE)
                .getMaxConcurrent();
        return new HealthComponent("容量治理", "capacity", status,
                status.equals("BUSY") ? "容量治理检测到压力态" : "容量治理已启用且未检测到压力态",
                detail, toneForHealth(status));
    }

    /**
     * 创建目标数据源注册表健康分组。
     *
     * @return 目标数据源健康分组
     */
    public TargetDatasourceHealth targetDatasourceRegistry() {
        List<HealthComponent> items = new ArrayList<>();
        List<ConfiguredEnvironmentView> environments = catalogService.listConfiguredEnvironments();
        if (environments.isEmpty()) {
            items.add(new HealthComponent("项目环境连接池", "target-pool", "DISABLED",
                    "当前未配置目标项目环境", "dbflow.projects 为空", "neutral"));
            return new TargetDatasourceHealth(0, 0, 0, items);
        }
        for (ConfiguredEnvironmentView environmentView : environments) {
            items.add(targetPoolHealth(environmentView));
        }
        long unhealthy = items.stream().filter(HealthComponent::unhealthy).count();
        return new TargetDatasourceHealth(environments.size(), environments.size() - unhealthy, unhealthy, List.copyOf(items));
    }

    /**
     * 创建单个目标连接池健康项。
     *
     * @param environmentView 项目环境视图
     * @return 健康项
     */
    private HealthComponent targetPoolHealth(ConfiguredEnvironmentView environmentView) {
        String name = environmentView.projectKey() + " / " + environmentView.environmentKey();
        try {
            DataSource dataSource = targetRegistry.getDataSource(
                    environmentView.projectKey(),
                    environmentView.environmentKey()
            );
            if (dataSource instanceof HikariDataSource hikariDataSource) {
                String status = targetPoolStatus(hikariDataSource);
                return new HealthComponent(name, "target-pool", status,
                        "Hikari 连接池已注册，driver=" + displayText(environmentView.driverClassName()),
                        hikariDetail(hikariDataSource), toneForHealth(status));
            }
            return new HealthComponent(name, "target-pool", "HEALTHY",
                    "目标数据源已注册", "type=" + dataSource.getClass().getSimpleName(), "ok");
        } catch (RuntimeException exception) {
            return new HealthComponent(name, "target-pool", "DEGRADED",
                    "目标数据源未就绪或不可用", sanitize(exception.getMessage()), "warn");
        }
    }

    /**
     * 创建 Hikari 连接池详情文本。
     *
     * @param dataSource Hikari 数据源
     * @return 脱敏详情文本
     */
    private String hikariDetail(HikariDataSource dataSource) {
        HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
        if (pool == null) {
            return "pool=" + dataSource.getPoolName() + " metrics=not-started";
        }
        return "pool=" + dataSource.getPoolName()
                + " active=" + pool.getActiveConnections()
                + " idle=" + pool.getIdleConnections()
                + " total=" + pool.getTotalConnections()
                + " waiting=" + pool.getThreadsAwaitingConnection()
                + " pressure=" + hikariPressure(pool);
    }

    /**
     * 创建目标连接池健康状态。
     *
     * @param dataSource Hikari 数据源
     * @return 健康状态
     */
    private String targetPoolStatus(HikariDataSource dataSource) {
        if (dataSource.isClosed()) {
            return "DOWN";
        }
        HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
        if (pool != null && hikariPressure(pool)) {
            return "BUSY";
        }
        return "HEALTHY";
    }

    /**
     * 判断 Hikari 连接池是否达到容量压力阈值。
     *
     * @param pool Hikari 连接池 MXBean
     * @return 达到压力阈值时返回 true
     */
    private boolean hikariPressure(HikariPoolMXBean pool) {
        int waitingThreshold = capacityProperties.getPressure().getTargetPoolWaitingThreshold();
        if (waitingThreshold > 0 && pool.getThreadsAwaitingConnection() >= waitingThreshold) {
            return true;
        }
        int total = pool.getTotalConnections();
        if (total <= 0) {
            return false;
        }
        double activeRatio = (double) pool.getActiveConnections() / (double) total;
        return activeRatio >= capacityProperties.getPressure().getTargetPoolActiveRatioThreshold();
    }

    /**
     * 转换健康状态色调。
     *
     * @param status 健康状态
     * @return 色调
     */
    public String toneForHealth(String status) {
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "HEALTHY" -> "ok";
            case "DISABLED" -> "neutral";
            case "DOWN" -> "bad";
            case "BUSY" -> "warn";
            default -> "warn";
        };
    }

    /**
     * 判断状态是否应计入不健康。
     *
     * @param status 健康状态
     * @return 应计入不健康时返回 true
     */
    public boolean unhealthyStatus(String status) {
        return "DEGRADED".equalsIgnoreCase(status) || "DOWN".equalsIgnoreCase(status);
    }

    /**
     * 转换布尔状态文本。
     *
     * @param enabled 是否启用
     * @return 展示文本
     */
    private String enabledText(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }

    /**
     * 转换展示文本。
     *
     * @param value 原始对象
     * @return 展示文本
     */
    private String displayText(Object value) {
        return value == null || !StringUtils.hasText(value.toString()) ? "-" : value.toString();
    }

    /**
     * 对健康错误文本做连接串级脱敏。
     *
     * @param message 原始消息
     * @return 脱敏消息
     */
    private String sanitize(String message) {
        if (!StringUtils.hasText(message)) {
            return "无详细错误";
        }
        return SensitiveTextSanitizer.sanitize(message);
    }
}
