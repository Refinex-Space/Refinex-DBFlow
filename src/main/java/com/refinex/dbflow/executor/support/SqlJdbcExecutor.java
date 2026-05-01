package com.refinex.dbflow.executor.support;

import com.refinex.dbflow.executor.datasource.ProjectEnvironmentDataSourceRegistry;
import com.refinex.dbflow.executor.dto.SqlExecutionOptions;
import com.refinex.dbflow.executor.dto.SqlExecutionRequest;
import com.refinex.dbflow.executor.dto.SqlExecutionResult;
import com.refinex.dbflow.executor.dto.SqlExecutionWarning;
import com.refinex.dbflow.sqlpolicy.dto.SqlClassification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.util.*;

/**
 * SQL JDBC 执行器，封装目标库连接、statement 限制、结果集读取和 warning 映射。
 *
 * @author refinex
 */
@Component
public class SqlJdbcExecutor {

    /**
     * 目标库数据源注册表。
     */
    private final ProjectEnvironmentDataSourceRegistry dataSourceRegistry;

    /**
     * 创建 SQL JDBC 执行器。
     *
     * @param dataSourceRegistry 目标库数据源注册表
     */
    public SqlJdbcExecutor(ProjectEnvironmentDataSourceRegistry dataSourceRegistry) {
        this.dataSourceRegistry = dataSourceRegistry;
    }

    /**
     * 通过 JDBC 执行目标 SQL。
     *
     * @param request        执行请求
     * @param classification SQL 分类结果
     * @param sqlText        标准化 SQL
     * @param sqlHash        SQL hash
     * @param status         成功状态
     * @return 执行结果
     */
    public SqlExecutionResult execute(
            SqlExecutionRequest request,
            SqlClassification classification,
            String sqlText,
            String sqlHash,
            String status
    ) {
        SqlExecutionOptions options = request.effectiveOptions();
        long started = System.nanoTime();
        try (Connection connection = dataSourceRegistry.getDataSource(request.projectKey(), request.environmentKey())
                .getConnection();
             java.sql.Statement statement = connection.createStatement()) {
            applySchema(connection, request.schema());
            applyStatementOptions(statement, options);
            boolean hasResultSet = statement.execute(sqlText);
            long durationMillis = elapsedMillis(started);
            return hasResultSet
                    ? queryResult(request, classification, sqlHash, statement, options, durationMillis, status)
                    : updateResult(request, classification, sqlHash, statement, durationMillis, status);
        } catch (SQLException exception) {
            long durationMillis = elapsedMillis(started);
            String sanitized = sanitize(exception);
            String summary = "SQL 执行失败，durationMillis=" + durationMillis + ", message=" + sanitized;
            throw new SqlJdbcExecutionException(summary, exception.getSQLState(), sanitized, exception);
        }
    }

    /**
     * 创建查询结果。
     *
     * @param request        执行请求
     * @param classification SQL 分类结果
     * @param sqlHash        SQL hash
     * @param statement      JDBC statement
     * @param options        执行限制
     * @param durationMillis 执行耗时
     * @param status         成功状态
     * @return 查询结果
     * @throws SQLException 查询结果读取失败时抛出
     */
    private SqlExecutionResult queryResult(
            SqlExecutionRequest request,
            SqlClassification classification,
            String sqlHash,
            java.sql.Statement statement,
            SqlExecutionOptions options,
            long durationMillis,
            String status
    ) throws SQLException {
        try (ResultSet resultSet = statement.getResultSet()) {
            ResultSetMetaData metadata = resultSet.getMetaData();
            List<String> columns = columns(metadata);
            List<Map<String, Object>> rows = new ArrayList<>();
            boolean truncated = false;
            while (resultSet.next()) {
                if (rows.size() >= options.maxRows()) {
                    truncated = true;
                    break;
                }
                rows.add(row(resultSet, columns));
            }
            List<SqlExecutionWarning> warnings = warnings(statement.getWarnings());
            String summary = "查询返回 " + rows.size() + " 行，truncated=" + truncated
                    + ", durationMillis=" + durationMillis;
            return SqlExecutionResultFactory.builder(request, classification)
                    .queryResult(columns, rows, truncated)
                    .warnings(warnings)
                    .durationMillis(durationMillis)
                    .statementSummary(summary)
                    .sqlHash(sqlHash)
                    .status(status)
                    .build();
        }
    }

