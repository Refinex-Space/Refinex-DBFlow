package com.refinex.dbflow.capacity;

import com.refinex.dbflow.capacity.model.McpToolClass;
import com.refinex.dbflow.capacity.properties.CapacityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 容量治理配置绑定与启动期校验测试。
 *
 * @author refinex
 */
class CapacityPropertiesTests {

    /**
     * 轻量配置上下文运行器，仅加载容量配置属性绑定。
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    /**
     * 断言启动失败信息包含指定片段。
     *
     * @param failure         启动失败异常
     * @param expectedMessage 预期错误信息片段
     */
    private static void assertStartupFailureContains(Throwable failure, String expectedMessage) {
        assertThat(failure).isNotNull();
        assertThat(failure).hasMessageContaining(expectedMessage);
    }

    /**
     * 验证容量治理默认值符合企业单实例设计基线。
     */
    @Test
    void shouldProvideEnterpriseCapacityDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CapacityProperties.class);
            CapacityProperties properties = context.getBean(CapacityProperties.class);

            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getPressure().isEnabled()).isTrue();
            assertThat(properties.getPressure().getTargetPoolWaitingThreshold()).isEqualTo(1);
            assertThat(properties.getPressure().getTargetPoolActiveRatioThreshold()).isEqualTo(0.85D);
            assertThat(properties.getPressure().getJvmMemoryUsedRatioThreshold()).isEqualTo(0.85D);
            assertThat(properties.getRateLimit().getPerToken().getMaxRequests()).isEqualTo(60);
            assertThat(properties.getRateLimit().getPerToken().getWindow()).isEqualTo(Duration.ofMinutes(1));
            assertThat(properties.getRateLimit().getPerUser().getMaxRequests()).isEqualTo(120);
            assertThat(properties.getRateLimit().getPerTool().get(McpToolClass.LIGHT_READ).getMaxRequests())
                    .isEqualTo(180);
            assertThat(properties.getRateLimit().getPerTool().get(McpToolClass.HEAVY_READ).getMaxRequests())
                    .isEqualTo(60);
            assertThat(properties.getRateLimit().getPerTool().get(McpToolClass.EXPLAIN).getMaxRequests())
                    .isEqualTo(60);
            assertThat(properties.getRateLimit().getPerTool().get(McpToolClass.EXECUTE).getMaxRequests())
                    .isEqualTo(30);
            assertThat(properties.getBulkhead().getGlobalMaxConcurrent()).isEqualTo(80);
            assertThat(properties.getBulkhead().getClasses().get(McpToolClass.LIGHT_READ).getMaxConcurrent())
                    .isEqualTo(40);
            assertThat(properties.getBulkhead().getClasses().get(McpToolClass.EXECUTE).getAcquireTimeout())
                    .isEqualTo(Duration.ZERO);
            assertThat(properties.getBulkhead().getPerTokenMaxConcurrent()).isEqualTo(4);
            assertThat(properties.getBulkhead().getPerUserMaxConcurrent()).isEqualTo(8);
            assertThat(properties.getBulkhead().getPerTargetMaxConcurrent()).isEqualTo(6);
            assertThat(properties.getDegradation().getHeavyReadMaxItemsUnderPressure()).isEqualTo(50);
            assertThat(properties.getDegradation().isRejectExplainUnderPressure()).isTrue();
            assertThat(properties.getDegradation().isRejectExecuteUnderPressure()).isTrue();
        });
    }

    /**
     * 验证显式配置可以覆盖容量治理默认值。
     */
    @Test
    void shouldBindCapacityOverrides() {
        contextRunner.withPropertyValues(
                "dbflow.capacity.enabled=false",
                "dbflow.capacity.pressure.enabled=false",
                "dbflow.capacity.pressure.target-pool-waiting-threshold=3",
                "dbflow.capacity.pressure.target-pool-active-ratio-threshold=0.75",
                "dbflow.capacity.pressure.jvm-memory-used-ratio-threshold=0.70",
                "dbflow.capacity.rate-limit.per-token.max-requests=11",
                "dbflow.capacity.rate-limit.per-token.window=30s",
                "dbflow.capacity.rate-limit.per-user.max-requests=22",
                "dbflow.capacity.rate-limit.per-tool.EXECUTE.max-requests=7",
                "dbflow.capacity.rate-limit.per-tool.EXECUTE.window=10s",
                "dbflow.capacity.bulkhead.global-max-concurrent=33",
                "dbflow.capacity.bulkhead.acquire-timeout=25ms",
                "dbflow.capacity.bulkhead.classes.EXPLAIN.max-concurrent=5",
                "dbflow.capacity.bulkhead.classes.EXPLAIN.acquire-timeout=15ms",
                "dbflow.capacity.bulkhead.per-token-max-concurrent=2",
                "dbflow.capacity.bulkhead.per-user-max-concurrent=3",
                "dbflow.capacity.bulkhead.per-target-max-concurrent=4",
                "dbflow.capacity.degradation.enabled=false",
                "dbflow.capacity.degradation.heavy-read-max-items-under-pressure=25",
                "dbflow.capacity.degradation.reject-explain-under-pressure=false",
                "dbflow.capacity.degradation.reject-execute-under-pressure=false"
        ).run(context -> {
            CapacityProperties properties = context.getBean(CapacityProperties.class);

            assertThat(properties.isEnabled()).isFalse();
            assertThat(properties.getPressure().isEnabled()).isFalse();
            assertThat(properties.getPressure().getTargetPoolWaitingThreshold()).isEqualTo(3);
            assertThat(properties.getPressure().getTargetPoolActiveRatioThreshold()).isEqualTo(0.75D);
            assertThat(properties.getPressure().getJvmMemoryUsedRatioThreshold()).isEqualTo(0.70D);
            assertThat(properties.getRateLimit().getPerToken().getMaxRequests()).isEqualTo(11);
            assertThat(properties.getRateLimit().getPerToken().getWindow()).isEqualTo(Duration.ofSeconds(30));
            assertThat(properties.getRateLimit().getPerUser().getMaxRequests()).isEqualTo(22);
            assertThat(properties.getRateLimit().getPerTool().get(McpToolClass.EXECUTE).getMaxRequests())
                    .isEqualTo(7);
            assertThat(properties.getRateLimit().getPerTool().get(McpToolClass.EXECUTE).getWindow())
                    .isEqualTo(Duration.ofSeconds(10));
            assertThat(properties.getRateLimit().getPerTool().get(McpToolClass.LIGHT_READ).getMaxRequests())
                    .isEqualTo(180);
            assertThat(properties.getBulkhead().getGlobalMaxConcurrent()).isEqualTo(33);
            assertThat(properties.getBulkhead().getAcquireTimeout()).isEqualTo(Duration.ofMillis(25));
            assertThat(properties.getBulkhead().getClasses().get(McpToolClass.EXPLAIN).getMaxConcurrent())
                    .isEqualTo(5);
            assertThat(properties.getBulkhead().getClasses().get(McpToolClass.EXPLAIN).getAcquireTimeout())
                    .isEqualTo(Duration.ofMillis(15));
            assertThat(properties.getBulkhead().getPerTokenMaxConcurrent()).isEqualTo(2);
            assertThat(properties.getBulkhead().getPerUserMaxConcurrent()).isEqualTo(3);
            assertThat(properties.getBulkhead().getPerTargetMaxConcurrent()).isEqualTo(4);
            assertThat(properties.getDegradation().isEnabled()).isFalse();
            assertThat(properties.getDegradation().getHeavyReadMaxItemsUnderPressure()).isEqualTo(25);
            assertThat(properties.getDegradation().isRejectExplainUnderPressure()).isFalse();
            assertThat(properties.getDegradation().isRejectExecuteUnderPressure()).isFalse();
        });
    }

    /**
     * 验证非法压力占比阈值会导致启动失败。
     */
    @Test
    void shouldRejectInvalidPressureRatio() {
        contextRunner.withPropertyValues(
                "dbflow.capacity.pressure.target-pool-active-ratio-threshold=1.5"
        ).run(context -> assertStartupFailureContains(context.getStartupFailure(),
                "target-pool-active-ratio-threshold 必须大于 0 且小于等于 1"));
    }

    /**
     * 验证非法限流最大请求数会导致启动失败。
     */
    @Test
    void shouldRejectInvalidRateLimitMaximum() {
        contextRunner.withPropertyValues(
                "dbflow.capacity.rate-limit.per-token.max-requests=0"
        ).run(context -> assertStartupFailureContains(context.getStartupFailure(),
                "per-token.max-requests 必须大于 0"));
    }

    /**
     * 验证非法并发上限会导致启动失败。
     */
    @Test
    void shouldRejectInvalidBulkheadMaximum() {
        contextRunner.withPropertyValues(
                "dbflow.capacity.bulkhead.classes.EXECUTE.max-concurrent=0"
        ).run(context -> assertStartupFailureContains(context.getStartupFailure(),
                "classes.EXECUTE.max-concurrent 必须大于 0"));
    }

    /**
     * 验证非法降级条目上限会导致启动失败。
     */
    @Test
    void shouldRejectInvalidDegradationMaximum() {
        contextRunner.withPropertyValues(
                "dbflow.capacity.degradation.heavy-read-max-items-under-pressure=0"
        ).run(context -> assertStartupFailureContains(context.getStartupFailure(),
                "heavy-read-max-items-under-pressure 必须大于 0"));
    }

    /**
     * 验证容量治理关闭模式可以绑定，供后续 guard 返回 CAPACITY_DISABLED 放行。
     */
    @Test
    void shouldBindDisabledCapacityMode() {
        contextRunner.withPropertyValues("dbflow.capacity.enabled=false").run(context -> {
            CapacityProperties properties = context.getBean(CapacityProperties.class);

            assertThat(properties.isEnabled()).isFalse();
        });
    }

    /**
     * 容量配置属性测试配置。
     *
     * @author refinex
     */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CapacityProperties.class)
    static class TestConfiguration {
    }
}
