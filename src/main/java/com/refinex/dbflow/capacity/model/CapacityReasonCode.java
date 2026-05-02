package com.refinex.dbflow.capacity.model;

/**
 * 容量决策原因码，面向日志、指标和 MCP 错误元数据保持稳定。
 *
 * @author refinex
 */
public enum CapacityReasonCode {

    /**
     * 容量检查通过。
     */
    ALLOWED,

    /**
     * 容量治理被配置关闭。
     */
    CAPACITY_DISABLED,

    /**
     * 单实例本地资源处于压力态。
     */
    LOCAL_PRESSURE,

    /**
     * 目标项目环境连接池处于压力态。
     */
    TARGET_PRESSURE,

    /**
     * 全局并发舱壁已满。
     */
    GLOBAL_BULKHEAD_FULL,

    /**
     * 工具类别并发舱壁已满。
     */
    TOOL_BULKHEAD_FULL,

    /**
     * Token 并发舱壁已满。
     */
    TOKEN_BULKHEAD_FULL,

    /**
     * 用户并发舱壁已满。
     */
    USER_BULKHEAD_FULL,

    /**
     * 目标项目环境并发舱壁已满。
     */
    TARGET_BULKHEAD_FULL,

    /**
     * Token 固定窗口限流已耗尽。
     */
    TOKEN_RATE_LIMITED,

    /**
     * 用户固定窗口限流已耗尽。
     */
    USER_RATE_LIMITED,

    /**
     * 工具类别固定窗口限流已耗尽。
     */
    TOOL_RATE_LIMITED,

    /**
     * 目标项目环境固定窗口限流已耗尽。
     */
    TARGET_RATE_LIMITED
}
