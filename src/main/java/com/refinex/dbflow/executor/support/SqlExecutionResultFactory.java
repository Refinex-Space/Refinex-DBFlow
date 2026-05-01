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
     * 创建 SQL 执行结果 Builder。
     *
     * @param request        执行请求
     * @param classification SQL 分类结果
     * @return SQL 执行结果 Builder
     */
    public static Builder builder(SqlExecutionRequest request, SqlClassification classification) {
        return new Builder(request, classification);
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
        return new SqlExecutionResult(request.projectKey(), request.environmentKey(), operation, riskLevel, false,
                List.of(), List.of(), false, 0L, List.of(), 0L, summary, sqlHash, false, null, null, status);
    }

    /**
     * SQL 执行结果 Builder，避免结果构造参数在调用端膨胀。
     */
    public static final class Builder {

        /**
         * 执行请求。
         */
        private final SqlExecutionRequest request;

        /**
         * SQL 分类结果。
         */
        private final SqlClassification classification;

        /**
         * 是否查询。
         */
        private boolean query;

        /**
         * 查询列。
         */
        private List<String> columns = List.of();

        /**
         * 查询行。
         */
        private List<Map<String, Object>> rows = List.of();

        /**
         * 是否截断。
         */
        private boolean truncated;

        /**
         * 影响行数。
         */
        private long affectedRows;

        /**
         * warning 摘要。
         */
        private List<SqlExecutionWarning> warnings = List.of();

        /**
         * 执行耗时。
         */
        private long durationMillis;

        /**
         * 语句摘要。
         */
        private String statementSummary;

        /**
         * SQL hash。
         */
        private String sqlHash;

        /**
         * 是否需要确认。
         */
        private boolean confirmationRequired;

        /**
         * 确认标识。
         */
        private String confirmationId;

        /**
         * 过期时间。
         */
        private Instant expiresAt;

        /**
         * 执行状态。
         */
        private String status;

        /**
         * 创建 SQL 执行结果 Builder。
         *
         * @param request        执行请求
         * @param classification SQL 分类结果
         */
        private Builder(SqlExecutionRequest request, SqlClassification classification) {
            this.request = request;
            this.classification = classification;
        }

        /**
         * 设置查询结果。
         *
         * @param columns   查询列
         * @param rows      查询行
         * @param truncated 是否截断
         * @return 当前 Builder
         */
        public Builder queryResult(List<String> columns, List<Map<String, Object>> rows, boolean truncated) {
            this.query = true;
            this.columns = List.copyOf(columns);
            this.rows = List.copyOf(rows);
            this.truncated = truncated;
            return this;
        }

        /**
         * 设置影响行数。
         *
         * @param affectedRows 影响行数
         * @return 当前 Builder
         */
        public Builder affectedRows(long affectedRows) {
            this.affectedRows = affectedRows;
            return this;
        }

        /**
         * 设置 warning 摘要。
         *
         * @param warnings warning 摘要
         * @return 当前 Builder
         */
        public Builder warnings(List<SqlExecutionWarning> warnings) {
            this.warnings = List.copyOf(warnings);
            return this;
        }

        /**
         * 设置执行耗时。
         *
         * @param durationMillis 执行耗时
         * @return 当前 Builder
         */
        public Builder durationMillis(long durationMillis) {
            this.durationMillis = durationMillis;
            return this;
        }

        /**
         * 设置语句摘要。
         *
         * @param statementSummary 语句摘要
         * @return 当前 Builder
         */
        public Builder statementSummary(String statementSummary) {
            this.statementSummary = statementSummary;
            return this;
        }

        /**
         * 设置 SQL hash。
         *
         * @param sqlHash SQL hash
         * @return 当前 Builder
         */
        public Builder sqlHash(String sqlHash) {
            this.sqlHash = sqlHash;
            return this;
        }

        /**
         * 设置确认信息。
         *
         * @param confirmationId 确认标识
         * @param expiresAt      过期时间
         * @return 当前 Builder
         */
        public Builder confirmationRequired(String confirmationId, Instant expiresAt) {
            this.confirmationRequired = true;
            this.confirmationId = confirmationId;
            this.expiresAt = expiresAt;
            return this;
        }

        /**
         * 设置执行状态。
         *
         * @param status 执行状态
         * @return 当前 Builder
         */
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        /**
         * 构建 SQL 执行结果。
         *
         * @return SQL 执行结果
         */
        public SqlExecutionResult build() {
            return new SqlExecutionResult(
                    request.projectKey(),
                    request.environmentKey(),
                    classification.operation(),
                    classification.riskLevel(),
                    query,
                    columns,
                    rows,
                    truncated,
                    affectedRows,
                    warnings,
                    durationMillis,
                    statementSummary,
                    sqlHash,
                    confirmationRequired,
                    confirmationId,
                    expiresAt,
                    status
            );
        }
    }
}
