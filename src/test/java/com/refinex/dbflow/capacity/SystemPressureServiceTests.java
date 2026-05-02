package com.refinex.dbflow.capacity;

import com.refinex.dbflow.capacity.properties.CapacityProperties;
import com.refinex.dbflow.capacity.service.CapacityMetricsService;
import com.refinex.dbflow.capacity.service.SystemPressureService;
import com.refinex.dbflow.executor.datasource.ProjectEnvironmentDataSourceRegistry;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 系统压力判断服务测试。
 *
 * @author refinex
 */
class SystemPressureServiceTests {

    /**
     * 验证 Hikari 等待线程达到阈值时触发目标压力态，且不获取目标连接。
     */
    @Test
    void shouldDetectTargetPressureFromWaitingThreads() throws Exception {
        HikariDataSource dataSource = mock(HikariDataSource.class);
        HikariPoolMXBean pool = mock(HikariPoolMXBean.class);
        when(dataSource.getHikariPoolMXBean()).thenReturn(pool);
        when(pool.getThreadsAwaitingConnection()).thenReturn(1);
        ProjectEnvironmentDataSourceRegistry registry = (projectKey, environmentKey) -> dataSource;

        SystemPressureService service = new SystemPressureService(registry, properties(), provider());

        assertThat(service.targetPressure("demo", "dev")).isTrue();
        org.mockito.Mockito.verify(dataSource, org.mockito.Mockito.never()).getConnection();
    }

    /**
     * 验证 Hikari active ratio 达到阈值时触发目标压力态。
     */
    @Test
    void shouldDetectTargetPressureFromActiveRatio() {
        HikariDataSource dataSource = mock(HikariDataSource.class);
        HikariPoolMXBean pool = mock(HikariPoolMXBean.class);
        when(dataSource.getHikariPoolMXBean()).thenReturn(pool);
        when(pool.getThreadsAwaitingConnection()).thenReturn(0);
        when(pool.getTotalConnections()).thenReturn(10);
        when(pool.getActiveConnections()).thenReturn(9);
        ProjectEnvironmentDataSourceRegistry registry = (projectKey, environmentKey) -> dataSource;

        SystemPressureService service = new SystemPressureService(registry, properties(), provider());

        assertThat(service.targetPressure("demo", "dev")).isTrue();
    }

    /**
     * 验证未达到 Hikari 阈值时不触发目标压力态。
     */
    @Test
    void shouldNotDetectTargetPressureWhenPoolIsAvailable() {
        HikariDataSource dataSource = mock(HikariDataSource.class);
        HikariPoolMXBean pool = mock(HikariPoolMXBean.class);
        when(dataSource.getHikariPoolMXBean()).thenReturn(pool);
        when(pool.getThreadsAwaitingConnection()).thenReturn(0);
        when(pool.getTotalConnections()).thenReturn(10);
        when(pool.getActiveConnections()).thenReturn(2);
        ProjectEnvironmentDataSourceRegistry registry = (projectKey, environmentKey) -> dataSource;

        SystemPressureService service = new SystemPressureService(registry, properties(), provider());

        assertThat(service.targetPressure("demo", "dev")).isFalse();
    }

    /**
     * 验证压力判断关闭后不会触发本地或目标压力态。
     */
    @Test
    void shouldStayHealthyWhenPressureDetectionDisabled() throws Exception {
        CapacityProperties properties = properties();
        properties.getPressure().setEnabled(false);
        HikariDataSource dataSource = mock(HikariDataSource.class);
        HikariPoolMXBean pool = mock(HikariPoolMXBean.class);
        when(dataSource.getHikariPoolMXBean()).thenReturn(pool);
        when(pool.getThreadsAwaitingConnection()).thenReturn(99);
        ProjectEnvironmentDataSourceRegistry registry = (projectKey, environmentKey) -> dataSource;

        SystemPressureService service = new SystemPressureService(registry, properties, provider());
        service.recordRejectionSignal();
        service.recordRejectionSignal();
        service.recordRejectionSignal();

        assertThat(service.localPressure()).isFalse();
        assertThat(service.targetPressure("demo", "dev")).isFalse();
        org.mockito.Mockito.verify(dataSource, org.mockito.Mockito.never()).getConnection();
    }

    /**
     * 验证连续容量拒绝信号可触发一次本地压力态。
     */
    @Test
    void shouldDetectLocalPressureFromRejectionSignals() {
        SystemPressureService service = new SystemPressureService(
                (projectKey, environmentKey) -> {
                    throw new AssertionError("不应访问目标数据源");
                },
                properties(),
                provider()
        );

        service.recordRejectionSignal();
        service.recordRejectionSignal();
        service.recordRejectionSignal();

        assertThat(service.localPressure()).isTrue();
        assertThat(service.localPressure()).isFalse();
    }

    /**
     * 创建默认容量配置。
     *
     * @return 默认容量配置
     */
    private CapacityProperties properties() {
        CapacityProperties properties = new CapacityProperties();
        properties.afterPropertiesSet();
        return properties;
    }

    /**
     * 创建空指标 provider。
     *
     * @return 空指标 provider
     */
    @SuppressWarnings("unchecked")
    private ObjectProvider<CapacityMetricsService> provider() {
        ObjectProvider<CapacityMetricsService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }
}
