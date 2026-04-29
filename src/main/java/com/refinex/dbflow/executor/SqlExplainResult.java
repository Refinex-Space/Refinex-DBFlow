package com.refinex.dbflow.executor;

import com.refinex.dbflow.sqlpolicy.SqlOperation;
import com.refinex.dbflow.sqlpolicy.SqlRiskLevel;

import java.util.List;

/**
 * SQL EXPLAIN 结果。
 *
 * @param projectKey       项目标识
 * @param environmentKey   环境标识
 * @param allowed          是否允许 explain
 * @param status           结果状态
 * @param operation        目标 SQL 操作
 * @param riskLevel        风险级别
 * @param format           EXPLAIN 输出格式
 * @param explainSql       实际执行的 EXPLAIN SQL
 * @param planRows         标准化执行计划行
 * @param advice           基础索引建议
 * @param jsonPlanSummary  JSON 计划摘要
 * @param durationMillis   执行耗时，单位毫秒
 * @param statementSummary 语句摘要
 * @param sqlHash          SQL hash
 * @param errorCode        稳定错误码
 * @param errorMessage     错误摘要
 * @author refinex
 */
public record SqlExplainResult(
        String projectKey,
        String environmentKey,
        boolean allowed,
        String status,
        SqlOperation operation,
        SqlRiskLevel riskLevel,
        String format,
        String explainSql,
        List<SqlExplainPlanRow> planRows,
        List<SqlExplainAdvice> advice,
        String jsonPlanSummary,
        long durationMillis,
        String statementSummary,
        String sqlHash,
        String errorCode,
        String errorMessage
) {
}