    /**
     * 创建更新或 DDL 结果。
     *
     * @param request        执行请求
     * @param classification SQL 分类结果
     * @param sqlHash        SQL hash
     * @param statement      JDBC statement
     * @param durationMillis 执行耗时
     * @param status         成功状态
     * @return 更新或 DDL 结果
     * @throws SQLException 结果读取失败时抛出
     */
    private SqlExecutionResult updateResult(
            SqlExecutionRequest request,
            SqlClassification classification,
            String sqlHash,
            java.sql.Statement statement,
            long durationMillis,
            String status
    ) throws SQLException {
        long updateCount = statement.getUpdateCount();
        long affectedRows = updateCount < 0 ? 0L : updateCount;
        List<SqlExecutionWarning> warnings = warnings(statement.getWarnings());
        String summary = classification.operation().name() + " " + targetSummary(classification)
                + " affectedRows=" + affectedRows
                + ", warnings=" + warnings.size()
                + ", durationMillis=" + durationMillis;
        return SqlExecutionResultFactory.builder(request, classification)
                .affectedRows(affectedRows)
                .warnings(warnings)
                .durationMillis(durationMillis)
                .statementSummary(summary)
                .sqlHash(sqlHash)
                .status(status)
                .build();
    }

    /**
     * 应用默认 schema。
     *
     * @param connection 目标库连接
     * @param schema     schema 名称
     * @throws SQLException 设置失败时抛出
     */
    private void applySchema(Connection connection, String schema) throws SQLException {
        if (StringUtils.hasText(schema)) {
            connection.setCatalog(schema);
        }
    }

    /**
     * 应用 JDBC statement 安全限制。
     *
     * @param statement JDBC statement
     * @param options   执行限制
     * @throws SQLException 设置失败时抛出
     */
    private void applyStatementOptions(java.sql.Statement statement, SqlExecutionOptions options) throws SQLException {
        statement.setQueryTimeout(options.queryTimeoutSeconds());
        statement.setMaxRows(Math.addExact(options.maxRows(), 1));
        statement.setFetchSize(options.fetchSize());
    }

    /**
     * 读取结果集列名。
     *
     * @param metadata 结果集元数据
     * @return 列名列表
     * @throws SQLException 读取失败时抛出
     */
    private List<String> columns(ResultSetMetaData metadata) throws SQLException {
        List<String> columns = new ArrayList<>();
        for (int index = 1; index <= metadata.getColumnCount(); index++) {
            columns.add(metadata.getColumnLabel(index));
        }
        return List.copyOf(columns);
    }

    /**
     * 读取单行查询结果。
     *
     * @param resultSet 结果集
     * @param columns   列名列表
     * @return 单行结果
     * @throws SQLException 读取失败时抛出
     */
    private Map<String, Object> row(ResultSet resultSet, List<String> columns) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int index = 0; index < columns.size(); index++) {
            row.put(columns.get(index), resultSet.getObject(index + 1));
        }
        return Collections.unmodifiableMap(row);
    }

    /**
     * 读取 JDBC warning 链。
     *
     * @param warning JDBC warning
     * @return warning 摘要列表
     */
    private List<SqlExecutionWarning> warnings(SQLWarning warning) {
        List<SqlExecutionWarning> warnings = new ArrayList<>();
        SQLWarning current = warning;
        while (current != null) {
            warnings.add(new SqlExecutionWarning(current.getSQLState(), current.getErrorCode(), sanitize(current)));
            current = current.getNextWarning();
        }
        return List.copyOf(warnings);
    }

    /**
     * 创建目标摘要。
     *
     * @param classification SQL 分类结果
     * @return 目标摘要
     */
    private String targetSummary(SqlClassification classification) {
        String target = StringUtils.hasText(classification.targetTable())
                ? classification.targetTable()
                : classification.targetSchema();
        return StringUtils.hasText(target) ? target : "UNKNOWN_TARGET";
    }

    /**
     * 计算耗时毫秒。
     *
     * @param startedNanos 开始时间
     * @return 耗时毫秒
     */
    private long elapsedMillis(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    /**
     * 清理异常摘要，避免返回多行或过长错误。
     *
     * @param throwable 异常
     * @return 错误摘要
     */
    private String sanitize(Throwable throwable) {
        String message = throwable.getMessage();
        if (!StringUtils.hasText(message)) {
            return throwable.getClass().getSimpleName();
        }
        String singleLine = message.replaceAll("\\s+", " ").strip();
        return singleLine.length() <= 240 ? singleLine : singleLine.substring(0, 240);
    }
}
