package com.refinex.dbflow.capacity.model;

/**
 * 容量治理维度，用于区分全局、工具、用户、Token 和目标库保护范围。
 *
 * @author refinex
 */
public enum CapacityScope {

    /**
     * 单实例全局容量范围。
     */
    GLOBAL,

    /**
     * MCP 工具类别容量范围。
     */
    TOOL_CLASS,

    /**
     * MCP Token 容量范围。
     */
    TOKEN,

    /**
     * 用户容量范围。
     */
    USER,

    /**
     * 目标 project/environment 容量范围。
     */
    TARGET
}
