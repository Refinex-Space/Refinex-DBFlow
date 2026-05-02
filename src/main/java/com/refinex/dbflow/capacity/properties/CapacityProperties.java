package com.refinex.dbflow.capacity.properties;

import com.refinex.dbflow.capacity.model.McpToolClass;
import jakarta.validation.Valid;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * DBFlow 容量治理配置，覆盖限流、并发舱壁、压力判断和降级策略。
 *
 * @author refinex
 */
@Validated
@ConfigurationProperties(prefix = "dbflow.capacity")
public class CapacityProperties implements InitializingBean {

    /**
     * 是否启用容量治理；关闭时容量检查直接放行但保留稳定原因码。
     */
    private boolean enabled = true;

    /**
     * 压力判断配置。
     */
    @Valid
    private Pressure pressure = new Pressure();

    /**
     * 固定窗口限流配置。
     */
    @Valid
    private RateLimit rateLimit = new RateLimit();

    /**
     * 并发舱壁配置。
     */
    @Valid
    private Bulkhead bulkhead = new Bulkhead();

    /**
     * 流量突增降级配置。
     */
    @Valid
    private Degradation degradation = new Degradation();

    /**
     * 创建工具类别默认限流规则。
     *
     * @return 工具类别默认限流规则
     */
    private static EnumMap<McpToolClass, RateLimitRule> defaultToolRateLimits() {
        EnumMap<McpToolClass, RateLimitRule> limits = new EnumMap<>(McpToolClass.class);
        limits.put(McpToolClass.LIGHT_READ, new RateLimitRule(180, Duration.ofMinutes(1)));
        limits.put(McpToolClass.HEAVY_READ, new RateLimitRule(60, Duration.ofMinutes(1)));
        limits.put(McpToolClass.EXPLAIN, new RateLimitRule(60, Duration.ofMinutes(1)));
        limits.put(McpToolClass.EXECUTE, new RateLimitRule(30, Duration.ofMinutes(1)));
        return limits;
    }

    /**
     * 创建工具类别默认并发舱壁规则。
     *
     * @return 工具类别默认并发舱壁规则
     */
    private static EnumMap<McpToolClass, BulkheadClassRule> defaultClassBulkheads() {
        EnumMap<McpToolClass, BulkheadClassRule> classes = new EnumMap<>(McpToolClass.class);
        classes.put(McpToolClass.LIGHT_READ, new BulkheadClassRule(40, Duration.ofMillis(200)));
        classes.put(McpToolClass.HEAVY_READ, new BulkheadClassRule(20, Duration.ofMillis(100)));
        classes.put(McpToolClass.EXPLAIN, new BulkheadClassRule(16, Duration.ofMillis(100)));
        classes.put(McpToolClass.EXECUTE, new BulkheadClassRule(10, Duration.ZERO));
        return classes;
    }

    /**
     * 校验占比阈值。
     *
     * @param value 阈值
     * @param name  配置名称
     */
    private static void validateRatio(double value, String name) {
        if (value <= 0D || value > 1D) {
            throw new IllegalStateException("dbflow.capacity.pressure." + name + " 必须大于 0 且小于等于 1");
        }
    }

    /**
     * 校验持续时间非负。
     *
     * @param duration 持续时间
     * @param name     配置名称
     */
    private static void validateNonNegative(Duration duration, String name) {
        if (duration == null || duration.isNegative()) {
            throw new IllegalStateException(name + " 不能小于 0");
        }
    }

    /**
     * 返回是否启用容量治理。
     *
     * @return 启用时返回 true
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用容量治理。
     *
     * @param enabled 是否启用容量治理
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 返回压力判断配置。
     *
     * @return 压力判断配置
     */
    public Pressure getPressure() {
        return pressure;
    }

    /**
     * 设置压力判断配置。
     *
     * @param pressure 压力判断配置
     */
    public void setPressure(Pressure pressure) {
        this.pressure = Objects.requireNonNullElseGet(pressure, Pressure::new);
    }

    /**
     * 返回限流配置。
     *
     * @return 限流配置
     */
    public RateLimit getRateLimit() {
        return rateLimit;
    }

