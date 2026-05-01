package com.refinex.dbflow.config.model;

/**
 * 当前阶段支持配置的高危 DDL 操作类型。
 *
 * @author refinex
 */
public enum DangerousDdlOperation {

    /**
     * 删除表操作。
     */
    DROP_TABLE,

    /**
     * 删除数据库或 schema 操作。
     */
    DROP_DATABASE,

    /**
     * 清空表操作。
     */
    TRUNCATE
}
