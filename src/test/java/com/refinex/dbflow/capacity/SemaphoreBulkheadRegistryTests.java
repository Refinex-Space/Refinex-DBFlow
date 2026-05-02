package com.refinex.dbflow.capacity;

import com.refinex.dbflow.capacity.model.CapacityReasonCode;
import com.refinex.dbflow.capacity.model.CapacityScope;
import com.refinex.dbflow.capacity.support.CapacityPermit;
import com.refinex.dbflow.capacity.support.SemaphoreBulkheadRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 信号量并发舱壁注册表测试。
 *
 * @author refinex
 */
class SemaphoreBulkheadRegistryTests {

    /**
     * 验证零等待舱壁在满载时立即拒绝。
     */
    @Test
    void shouldRejectImmediatelyWhenZeroTimeoutBulkheadIsFull() {
        SemaphoreBulkheadRegistry registry = new SemaphoreBulkheadRegistry();
        SemaphoreBulkheadRegistry.BulkheadRequest request = request(CapacityScope.TOOL_CLASS, "EXECUTE", 1,
                Duration.ZERO, CapacityReasonCode.TOOL_BULKHEAD_FULL);

        SemaphoreBulkheadRegistry.BulkheadAcquireResult first = registry.acquire(request);
        SemaphoreBulkheadRegistry.BulkheadAcquireResult second = registry.acquire(request);

        assertThat(first.acquired()).isTrue();
        assertThat(second.acquired()).isFalse();
        assertThat(second.reasonCode()).isEqualTo(CapacityReasonCode.TOOL_BULKHEAD_FULL);
        assertThat(registry.activePermits(CapacityScope.TOOL_CLASS, "EXECUTE")).isEqualTo(1);

        first.permit().close();
        assertThat(registry.availablePermits(CapacityScope.TOOL_CLASS, "EXECUTE")).isEqualTo(1);
    }

    /**
     * 验证不同 scope 和名称的舱壁互不影响。
     */
    @Test
    void shouldIsolateDifferentScopesAndNames() {
        SemaphoreBulkheadRegistry registry = new SemaphoreBulkheadRegistry();
        SemaphoreBulkheadRegistry.BulkheadAcquireResult tokenPermit = registry.acquire(
                request(CapacityScope.TOKEN, "1", 1, Duration.ZERO, CapacityReasonCode.TOKEN_BULKHEAD_FULL));
        SemaphoreBulkheadRegistry.BulkheadAcquireResult userPermit = registry.acquire(
                request(CapacityScope.USER, "1", 1, Duration.ZERO, CapacityReasonCode.USER_BULKHEAD_FULL));

        assertThat(tokenPermit.acquired()).isTrue();
        assertThat(userPermit.acquired()).isTrue();
        assertThat(registry.activePermits(CapacityScope.TOKEN, "1")).isEqualTo(1);
        assertThat(registry.activePermits(CapacityScope.USER, "1")).isEqualTo(1);

        tokenPermit.permit().close();
        userPermit.permit().close();
    }

    /**
     * 验证聚合获取在后续舱壁失败时会释放已获取 permit。
     */
    @Test
    void shouldReleaseAlreadyAcquiredPermitsWhenAggregateAcquireFails() {
        SemaphoreBulkheadRegistry registry = new SemaphoreBulkheadRegistry();
        SemaphoreBulkheadRegistry.BulkheadRequest global = request(CapacityScope.GLOBAL, "global", 1,
                Duration.ZERO, CapacityReasonCode.GLOBAL_BULKHEAD_FULL);
        SemaphoreBulkheadRegistry.BulkheadRequest target = request(CapacityScope.TARGET, "demo/dev", 1,
                Duration.ZERO, CapacityReasonCode.TARGET_BULKHEAD_FULL);

        SemaphoreBulkheadRegistry.BulkheadAcquireResult occupied = registry.acquire(target);
        SemaphoreBulkheadRegistry.BulkheadAcquireResult aggregate = registry.acquireAll(List.of(global, target));

        assertThat(occupied.acquired()).isTrue();
        assertThat(aggregate.acquired()).isFalse();
        assertThat(aggregate.reasonCode()).isEqualTo(CapacityReasonCode.TARGET_BULKHEAD_FULL);
        assertThat(registry.activePermits(CapacityScope.GLOBAL, "global")).isZero();
        assertThat(registry.activePermits(CapacityScope.TARGET, "demo/dev")).isEqualTo(1);

        occupied.permit().close();
    }

    /**
     * 验证聚合 permit 关闭后会释放所有已获取舱壁，并且重复关闭不会多释放。
     */
    @Test
    void shouldReleaseAggregatePermitIdempotently() {
        SemaphoreBulkheadRegistry registry = new SemaphoreBulkheadRegistry();
        SemaphoreBulkheadRegistry.BulkheadAcquireResult aggregate = registry.acquireAll(List.of(
                request(CapacityScope.GLOBAL, "global", 2, Duration.ZERO, CapacityReasonCode.GLOBAL_BULKHEAD_FULL),
                request(CapacityScope.TOOL_CLASS, "LIGHT_READ", 2, Duration.ZERO,
                        CapacityReasonCode.TOOL_BULKHEAD_FULL)
        ));

        assertThat(aggregate.acquired()).isTrue();
        assertThat(registry.activePermits(CapacityScope.GLOBAL, "global")).isEqualTo(1);
        assertThat(registry.activePermits(CapacityScope.TOOL_CLASS, "LIGHT_READ")).isEqualTo(1);

        CapacityPermit permit = aggregate.permit();
        permit.close();
        permit.close();

        assertThat(registry.availablePermits(CapacityScope.GLOBAL, "global")).isEqualTo(2);
        assertThat(registry.availablePermits(CapacityScope.TOOL_CLASS, "LIGHT_READ")).isEqualTo(2);
    }

    /**
     * 创建并发舱壁请求。
     *
     * @param scope         舱壁范围
     * @param name          舱壁名称
     * @param maxConcurrent 最大并发
     * @param timeout       获取等待时间
     * @param reasonCode    失败原因码
     * @return 并发舱壁请求
     */
    private SemaphoreBulkheadRegistry.BulkheadRequest request(
            CapacityScope scope,
            String name,
            int maxConcurrent,
            Duration timeout,
            CapacityReasonCode reasonCode
    ) {
        return new SemaphoreBulkheadRegistry.BulkheadRequest(scope, name, maxConcurrent, timeout, reasonCode);
    }
}
