package com.refinex.dbflow.capacity;

import com.refinex.dbflow.capacity.properties.CapacityProperties;
import com.refinex.dbflow.capacity.support.InMemoryWindowRateLimiter;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 进程内固定窗口限流器测试。
 *
 * @author refinex
 */
class InMemoryWindowRateLimiterTests {

    /**
     * 验证同一 key 在窗口内超过阈值后会被拒绝并返回重试时间。
     */
    @Test
    void shouldRejectWhenWindowLimitExceeded() {
        MutableClock clock = new MutableClock();
        InMemoryWindowRateLimiter limiter = new InMemoryWindowRateLimiter(clock);
        CapacityProperties.RateLimitRule rule = new CapacityProperties.RateLimitRule(2, Duration.ofSeconds(10));

        assertThat(limiter.allow("token:1", rule).allowed()).isTrue();
        assertThat(limiter.allow("token:1", rule).allowed()).isTrue();
        InMemoryWindowRateLimiter.RateLimitResult rejected = limiter.allow("token:1", rule);

        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfter()).isEqualTo(Duration.ofSeconds(10));
        assertThat(rejected.current()).isEqualTo(3);
        assertThat(rejected.maxRequests()).isEqualTo(2);
    }

    /**
     * 验证窗口推进后限流计数会恢复。
     */
    @Test
    void shouldRecoverAfterWindowMoves() {
        MutableClock clock = new MutableClock();
        InMemoryWindowRateLimiter limiter = new InMemoryWindowRateLimiter(clock);
        CapacityProperties.RateLimitRule rule = new CapacityProperties.RateLimitRule(1, Duration.ofSeconds(5));

        assertThat(limiter.allow("user:1", rule).allowed()).isTrue();
        assertThat(limiter.allow("user:1", rule).allowed()).isFalse();

        clock.advance(Duration.ofSeconds(5));

        InMemoryWindowRateLimiter.RateLimitResult recovered = limiter.allow("user:1", rule);
        assertThat(recovered.allowed()).isTrue();
        assertThat(recovered.current()).isEqualTo(1);
    }

    /**
     * 验证不同 key 的限流窗口互不影响。
     */
    @Test
    void shouldIsolateDifferentKeys() {
        InMemoryWindowRateLimiter limiter = new InMemoryWindowRateLimiter(new MutableClock());
        CapacityProperties.RateLimitRule rule = new CapacityProperties.RateLimitRule(1, Duration.ofMinutes(1));

        assertThat(limiter.allow("token:1", rule).allowed()).isTrue();
        assertThat(limiter.allow("token:1", rule).allowed()).isFalse();
        assertThat(limiter.allow("token:2", rule).allowed()).isTrue();
    }

    /**
     * 验证过期窗口会被机会性清理，避免长期运行时 key 无界增长。
     */
    @Test
    void shouldCleanupExpiredWindowsOpportunistically() {
        MutableClock clock = new MutableClock();
        InMemoryWindowRateLimiter limiter = new InMemoryWindowRateLimiter(clock);
        CapacityProperties.RateLimitRule rule = new CapacityProperties.RateLimitRule(1, Duration.ofMillis(1));

        assertThat(limiter.allow("stale", rule).allowed()).isTrue();
        clock.advance(Duration.ofMinutes(11));
        for (int index = 0; index < 256; index++) {
            limiter.allow("fresh-" + index, rule);
        }

        assertThat(limiter.trackedWindows()).isLessThan(257);
    }

    /**
     * 可推进测试时钟。
     *
     * @author refinex
     */
    private static class MutableClock extends Clock {

        /**
         * 时区。
         */
        private final ZoneId zone = ZoneId.of("UTC");
        /**
         * 当前时间。
         */
        private Instant current = Instant.parse("2026-05-02T00:00:00Z");

        /**
         * 推进当前时间。
         *
         * @param duration 推进时长
         */
        private void advance(Duration duration) {
            current = current.plus(duration);
        }

        /**
         * 返回时区。
         *
         * @return 时区
         */
        @Override
        public ZoneId getZone() {
            return zone;
        }

        /**
         * 返回指定时区的新时钟。
         *
         * @param zone 时区
         * @return 指定时区的新时钟
         */
        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(current, zone);
        }

        /**
         * 返回当前时间。
         *
         * @return 当前时间
         */
        @Override
        public Instant instant() {
            return current;
        }
    }
}
