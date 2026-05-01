package com.refinex.dbflow.observability.support;

import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 日志关联上下文工具，统一维护 requestId 和 traceId。
 *
 * @author refinex
 */
public final class LogContext {

    /**
     * requestId MDC 键名。
     */
    public static final String REQUEST_ID_KEY = "requestId";

    /**
     * traceId MDC 键名。
     */
    public static final String TRACE_ID_KEY = "traceId";

    /**
     * traceId HTTP 头名称。
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 私有构造器。
     */
    private LogContext() {
    }

    /**
     * 设置当前线程日志关联上下文。
     *
     * @param requestId 请求标识
     * @param traceId   链路标识
     * @return 可关闭上下文
     */
    public static Scope withCorrelation(String requestId, String traceId) {
        String previousRequestId = MDC.get(REQUEST_ID_KEY);
        String previousTraceId = MDC.get(TRACE_ID_KEY);
        String effectiveRequestId = valueOrFallback(requestId, previousRequestId, "no-request");
        String effectiveTraceId = valueOrFallback(traceId, previousTraceId, effectiveRequestId);
        MDC.put(REQUEST_ID_KEY, effectiveRequestId);
        MDC.put(TRACE_ID_KEY, effectiveTraceId);
        return new Scope(previousRequestId, previousTraceId);
    }

    /**
     * 当前线程缺少关联上下文时生成一个新的上下文。
     *
     * @param prefix 生成标识前缀
     * @return 可关闭上下文
     */
    public static Scope ensureCorrelation(String prefix) {
        String currentRequestId = MDC.get(REQUEST_ID_KEY);
        String currentTraceId = MDC.get(TRACE_ID_KEY);
        if (StringUtils.hasText(currentRequestId) && StringUtils.hasText(currentTraceId)) {
            return new Scope(currentRequestId, currentTraceId, false);
        }
        String generated = safePrefix(prefix) + "-" + UUID.randomUUID();
        return withCorrelation(generated, generated);
    }

    /**
     * 返回当前 requestId。
     *
     * @return 当前 requestId
     */
    public static String currentRequestId() {
        return valueOrFallback(MDC.get(REQUEST_ID_KEY), null, "no-request");
    }

    /**
     * 返回当前 traceId。
     *
     * @return 当前 traceId
     */
    public static String currentTraceId() {
        return valueOrFallback(MDC.get(TRACE_ID_KEY), null, "no-trace");
    }

    /**
     * 返回当前 traceId；缺失时使用指定回退值。
     *
     * @param fallback 回退值
     * @return 当前 traceId 或回退值
     */
    public static String currentTraceIdOrDefault(String fallback) {
        return valueOrFallback(MDC.get(TRACE_ID_KEY), null, fallback);
    }

    /**
     * 解析文本值。
     *
     * @param value        首选值
     * @param fallback     回退值
     * @param defaultValue 默认值
     * @return 有效文本
     */
    private static String valueOrFallback(String value, String fallback, String defaultValue) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        if (StringUtils.hasText(fallback)) {
            return fallback.trim();
        }
        return defaultValue;
    }

    /**
     * 生成安全前缀。
     *
     * @param prefix 原始前缀
     * @return 安全前缀
     */
    private static String safePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "operation";
        }
        return prefix.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    /**
     * 可关闭日志上下文。
     */
    public static final class Scope implements AutoCloseable {

        /**
         * 上一个 requestId。
         */
        private final String previousRequestId;

        /**
         * 上一个 traceId。
         */
        private final String previousTraceId;

        /**
         * 是否在关闭时恢复。
         */
        private final boolean restoreOnClose;

        /**
         * 创建可关闭日志上下文。
         *
         * @param previousRequestId 上一个 requestId
         * @param previousTraceId   上一个 traceId
         */
        private Scope(String previousRequestId, String previousTraceId) {
            this(previousRequestId, previousTraceId, true);
        }

        /**
         * 创建可关闭日志上下文。
         *
         * @param previousRequestId 上一个 requestId
         * @param previousTraceId   上一个 traceId
         * @param restoreOnClose    是否在关闭时恢复
         */
        private Scope(String previousRequestId, String previousTraceId, boolean restoreOnClose) {
            this.previousRequestId = previousRequestId;
            this.previousTraceId = previousTraceId;
            this.restoreOnClose = restoreOnClose;
        }

        /**
         * 关闭上下文并恢复原 MDC。
         */
        @Override
        public void close() {
            if (!restoreOnClose) {
                return;
            }
            restore(REQUEST_ID_KEY, previousRequestId);
            restore(TRACE_ID_KEY, previousTraceId);
        }

        /**
         * 恢复 MDC 键。
         *
         * @param key   MDC 键
         * @param value 原值
         */
        private void restore(String key, String value) {
            if (value == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, value);
            }
        }
    }
}
