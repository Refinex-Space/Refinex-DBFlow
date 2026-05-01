package com.refinex.dbflow.executor.support;

import java.sql.SQLException;

/**
 * SQL JDBC 执行失败异常，携带已脱敏的审计摘要。
 *
 * @author refinex
 */
public class SqlJdbcExecutionException extends RuntimeException {

    /**
     * SQLState。
     */
    private final String sqlState;

    /**
     * 脱敏错误摘要。
     */
    private final String sanitizedMessage;

    /**
     * 执行结果摘要。
     */
    private final String summary;

    /**
     * 创建 SQL JDBC 执行失败异常。
     *
     * @param summary          执行结果摘要
     * @param sqlState         SQLState
     * @param sanitizedMessage 脱敏错误摘要
     * @param cause            原始 SQL 异常
     */
    public SqlJdbcExecutionException(String summary, String sqlState, String sanitizedMessage, SQLException cause) {
        super(summary, cause);
        this.summary = summary;
        this.sqlState = sqlState;
        this.sanitizedMessage = sanitizedMessage;
    }

    /**
     * 读取 SQLState。
     *
     * @return SQLState
     */
    public String sqlState() {
        return sqlState;
    }

    /**
     * 读取脱敏错误摘要。
     *
     * @return 脱敏错误摘要
     */
    public String sanitizedMessage() {
        return sanitizedMessage;
    }

    /**
     * 读取执行结果摘要。
     *
     * @return 执行结果摘要
     */
    public String summary() {
        return summary;
    }
}
