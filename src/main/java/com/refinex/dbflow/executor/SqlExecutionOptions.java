package com.refinex.dbflow.executor;

/**
 * SQL 执行安全限制选项。
 *
 * @param queryTimeoutSeconds 单条语句超时时间，单位秒
 * @param maxRows             查询最大返回行数
 * @param fetchSize           JDBC 抓取批大小
 * @author refinex
 */
public record SqlExecutionOptions(
        int queryTimeoutSeconds,
        int maxRows,
        int fetchSize
) {

    /**
     * 默认查询超时时间。
     */
    private static final int DEFAULT_QUERY_TIMEOUT_SECONDS = 10;

    /**
     * 默认查询最大返回行数。
     */
    private static final int DEFAULT_MAX_ROWS = 100;

    /**
     * 默认 JDBC 抓取批大小。
     */
    private static final int DEFAULT_FETCH_SIZE = 100;

    /**
     * 创建 SQL 执行限制选项。
     */
    public SqlExecutionOptions {
        if (queryTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("queryTimeoutSeconds 必须大于 0");
        }
        if (maxRows <= 0) {
            throw new IllegalArgumentException("maxRows 必须大于 0");
        }
        if (fetchSize <= 0) {
            throw new IllegalArgumentException("fetchSize 必须大于 0");
        }
    }

    /**
     * 创建默认执行限制。
     *
     * @return 默认执行限制
     */
    public static SqlExecutionOptions defaults() {
        return new SqlExecutionOptions(DEFAULT_QUERY_TIMEOUT_SECONDS, DEFAULT_MAX_ROWS, DEFAULT_FETCH_SIZE);
    }
}
