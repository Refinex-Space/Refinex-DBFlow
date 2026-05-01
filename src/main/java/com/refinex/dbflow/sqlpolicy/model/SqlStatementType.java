package com.refinex.dbflow.sqlpolicy.model;

/**
 * SQL 语句类型。
 *
 * @author refinex
 */
public enum SqlStatementType {

    /**
     * 查询或只读检查语句。
     */
    QUERY,

    /**
     * 数据变更语句。
     */
    DML,

    /**
     * 结构变更语句。
     */
    DDL,

    /**
     * 权限或运维管理语句。
     */
    ADMIN,

    /**
     * 未知语句。
     */
    UNKNOWN
}
