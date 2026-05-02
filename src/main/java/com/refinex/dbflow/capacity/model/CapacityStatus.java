package com.refinex.dbflow.capacity.model;

/**
 * 容量决策状态，供 MCP 响应、指标和健康状态复用。
 *
 * @author refinex
 */
public enum CapacityStatus {

    /**
     * 请求允许继续执行。
     */
    ALLOWED,

    /**
     * 请求允许执行，但服务端已执行降级。
     */
    DEGRADED,

    /**
     * 请求因容量保护被拒绝。
     */
    REJECTED
}
