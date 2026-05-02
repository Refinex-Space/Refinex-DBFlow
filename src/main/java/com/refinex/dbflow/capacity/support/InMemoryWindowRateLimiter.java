package com.refinex.dbflow.capacity.support;

import com.refinex.dbflow.capacity.properties.CapacityProperties;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 进程内固定窗口限流器，用于单实例 Token、用户、工具和目标维度限流。
 *
 * @author refinex
 */
public class InMemoryWindowRateLimiter {

    /**
     * 触发过期窗口清理的调用间隔。
     */
    private static final long CLEANUP_INTERVAL_CALLS = 256L;

    /**
     * 限流 key 到固定窗口计数器的映射。
     */
    private final ConcurrentMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    /**
     * 时钟，用于测试中推进窗口。
     */
    private final Clock clock;

    /**
     * 调用计数，用于机会性清理过期窗口。
     */
    private final AtomicLong calls = new AtomicLong();

    /**
     * 创建使用系统时钟的固定窗口限流器。
     */
    public InMemoryWindowRateLimiter() {
        this(Clock.systemUTC());
    }

    /**
     * 创建固定窗口限流器。
     *
     * @param clock 时钟
     */
    public InMemoryWindowRateLimiter(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * 判断当前 key 是否允许通过限流。
     *
     * @param key  限流 key
     * @param rule 限流规则
     * @return 限流结果
     */
    public RateLimitResult allow(String key, CapacityProperties.RateLimitRule rule) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(rule, "rule");
        cleanupIfNeeded();
        long now = clock.millis();
        long windowMillis = Math.max(1L, rule.getWindow().toMillis());
        WindowCounter counter = counters.compute(key, (currentKey, current) -> {
            if (current == null || now >= current.windowStartedAtMillis() + windowMillis) {
                return new WindowCounter(now, 1);
            }
            return new WindowCounter(current.windowStartedAtMillis(), current.count() + 1);
        });
        boolean allowed = counter.count() <= rule.getMaxRequests();
        Duration retryAfter = allowed ? Duration.ZERO : retryAfter(now, counter.windowStartedAtMillis(), windowMillis);
        return new RateLimitResult(allowed, retryAfter, counter.count(), rule.getMaxRequests());
    }

    /**
     * 返回当前跟踪的限流窗口数量，供测试和健康检查使用。
     *
     * @return 当前窗口数量
     */
    public int trackedWindows() {
        return counters.size();
    }

    /**
     * 机会性清理过期窗口，避免长期运行后 key 无界增长。
     */
    private void cleanupIfNeeded() {
        long currentCalls = calls.incrementAndGet();
        if (currentCalls % CLEANUP_INTERVAL_CALLS != 0) {
            return;
        }
        long now = clock.millis();
        counters.entrySet().removeIf(entry -> now >= entry.getValue().windowStartedAtMillis()
                + Duration.ofMinutes(10).toMillis());
    }

    /**
     * 计算建议重试等待时间。
     *
     * @param now                   当前时间毫秒
     * @param windowStartedAtMillis 窗口开始时间毫秒
     * @param windowMillis          窗口长度毫秒
     * @return 建议重试等待时间
     */
    private Duration retryAfter(long now, long windowStartedAtMillis, long windowMillis) {
        return Duration.ofMillis(Math.max(1L, windowStartedAtMillis + windowMillis - now));
    }

    /**
     * 固定窗口限流结果。
     *
     * @param allowed     是否允许通过
     * @param retryAfter  建议重试等待时间
     * @param current     当前窗口计数
     * @param maxRequests 当前窗口最大请求数
     */
    public record RateLimitResult(boolean allowed, Duration retryAfter, int current, int maxRequests) {
    }

    /**
     * 固定窗口计数器。
     *
     * @param windowStartedAtMillis 窗口开始时间毫秒
     * @param count                 当前窗口计数
     */
    private record WindowCounter(long windowStartedAtMillis, int count) {
    }
}
