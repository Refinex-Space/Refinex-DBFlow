package com.refinex.dbflow.capacity.model;

/**
 * MCP 暴露面的容量分级，用于区分轻量只读、重型只读、EXPLAIN 和真实执行路径。
 *
 * @author refinex
 */
public enum McpToolClass {

    /**
     * 轻量只读能力，例如目标列表、策略查看、prompt 和轻量 resource。
     */
    LIGHT_READ,

    /**
     * 重型只读能力，例如 schema inspect 和 schema resource。
     */
    HEAVY_READ,

    /**
     * 受控 EXPLAIN 能力，会访问目标库但不执行目标 DML。
     */
    EXPLAIN,

    /**
     * SQL 执行和高风险确认能力，容量策略最保守。
     */
    EXECUTE
}