    /**
     * 设置限流配置。
     *
     * @param rateLimit 限流配置
     */
    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = Objects.requireNonNullElseGet(rateLimit, RateLimit::new);
    }

    /**
     * 返回并发舱壁配置。
     *
     * @return 并发舱壁配置
     */
    public Bulkhead getBulkhead() {
        return bulkhead;
    }

    /**
     * 设置并发舱壁配置。
     *
     * @param bulkhead 并发舱壁配置
     */
    public void setBulkhead(Bulkhead bulkhead) {
        this.bulkhead = Objects.requireNonNullElseGet(bulkhead, Bulkhead::new);
    }

    /**
     * 返回降级配置。
     *
     * @return 降级配置
     */
    public Degradation getDegradation() {
        return degradation;
    }

    /**
     * 设置降级配置。
     *
     * @param degradation 降级配置
     */
    public void setDegradation(Degradation degradation) {
        this.degradation = Objects.requireNonNullElseGet(degradation, Degradation::new);
    }

    /**
     * 完成绑定后执行跨字段校验并补齐工具类别默认配置。
     */
    @Override
    public void afterPropertiesSet() {
        rateLimit.ensureToolDefaults();
        bulkhead.ensureClassDefaults();
        pressure.validate();
        rateLimit.validate();
        bulkhead.validate();
        degradation.validate();
    }

    /**
     * 容量压力判断配置。
     *
     * @author refinex
     */
    public static class Pressure {

        /**
         * 是否启用压力判断。
         */
        private boolean enabled = true;

        /**
         * 目标连接池等待线程阈值，大于等于该值视为目标压力。
         */
        private int targetPoolWaitingThreshold = 1;

        /**
         * 目标连接池活跃连接占比阈值，范围为 (0, 1]。
         */
        private double targetPoolActiveRatioThreshold = 0.85D;

        /**
         * JVM 已用内存占比阈值，范围为 (0, 1]。
         */
        private double jvmMemoryUsedRatioThreshold = 0.85D;

        /**
         * 返回是否启用压力判断。
         *
         * @return 启用时返回 true
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用压力判断。
         *
         * @param enabled 是否启用压力判断
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 返回目标连接池等待线程阈值。
         *
         * @return 目标连接池等待线程阈值
         */
        public int getTargetPoolWaitingThreshold() {
            return targetPoolWaitingThreshold;
        }

        /**
         * 设置目标连接池等待线程阈值。
         *
         * @param targetPoolWaitingThreshold 目标连接池等待线程阈值
         */
        public void setTargetPoolWaitingThreshold(int targetPoolWaitingThreshold) {
            this.targetPoolWaitingThreshold = targetPoolWaitingThreshold;
        }

        /**
         * 返回目标连接池活跃连接占比阈值。
         *
         * @return 目标连接池活跃连接占比阈值
         */
        public double getTargetPoolActiveRatioThreshold() {
            return targetPoolActiveRatioThreshold;
        }

        /**
         * 设置目标连接池活跃连接占比阈值。
         *
         * @param targetPoolActiveRatioThreshold 目标连接池活跃连接占比阈值
         */
        public void setTargetPoolActiveRatioThreshold(double targetPoolActiveRatioThreshold) {
            this.targetPoolActiveRatioThreshold = targetPoolActiveRatioThreshold;
        }

        /**
         * 返回 JVM 已用内存占比阈值。
         *
         * @return JVM 已用内存占比阈值
         */
        public double getJvmMemoryUsedRatioThreshold() {
            return jvmMemoryUsedRatioThreshold;
        }

        /**
         * 设置 JVM 已用内存占比阈值。
         *
         * @param jvmMemoryUsedRatioThreshold JVM 已用内存占比阈值
         */
        public void setJvmMemoryUsedRatioThreshold(double jvmMemoryUsedRatioThreshold) {
            this.jvmMemoryUsedRatioThreshold = jvmMemoryUsedRatioThreshold;
        }

        /**
         * 校验压力阈值配置。
         */
        private void validate() {
            if (targetPoolWaitingThreshold < 0) {
                throw new IllegalStateException(
                        "dbflow.capacity.pressure.target-pool-waiting-threshold 不能小于 0");
            }
            validateRatio(targetPoolActiveRatioThreshold, "target-pool-active-ratio-threshold");
            validateRatio(jvmMemoryUsedRatioThreshold, "jvm-memory-used-ratio-threshold");
        }
    }

    /**
     * 固定窗口限流配置。
     *
     * @author refinex
     */
    public static class RateLimit {

        /**
         * 单 Token 限流规则。
         */
        @Valid
        private RateLimitRule perToken = new RateLimitRule(60, Duration.ofMinutes(1));

        /**
         * 单用户限流规则。
         */
        @Valid
        private RateLimitRule perUser = new RateLimitRule(120, Duration.ofMinutes(1));

        /**
         * 按工具类别的限流规则。
         */
        @Valid
        private Map<McpToolClass, RateLimitRule> perTool = defaultToolRateLimits();

        /**
         * 返回单 Token 限流规则。
         *
         * @return 单 Token 限流规则
         */
        public RateLimitRule getPerToken() {
            return perToken;
        }

        /**
         * 设置单 Token 限流规则。
         *
         * @param perToken 单 Token 限流规则
         */
        public void setPerToken(RateLimitRule perToken) {
            this.perToken = Objects.requireNonNullElseGet(perToken, () -> new RateLimitRule(60, Duration.ofMinutes(1)));
        }

        /**
         * 返回单用户限流规则。
         *
         * @return 单用户限流规则
         */
        public RateLimitRule getPerUser() {
            return perUser;
        }

        /**
         * 设置单用户限流规则。
         *
         * @param perUser 单用户限流规则
         */
        public void setPerUser(RateLimitRule perUser) {
            this.perUser = Objects.requireNonNullElseGet(perUser, () -> new RateLimitRule(120, Duration.ofMinutes(1)));
        }

        /**
         * 返回按工具类别的限流规则。
         *
         * @return 按工具类别的限流规则
         */
        public Map<McpToolClass, RateLimitRule> getPerTool() {
            return perTool;
        }

        /**
         * 设置按工具类别的限流规则。
         *
         * @param perTool 按工具类别的限流规则
         */
        public void setPerTool(Map<McpToolClass, RateLimitRule> perTool) {
            this.perTool = perTool == null ? defaultToolRateLimits() : new EnumMap<>(perTool);
        }

        /**
         * 补齐未显式配置的工具类别默认限流。
         */
        private void ensureToolDefaults() {
            EnumMap<McpToolClass, RateLimitRule> merged = defaultToolRateLimits();
            merged.putAll(perTool);
            perTool = merged;
        }

        /**
         * 校验限流配置。
         */
        private void validate() {
            perToken.validate("per-token");
            perUser.validate("per-user");
            perTool.forEach((toolClass, rule) -> rule.validate("per-tool." + toolClass.name()));
        }
    }

    /**
     * 固定窗口限流规则。
     *
     * @author refinex
     */
    public static class RateLimitRule {

        /**
         * 窗口内允许请求数。
         */
        private int maxRequests;

        /**
         * 固定窗口长度。
         */
        private Duration window;

        /**
         * 创建默认限流规则。
         */
        public RateLimitRule() {
            this(1, Duration.ofMinutes(1));
        }

        /**
         * 创建固定窗口限流规则。
         *
         * @param maxRequests 窗口内允许请求数
         * @param window      固定窗口长度
         */
        public RateLimitRule(int maxRequests, Duration window) {
            this.maxRequests = maxRequests;
            this.window = window;
        }

        /**
         * 返回窗口内允许请求数。
         *
         * @return 窗口内允许请求数
         */
        public int getMaxRequests() {
            return maxRequests;
        }

        /**
         * 设置窗口内允许请求数。
         *
         * @param maxRequests 窗口内允许请求数
         */
        public void setMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
        }

        /**
         * 返回固定窗口长度。
         *
         * @return 固定窗口长度
         */
        public Duration getWindow() {
            return window;
        }

        /**
         * 设置固定窗口长度。
         *
         * @param window 固定窗口长度
         */
        public void setWindow(Duration window) {
            this.window = Objects.requireNonNullElse(window, Duration.ofMinutes(1));
        }

        /**
         * 校验限流规则。
         *
         * @param name 配置名称
         */
        private void validate(String name) {
            if (maxRequests <= 0) {
                throw new IllegalStateException("dbflow.capacity.rate-limit." + name + ".max-requests 必须大于 0");
            }
            if (window == null || window.isZero() || window.isNegative()) {
                throw new IllegalStateException("dbflow.capacity.rate-limit." + name + ".window 必须大于 0");
            }
        }
    }

    /**
     * 并发舱壁配置。
     *
     * @author refinex
     */
    public static class Bulkhead {

        /**
         * 单实例全局最大并发。
         */
        private int globalMaxConcurrent = 80;

        /**
         * 全局 permit 获取等待时间。
         */
        private Duration acquireTimeout = Duration.ofMillis(100);

        /**
         * 按工具类别的并发舱壁规则。
         */
        @Valid
        private Map<McpToolClass, BulkheadClassRule> classes = defaultClassBulkheads();

        /**
         * 单 Token 最大并发。
         */
        private int perTokenMaxConcurrent = 4;

        /**
         * 单用户最大并发。
         */
        private int perUserMaxConcurrent = 8;

        /**
         * 单目标 project/environment 最大并发。
         */
        private int perTargetMaxConcurrent = 6;

        /**
         * 返回全局最大并发。
         *
         * @return 全局最大并发
         */
        public int getGlobalMaxConcurrent() {
            return globalMaxConcurrent;
        }

        /**
         * 设置全局最大并发。
         *
         * @param globalMaxConcurrent 全局最大并发
         */
        public void setGlobalMaxConcurrent(int globalMaxConcurrent) {
            this.globalMaxConcurrent = globalMaxConcurrent;
        }

        /**
         * 返回全局 permit 获取等待时间。
         *
         * @return 全局 permit 获取等待时间
         */
        public Duration getAcquireTimeout() {
            return acquireTimeout;
        }

        /**
         * 设置全局 permit 获取等待时间。
         *
         * @param acquireTimeout 全局 permit 获取等待时间
         */
        public void setAcquireTimeout(Duration acquireTimeout) {
            this.acquireTimeout = Objects.requireNonNullElse(acquireTimeout, Duration.ofMillis(100));
        }

        /**
         * 返回按工具类别的并发舱壁规则。
         *
         * @return 按工具类别的并发舱壁规则
         */
        public Map<McpToolClass, BulkheadClassRule> getClasses() {
            return classes;
        }

        /**
         * 设置按工具类别的并发舱壁规则。
         *
         * @param classes 按工具类别的并发舱壁规则
         */
        public void setClasses(Map<McpToolClass, BulkheadClassRule> classes) {
            this.classes = classes == null ? defaultClassBulkheads() : new EnumMap<>(classes);
        }

        /**
         * 返回单 Token 最大并发。
         *
         * @return 单 Token 最大并发
         */
        public int getPerTokenMaxConcurrent() {
            return perTokenMaxConcurrent;
        }

        /**
         * 设置单 Token 最大并发。
         *
         * @param perTokenMaxConcurrent 单 Token 最大并发
         */
        public void setPerTokenMaxConcurrent(int perTokenMaxConcurrent) {
            this.perTokenMaxConcurrent = perTokenMaxConcurrent;
        }

        /**
         * 返回单用户最大并发。
         *
         * @return 单用户最大并发
         */
        public int getPerUserMaxConcurrent() {
            return perUserMaxConcurrent;
        }

        /**
         * 设置单用户最大并发。
         *
         * @param perUserMaxConcurrent 单用户最大并发
         */
        public void setPerUserMaxConcurrent(int perUserMaxConcurrent) {
            this.perUserMaxConcurrent = perUserMaxConcurrent;
        }

        /**
         * 返回单目标最大并发。
         *
         * @return 单目标最大并发
         */
        public int getPerTargetMaxConcurrent() {
            return perTargetMaxConcurrent;
        }

        /**
         * 设置单目标最大并发。
         *
         * @param perTargetMaxConcurrent 单目标最大并发
         */
        public void setPerTargetMaxConcurrent(int perTargetMaxConcurrent) {
            this.perTargetMaxConcurrent = perTargetMaxConcurrent;
        }

        /**
         * 补齐未显式配置的工具类别默认并发舱壁。
         */
        private void ensureClassDefaults() {
            EnumMap<McpToolClass, BulkheadClassRule> merged = defaultClassBulkheads();
            merged.putAll(classes);
            classes = merged;
        }

        /**
         * 校验并发舱壁配置。
         */
        private void validate() {
            if (globalMaxConcurrent <= 0) {
                throw new IllegalStateException("dbflow.capacity.bulkhead.global-max-concurrent 必须大于 0");
            }
            validateNonNegative(acquireTimeout, "dbflow.capacity.bulkhead.acquire-timeout");
            if (perTokenMaxConcurrent <= 0 || perUserMaxConcurrent <= 0 || perTargetMaxConcurrent <= 0) {
                throw new IllegalStateException("dbflow.capacity.bulkhead per-token/per-user/per-target 必须大于 0");
            }
            classes.forEach((toolClass, rule) -> rule.validate(toolClass));
        }
    }

    /**
     * 工具类别并发舱壁规则。
     *
     * @author refinex
     */
    public static class BulkheadClassRule {

        /**
         * 工具类别最大并发。
         */
        private int maxConcurrent;

        /**
         * 工具类别 permit 获取等待时间。
         */
        private Duration acquireTimeout;

        /**
         * 创建默认工具类别并发舱壁规则。
         */
        public BulkheadClassRule() {
            this(1, Duration.ZERO);
        }

        /**
         * 创建工具类别并发舱壁规则。
         *
         * @param maxConcurrent  工具类别最大并发
         * @param acquireTimeout permit 获取等待时间
         */
        public BulkheadClassRule(int maxConcurrent, Duration acquireTimeout) {
            this.maxConcurrent = maxConcurrent;
            this.acquireTimeout = acquireTimeout;
        }

        /**
         * 返回最大并发。
         *
         * @return 最大并发
         */
        public int getMaxConcurrent() {
            return maxConcurrent;
        }

        /**
         * 设置最大并发。
         *
         * @param maxConcurrent 最大并发
         */
        public void setMaxConcurrent(int maxConcurrent) {
            this.maxConcurrent = maxConcurrent;
        }

        /**
         * 返回 permit 获取等待时间。
         *
         * @return permit 获取等待时间
         */
        public Duration getAcquireTimeout() {
            return acquireTimeout;
        }

        /**
         * 设置 permit 获取等待时间。
         *
         * @param acquireTimeout permit 获取等待时间
         */
        public void setAcquireTimeout(Duration acquireTimeout) {
            this.acquireTimeout = Objects.requireNonNullElse(acquireTimeout, Duration.ZERO);
        }

        /**
         * 校验工具类别并发舱壁规则。
         *
         * @param toolClass 工具类别
         */
        private void validate(McpToolClass toolClass) {
            if (maxConcurrent <= 0) {
                throw new IllegalStateException("dbflow.capacity.bulkhead.classes." + toolClass
                        + ".max-concurrent 必须大于 0");
            }
            validateNonNegative(acquireTimeout, "dbflow.capacity.bulkhead.classes." + toolClass + ".acquire-timeout");
        }
    }

    /**
     * 压力态降级配置。
     *
     * @author refinex
     */
    public static class Degradation {

        /**
         * 是否启用压力态降级。
         */
        private boolean enabled = true;

        /**
         * 压力态下重型只读最大返回条目数。
         */
        private int heavyReadMaxItemsUnderPressure = 50;

        /**
         * 目标或本地压力态下是否拒绝 EXPLAIN。
         */
        private boolean rejectExplainUnderPressure = true;

        /**
         * 目标或本地压力态下是否拒绝 EXECUTE。
         */
        private boolean rejectExecuteUnderPressure = true;

        /**
         * 返回是否启用压力态降级。
         *
         * @return 启用时返回 true
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用压力态降级。
         *
         * @param enabled 是否启用压力态降级
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 返回压力态下重型只读最大返回条目数。
         *
         * @return 压力态下重型只读最大返回条目数
         */
        public int getHeavyReadMaxItemsUnderPressure() {
            return heavyReadMaxItemsUnderPressure;
        }

        /**
         * 设置压力态下重型只读最大返回条目数。
         *
         * @param heavyReadMaxItemsUnderPressure 压力态下重型只读最大返回条目数
         */
        public void setHeavyReadMaxItemsUnderPressure(int heavyReadMaxItemsUnderPressure) {
            this.heavyReadMaxItemsUnderPressure = heavyReadMaxItemsUnderPressure;
        }

        /**
         * 返回压力态下是否拒绝 EXPLAIN。
         *
         * @return 拒绝时返回 true
         */
        public boolean isRejectExplainUnderPressure() {
            return rejectExplainUnderPressure;
        }

        /**
         * 设置压力态下是否拒绝 EXPLAIN。
         *
         * @param rejectExplainUnderPressure 压力态下是否拒绝 EXPLAIN
         */
        public void setRejectExplainUnderPressure(boolean rejectExplainUnderPressure) {
            this.rejectExplainUnderPressure = rejectExplainUnderPressure;
        }

        /**
         * 返回压力态下是否拒绝 EXECUTE。
         *
         * @return 拒绝时返回 true
         */
        public boolean isRejectExecuteUnderPressure() {
            return rejectExecuteUnderPressure;
        }

        /**
         * 设置压力态下是否拒绝 EXECUTE。
         *
         * @param rejectExecuteUnderPressure 压力态下是否拒绝 EXECUTE
         */
        public void setRejectExecuteUnderPressure(boolean rejectExecuteUnderPressure) {
            this.rejectExecuteUnderPressure = rejectExecuteUnderPressure;
        }

        /**
         * 校验降级配置。
         */
        private void validate() {
            if (heavyReadMaxItemsUnderPressure <= 0) {
                throw new IllegalStateException(
                        "dbflow.capacity.degradation.heavy-read-max-items-under-pressure 必须大于 0");
            }
        }
    }
}
