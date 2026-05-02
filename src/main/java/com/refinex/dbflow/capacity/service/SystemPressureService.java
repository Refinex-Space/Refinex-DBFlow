package com.refinex.dbflow.capacity.service;

import com.refinex.dbflow.capacity.properties.CapacityProperties;
import com.refinex.dbflow.executor.datasource.ProjectEnvironmentDataSourceRegistry;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 系统压力判断服务，基于 JVM、Hikari 连接池和容量拒绝信号判断是否需要降级。
 *
 * @author refinex
 */
@Service
public class SystemPressureService {

    /**
     * 目标库数据源注册表。
     */
    private final ProjectEnvironmentDataSourceRegistry dataSourceRegistry;

    /**
     * 容量治理配置。
     */
    private final CapacityProperties properties;

    /**
     * 容量指标服务，部分单元测试中允许不存在。
     */
    private final CapacityMetricsService metricsService;

    /**
     * 近期容量拒绝信号计数。
     */
    private final AtomicInteger rejectionSignals = new AtomicInteger();

    /**
     * 创建系统压力判断服务。
     *
     * @param dataSourceRegistry     目标库数据源注册表
     * @param properties             容量治理配置
     * @param metricsServiceProvider 容量指标服务 provider
     */
    @Autowired
    public SystemPressureService(
            ProjectEnvironmentDataSourceRegistry dataSourceRegistry,
            CapacityProperties properties,
            ObjectProvider<CapacityMetricsService> metricsServiceProvider
    ) {
        this.dataSourceRegistry = Objects.requireNonNull(dataSourceRegistry);
        this.properties = Objects.requireNonNull(properties);
        this.metricsService = metricsServiceProvider.getIfAvailable();
    }

    /**
     * 创建系统压力判断服务，供单元测试直接传入指标服务。
     *
     * @param dataSourceRegistry 目标库数据源注册表
     * @param properties         容量治理配置
     * @param metricsService     容量指标服务，可为空
     */
    SystemPressureService(
            ProjectEnvironmentDataSourceRegistry dataSourceRegistry,
            CapacityProperties properties,
            CapacityMetricsService metricsService
    ) {
        this.dataSourceRegistry = Objects.requireNonNull(dataSourceRegistry);
        this.properties = Objects.requireNonNull(properties);
        this.metricsService = metricsService;
    }

    /**
     * 判断单实例本地是否处于压力态。
     *
     * @return 处于压力态时返回 true
     */
    public boolean localPressure() {
        if (!properties.getPressure().isEnabled()) {
            updateLocalPressure(false);
            return false;
        }
        boolean pressured = jvmMemoryPressure() || rejectionSignals.getAndSet(0) >= 3;
        updateLocalPressure(pressured);
        return pressured;
    }

    /**
     * 查看当前本地压力态，不消费容量拒绝信号。
     *
     * @return 处于压力态时返回 true
     */
    public boolean currentLocalPressure() {
        if (!properties.getPressure().isEnabled()) {
            updateLocalPressure(false);
            return false;
        }
        boolean pressured = jvmMemoryPressure() || rejectionSignals.get() >= 3;
        updateLocalPressure(pressured);
        return pressured;
    }

    /**
     * 判断目标项目环境是否处于压力态。
     *
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @return 处于压力态时返回 true
     */
    public boolean targetPressure(String projectKey, String environmentKey) {
        if (!properties.getPressure().isEnabled()
                || !StringUtils.hasText(projectKey)
                || !StringUtils.hasText(environmentKey)) {
            return false;
        }
        try {
            DataSource dataSource = dataSourceRegistry.getDataSource(projectKey, environmentKey);
            if (dataSource instanceof HikariDataSource hikariDataSource) {
                return hikariPressure(hikariDataSource);
            }
            return false;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * 记录容量拒绝信号，供本地压力判断使用。
     */
    public void recordRejectionSignal() {
        rejectionSignals.incrementAndGet();
    }

    /**
     * 清理容量拒绝信号，供测试和后续健康窗口刷新使用。
     */
    public void clearRejectionSignals() {
        rejectionSignals.set(0);
    }

    /**
     * 判断 JVM 内存是否超过压力阈值。
     *
     * @return 超过阈值时返回 true
     */
    private boolean jvmMemoryPressure() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        if (max <= 0L) {
            return false;
        }
        long used = runtime.totalMemory() - runtime.freeMemory();
        double ratio = (double) used / (double) max;
        return ratio >= properties.getPressure().getJvmMemoryUsedRatioThreshold();
    }

    /**
     * 判断 Hikari 连接池是否处于压力态。
     *
     * @param dataSource Hikari 数据源
     * @return 处于压力态时返回 true
     */
    private boolean hikariPressure(HikariDataSource dataSource) {
        HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
        if (pool == null) {
            return false;
        }
        int waiting = pool.getThreadsAwaitingConnection();
        int waitingThreshold = properties.getPressure().getTargetPoolWaitingThreshold();
        if (waitingThreshold > 0 && waiting >= waitingThreshold) {
            return true;
        }
        int total = pool.getTotalConnections();
        if (total <= 0) {
            return false;
        }
        double activeRatio = (double) pool.getActiveConnections() / (double) total;
        return activeRatio >= properties.getPressure().getTargetPoolActiveRatioThreshold();
    }

    /**
     * 更新本地压力指标。
     *
     * @param pressured 是否处于压力态
     */
    private void updateLocalPressure(boolean pressured) {
        if (metricsService != null) {
            metricsService.updateLocalPressure(pressured);
        }
    }
}
