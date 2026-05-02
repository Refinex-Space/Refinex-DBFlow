package com.refinex.dbflow.capacity.service;

import com.refinex.dbflow.capacity.model.CapacityReasonCode;
import com.refinex.dbflow.capacity.model.CapacityScope;
import com.refinex.dbflow.capacity.model.CapacityStatus;
import com.refinex.dbflow.capacity.model.McpToolClass;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 容量治理 Micrometer 指标服务，集中维护容量指标名称和低基数标签。
 *
 * @author refinex
 */
@Service
public class CapacityMetricsService {

    /**
     * 最大标签长度，避免用户输入扩大指标基数。
     */
    private static final int MAX_TAG_LENGTH = 96;

    /**
     * Micrometer 指标注册表。
     */
    private final MeterRegistry meterRegistry;

    /**
     * 最近一轮本地压力信号，供健康和指标读取。
     */
    private final AtomicInteger localPressure = new AtomicInteger();

    /**
     * 创建容量指标服务。
     *
     * @param meterRegistry Micrometer 指标注册表
     */
    public CapacityMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry);
        Gauge.builder("dbflow.capacity.local.pressure", localPressure, AtomicInteger::get)
                .description("DBFlow local capacity pressure state, 1 means pressure detected.")
                .register(meterRegistry);
    }

    /**
     * 记录容量请求决策。
     *
     * @param toolClass  工具类别
     * @param status     容量状态
     * @param reasonCode 原因码
     */
    public void recordRequest(McpToolClass toolClass, CapacityStatus status, CapacityReasonCode reasonCode) {
        Counter.builder("dbflow.capacity.requests")
                .description("DBFlow capacity decisions.")
                .tag("toolClass", tag(toolClass == null ? null : toolClass.name()))
                .tag("decision", tag(status == null ? null : status.name()))
                .tag("reason", tag(reasonCode == null ? null : reasonCode.name()))
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录容量拒绝。
     *
     * @param toolClass  工具类别
     * @param reasonCode 原因码
     */
    public void recordRejection(McpToolClass toolClass, CapacityReasonCode reasonCode) {
        Counter.builder("dbflow.capacity.rejections")
                .description("DBFlow capacity rejections.")
                .tag("toolClass", tag(toolClass == null ? null : toolClass.name()))
                .tag("reason", tag(reasonCode == null ? null : reasonCode.name()))
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录容量降级。
     *
     * @param toolClass  工具类别
     * @param reasonCode 原因码
     */
    public void recordDegradation(McpToolClass toolClass, CapacityReasonCode reasonCode) {
        Counter.builder("dbflow.capacity.degradations")
                .description("DBFlow capacity degradations.")
                .tag("toolClass", tag(toolClass == null ? null : toolClass.name()))
                .tag("reason", tag(reasonCode == null ? null : reasonCode.name()))
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录限流耗尽。
     *
     * @param scope 限流范围
     */
    public void recordRateLimitExhausted(CapacityScope scope) {
        Counter.builder("dbflow.capacity.rate_limit.exhausted")
                .description("DBFlow capacity rate limit exhaustion count.")
                .tag("scope", tag(scope == null ? null : scope.name()))
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录 permit 获取耗时。
     *
     * @param toolClass    工具类别
     * @param status       容量状态
     * @param startedNanos 开始时间纳秒
     */
    public void recordAcquireDuration(McpToolClass toolClass, CapacityStatus status, long startedNanos) {
        long durationNanos = Math.max(0L, System.nanoTime() - startedNanos);
        Timer.builder("dbflow.capacity.acquire.duration")
                .description("DBFlow capacity permit acquire duration.")
                .tag("toolClass", tag(toolClass == null ? null : toolClass.name()))
                .tag("decision", tag(status == null ? null : status.name()))
                .register(meterRegistry)
                .record(Duration.ofNanos(durationNanos));
    }

    /**
     * 更新本地压力 gauge。
     *
     * @param pressured 是否处于压力态
     */
    public void updateLocalPressure(boolean pressured) {
        localPressure.set(pressured ? 1 : 0);
    }

    /**
     * 标准化指标标签值。
     *
     * @param value 原始值
     * @return 标签值
     */
    private String tag(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }
        String normalized = value.strip().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        if (normalized.length() <= MAX_TAG_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_TAG_LENGTH);
    }
}
