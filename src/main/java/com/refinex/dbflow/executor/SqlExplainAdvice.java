package com.refinex.dbflow.executor;

/**
 * 基础 SQL 执行计划建议。
 *
 * @param code     稳定建议代码
 * @param severity 严重级别
 * @param table    表名
 * @param message  建议摘要
 * @author refinex
 */
public record SqlExplainAdvice(
        String code,
        String severity,
        String table,
        String message
) {
}
