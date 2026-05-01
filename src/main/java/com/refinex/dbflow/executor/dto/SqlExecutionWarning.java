package com.refinex.dbflow.executor.dto;

/**
 * SQL 执行 warning 摘要。
 *
 * @param level   warning 级别
 * @param code    数据库 warning 编码
 * @param message warning 摘要
 * @author refinex
 */
public record SqlExecutionWarning(
        String level,
        int code,
        String message
) {
}
