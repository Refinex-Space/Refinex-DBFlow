package com.refinex.dbflow.sqlpolicy.model;

/**
 * SQL 解析状态。
 *
 * @author refinex
 */
public enum SqlParseStatus {

    /**
     * SQL 已成功解析。
     */
    SUCCESS,

    /**
     * SQL 解析失败。
     */
    PARSE_FAILED,

    /**
     * SQL 被识别为多语句并默认拒绝。
     */
    MULTI_STATEMENT_REJECTED
}
