package com.refinex.dbflow.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * MCP Streamable HTTP endpoint 安全加固配置。
 *
 * @author refinex
 */
@Validated
@ConfigurationProperties(prefix = "dbflow.security.mcp-endpoint")
public class McpEndpointSecurityProperties {

    /**
     * Origin 校验配置。
     */
    @Valid
    private Origin origin = new Origin();

    /**
     * 请求大小限制配置。
     */
    @Valid
    private RequestSize requestSize = new RequestSize();

    /**
     * 基础限流配置。
     */
    @Valid
    private RateLimit rateLimit = new RateLimit();

    /**
     * 返回 Origin 校验配置。
     *
     * @return Origin 校验配置
     */
    public Origin getOrigin() {
        return origin;
    }

    /**
     * 设置 Origin 校验配置。
     *
     * @param origin Origin 校验配置
     */
    public void setOrigin(Origin origin) {
        this.origin = Objects.requireNonNullElseGet(origin, Origin::new);
    }

    /**
     * 返回请求大小限制配置。
     *
     * @return 请求大小限制配置
     */
    public RequestSize getRequestSize() {
        return requestSize;
    }

    /**
     * 设置请求大小限制配置。
     *
     * @param requestSize 请求大小限制配置
     */
    public void setRequestSize(RequestSize requestSize) {
        this.requestSize = Objects.requireNonNullElseGet(requestSize, RequestSize::new);
    }

    /**
     * 返回基础限流配置。
     *
     * @return 基础限流配置
     */
    public RateLimit getRateLimit() {
        return rateLimit;
    }

    /**
     * 设置基础限流配置。
     *
     * @param rateLimit 基础限流配置
     */
    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = Objects.requireNonNullElseGet(rateLimit, RateLimit::new);
    }

    /**
     * MCP Origin 校验配置。
     *
     * @author refinex
     */
    public static class Origin {

        /**
         * 是否启用 Origin 校验。
         */
        private boolean enabled = true;

        /**
         * 可信 Origin 列表，格式为 scheme://host[:port]。
         */
        private List<String> trustedOrigins = new ArrayList<>();

        /**
         * 返回是否启用 Origin 校验。
         *
         * @return 启用时返回 true
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用 Origin 校验。
         *
         * @param enabled 是否启用 Origin 校验
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 返回可信 Origin 列表。
         *
         * @return 可信 Origin 列表
         */
        public List<String> getTrustedOrigins() {
            return trustedOrigins;
        }

        /**
         * 设置可信 Origin 列表。
         *
         * @param trustedOrigins 可信 Origin 列表
         */
        public void setTrustedOrigins(List<String> trustedOrigins) {
            this.trustedOrigins = Objects.requireNonNullElseGet(trustedOrigins, ArrayList::new);
        }
    }

    /**
     * MCP 请求大小限制配置。
     *
     * @author refinex
     */
    public static class RequestSize {

        /**
         * 是否启用请求大小限制。
         */
        private boolean enabled = true;

        /**
         * 最大请求体字节数。
         */
        @Min(1024)
        private long maxBytes = 1024 * 1024L;

        /**
         * 返回是否启用请求大小限制。
         *
         * @return 启用时返回 true
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用请求大小限制。
         *
         * @param enabled 是否启用请求大小限制
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 返回最大请求体字节数。
         *
         * @return 最大请求体字节数
         */
        public long getMaxBytes() {
            return maxBytes;
        }

        /**
         * 设置最大请求体字节数。
         *
         * @param maxBytes 最大请求体字节数
         */
        public void setMaxBytes(long maxBytes) {
            this.maxBytes = maxBytes;
        }
    }

    /**
     * MCP 基础限流配置。
     *
     * @author refinex
     */
    public static class RateLimit {

        /**
         * 是否启用基础限流。
         */
        private boolean enabled = true;

        /**
         * 固定窗口内允许请求数。
         */
        @Min(1)
        private int maxRequests = 120;

        /**
         * 限流固定窗口。
         */
        private Duration window = Duration.ofMinutes(1);

        /**
         * 返回是否启用基础限流。
         *
         * @return 启用时返回 true
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用基础限流。
         *
         * @param enabled 是否启用基础限流
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 返回固定窗口允许请求数。
         *
         * @return 固定窗口允许请求数
         */
        public int getMaxRequests() {
            return maxRequests;
        }

        /**
         * 设置固定窗口允许请求数。
         *
         * @param maxRequests 固定窗口允许请求数
         */
        public void setMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
        }

        /**
         * 返回限流固定窗口。
         *
         * @return 限流固定窗口
         */
        public Duration getWindow() {
            return window;
        }

        /**
         * 设置限流固定窗口。
         *
         * @param window 限流固定窗口
         */
        public void setWindow(Duration window) {
            this.window = Objects.requireNonNullElse(window, Duration.ofMinutes(1));
        }
    }
}
