package com.refinex.dbflow.executor.dto;

import com.refinex.dbflow.sqlpolicy.model.SqlOperation;
import com.refinex.dbflow.sqlpolicy.model.SqlRiskLevel;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * SQL 执行结果摘要，禁止承载无限结果集。
 *
 * @param projectKey           项目标识
 * @param environmentKey       环境标识
 * @param operation            SQL 操作
 * @param riskLevel            风险等级
 * @param query                是否查询类结果
 * @param columns              查询列名
 * @param rows                 已限流查询行
 * @param truncated            是否因行数上限被截断
 * @param affectedRows         影响行数
 * @param warnings             warning 摘要
 * @param durationMillis       执行耗时，单位毫秒
 * @param statementSummary     语句摘要
 * @param sqlHash              SQL hash
 * @param confirmationRequired 是否需要服务端确认
 * @param confirmationId       确认挑战标识
 * @param expiresAt            确认过期时间
 * @param status               执行状态
 * @author refinex
 */
public record SqlExecutionResult(
        String projectKey,
        String environmentKey,
        SqlOperation operation,
        SqlRiskLevel riskLevel,
        boolean query,
        List<String> columns,
        List<Map<String, Object>> rows,
        boolean truncated,
        long affectedRows,
        List<SqlExecutionWarning> warnings,
        long durationMillis,
        String statementSummary,
        String sqlHash,
        boolean confirmationRequired,
        String confirmationId,
        Instant expiresAt,
        String status
) {
}
