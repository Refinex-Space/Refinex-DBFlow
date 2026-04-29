package com.refinex.dbflow.sqlpolicy;

/**
 * SQL 风险等级。
 *
 * @author refinex
 */
public enum SqlRiskLevel {

    /**
     * 低风险。
     */
    LOW,

    /**
     * 中风险。
     */
    MEDIUM,

    /**
     * 高风险。
     */
    HIGH,

    /**
     * 致命风险。
     */
    CRITICAL,

    /**
     * 默认拒绝。
     */
    REJECTED
}
