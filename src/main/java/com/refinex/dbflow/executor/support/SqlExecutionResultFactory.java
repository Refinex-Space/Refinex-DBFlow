package com.refinex.dbflow.executor.support;

import com.refinex.dbflow.executor.dto.SqlExecutionRequest;
import com.refinex.dbflow.executor.dto.SqlExecutionResult;
import com.refinex.dbflow.executor.dto.SqlExecutionWarning;
import com.refinex.dbflow.sqlpolicy.dto.SqlClassification;
import com.refinex.dbflow.sqlpolicy.model.SqlOperation;
import com.refinex.dbflow.sqlpolicy.model.SqlRiskLevel;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * SQL 执行结果工厂，集中构造外部稳定响应结构。
 *
 * @author refinex
 */
public final class SqlExecutionResultFactory {

    /**
     * 工具类不允许实例化。
     */
    private SqlExecutionResultFactory() {
    }

    /**
     * 创建 SQL 执行结果。
     *
     * @param request              执行请求
     * @param classification       SQL 分类结果
     * @param query                是否查询
     * @param columns              查询列
     * @param rows                 查询行
     * @param truncated            是否截断
     * @param affectedRows         影响行数
     * @param warnings             warning 摘要
     * @param durationMillis       执行耗时
     * @param statementSummary     语句摘要
     * @param sqlHash              SQL hash
     * @param confirmationRequired 是否需要确认
     * @param confirmationId       确认标识
     * @param expiresAt            过期时间
     * @param status               执行状态
     * @return SQL 执行结果
     */
    public static SqlExecutionResult create(
            SqlExecutionRequest request,
            SqlClassification classification,
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
        return new SqlExecutionResult(
                request.projectKey(),
                request.environmentKey(),
                classification.operation(),
                classification.riskLevel(),
                query,
                List.copyOf(columns),
                List.copyOf(rows),
                truncated,
                affectedRows,
                List.copyOf(warnings),
                durationMillis,
                statementSummary,
                sqlHash,
                confirmationRequired,
                confirmationId,
                expiresAt,
                status
        );
    }

    /**
     * 创建拒绝结果。
     *
     * @param request   执行请求
     * @param operation SQL 操作
     * @param riskLevel 风险等级
     * @param sqlHash   SQL hash
     * @param summary   拒绝摘要
     * @param status    执行状态
     * @return SQL 执行拒绝结果
     */
    public static SqlExecutionResult denied(
            SqlExecutionRequest request,
            SqlOperation operation,
            SqlRiskLevel riskLevel,
            String sqlHash,
            String summary,
            String status
    ) {
        return new SqlExecutionResult(
                request.projectKey(),
                request.environmentKey(),
                operation,
                riskLevel,
                false,
                List.of(),
                List.of(),
                false,
                0L,
                List.of(),
                0L,
                summary,
                sqlHash,
                false,
                null,
                null,
                status
        );
    }
}
