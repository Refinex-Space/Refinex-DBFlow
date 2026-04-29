package com.refinex.dbflow.executor;

import com.refinex.dbflow.access.service.AccessDecision;
import com.refinex.dbflow.access.service.AccessDecisionReason;
import com.refinex.dbflow.access.service.AccessDecisionRequest;
import com.refinex.dbflow.access.service.AccessDecisionService;
import com.refinex.dbflow.audit.service.AuditEventWriteRequest;
import com.refinex.dbflow.audit.service.AuditEventWriter;
import com.refinex.dbflow.common.DbflowException;
import com.refinex.dbflow.common.ErrorCode;
import com.refinex.dbflow.sqlpolicy.SqlClassification;
import com.refinex.dbflow.sqlpolicy.SqlClassifier;
import com.refinex.dbflow.sqlpolicy.SqlOperation;
import com.refinex.dbflow.sqlpolicy.SqlRiskLevel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;

/**
 * 受控 SQL EXPLAIN 服务，只读取执行计划，不执行目标 DML。
 *
 * @author refinex
 */
@Service
public class SqlExplainService {

    /**
     * SQL hash 算法。
     */
    private static final String SQL_HASH_ALGORITHM = "SHA-256";

    /**
     * 已完成 EXPLAIN 状态。
     */
    private static final String STATUS_EXPLAINED = "EXPLAINED";

    /**
     * 审计允许状态。
     */
    private static final String AUDIT_STATUS_EXECUTED = "ALLOWED_EXECUTED";

    /**
     * 拒绝状态。
     */
    private static final String STATUS_DENIED = "DENIED";

    /**
     * 失败状态。
     */
    private static final String STATUS_FAILED = "FAILED";

    /**
     * MySQL 8 主版本号。
     */
    private static final int MYSQL_8_MAJOR_VERSION = 8;

    /**
     * 项目环境访问判断服务。
     */
    private final AccessDecisionService accessDecisionService;

    /**
     * SQL 分类服务。
     */
    private final SqlClassifier sqlClassifier;

    /**
     * 目标库数据源注册表。
     */
    private final ProjectEnvironmentDataSourceRegistry dataSourceRegistry;

    /**
     * 审计服务。
     */
    private final AuditEventWriter auditEventWriter;

    /**
     * 创建 SQL EXPLAIN 服务。
     *
     * @param accessDecisionService 项目环境访问判断服务
     * @param sqlClassifier         SQL 分类服务
     * @param dataSourceRegistry    目标库数据源注册表
     * @param auditEventWriter      统一审计事件写入器
     */
    public SqlExplainService(
            AccessDecisionService accessDecisionService,
            SqlClassifier sqlClassifier,
            ProjectEnvironmentDataSourceRegistry dataSourceRegistry,
            AuditEventWriter auditEventWriter
    ) {
        this.accessDecisionService = accessDecisionService;
        this.sqlClassifier = sqlClassifier;
        this.dataSourceRegistry = dataSourceRegistry;
        this.auditEventWriter = auditEventWriter;
    }

    /**
     * 执行受控 EXPLAIN。
     *
     * @param request EXPLAIN 请求
     * @return EXPLAIN 结果
     */
    @Transactional(noRollbackFor = DbflowException.class)
    public SqlExplainResult explain(SqlExplainRequest request) {
        Objects.requireNonNull(request, "request");
        String sqlText = safeSqlText(request.sql());
        String sqlHash = StringUtils.hasText(sqlText) ? sqlHash(sqlText) : null;
        auditEventWriter.requestReceived(auditRequest(request, SqlOperation.UNKNOWN, SqlRiskLevel.LOW, sqlHash,
                sqlText, "EXPLAIN 请求已接收", null, null));
        AccessDecision accessDecision = authorize(request);
        if (!accessDecision.allowed()) {
            return deny(request, SqlOperation.UNKNOWN, SqlRiskLevel.REJECTED, sqlHash, accessDecision.reason().name(),
                    "授权拒绝: " + accessDecision.reason().name() + " - " + accessDecision.message());
        }
        if (!StringUtils.hasText(sqlText)) {
            return deny(request, SqlOperation.UNKNOWN, SqlRiskLevel.REJECTED, sqlHash, "EMPTY_SQL", "SQL 不能为空");
        }

        SqlClassification classification = sqlClassifier.classify(sqlText);
        if (classification.rejectedByDefault()) {
            return deny(request, classification.operation(), classification.riskLevel(), sqlHash,
                    "CLASSIFICATION_REJECTED", "SQL 分类拒绝: " + classification.auditReason());
        }
        if (!explainable(classification.operation())) {
            return deny(request, classification.operation(), SqlRiskLevel.REJECTED, sqlHash, "NOT_EXPLAINABLE",
                    "EXPLAIN 仅支持 SELECT、INSERT、UPDATE 和 DELETE");
        }
        return explainJdbc(request, classification, sqlText, sqlHash);
    }

    /**
     * 执行授权检查。
     *
     * @param request EXPLAIN 请求
     * @return 授权结果
     */
    private AccessDecision authorize(SqlExplainRequest request) {
        if (request.userId() == null || request.tokenId() == null) {
            return AccessDecision.deny(AccessDecisionReason.TOKEN_NOT_FOUND, "MCP Token 上下文缺失");
        }
        return accessDecisionService.decide(new AccessDecisionRequest(
                request.userId(),
                request.tokenId(),
                request.projectKey(),
                request.environmentKey()
        ));
    }

    /**
     * 判断 SQL 操作是否可 explain。
     *
     * @param operation SQL 操作
     * @return 可 explain 时返回 true
     */
    private boolean explainable(SqlOperation operation) {
        return operation == SqlOperation.SELECT
                || operation == SqlOperation.INSERT
                || operation == SqlOperation.UPDATE
                || operation == SqlOperation.DELETE;
    }

    /**
     * 通过 JDBC 读取执行计划。
     *
     * @param request        EXPLAIN 请求
     * @param classification SQL 分类结果
     * @param sqlText        SQL 文本
     * @param sqlHash        SQL hash
     * @return EXPLAIN 结果
     */
    private SqlExplainResult explainJdbc(
            SqlExplainRequest request,
            SqlClassification classification,
            String sqlText,
            String sqlHash
    ) {
        long started = System.nanoTime();
        String explainSql = traditionalExplainSql(sqlText);
        try (Connection connection = dataSourceRegistry.getDataSource(request.projectKey(), request.environmentKey())
                .getConnection()) {
            applySchema(connection, request.schema());
            String jsonPlanSummary = null;
            if (supportsPreferredJsonExplain(connection)) {
                jsonPlanSummary = explainJson(connection, sqlText);
            }
            List<SqlExplainPlanRow> planRows = explainTraditional(connection, sqlText);
            List<SqlExplainAdvice> advice = advice(planRows);
            long durationMillis = elapsedMillis(started);
            String format = StringUtils.hasText(jsonPlanSummary) ? "JSON" : "TRADITIONAL";
            String summary = "EXPLAIN " + classification.operation().name()
                    + " planRows=" + planRows.size()
                    + ", advice=" + advice.size()
                    + ", format=" + format
                    + ", durationMillis=" + durationMillis;
            audit(request, classification.operation(), SqlRiskLevel.LOW, AUDIT_STATUS_EXECUTED, sqlHash, sqlText,
                    summary, null, null);
            return new SqlExplainResult(
                    request.projectKey(),
                    request.environmentKey(),
                    true,
                    STATUS_EXPLAINED,
                    classification.operation(),
                    classification.riskLevel(),
                    format,
                    explainSql,
                    planRows,
                    advice,
                    jsonPlanSummary,
                    durationMillis,
                    summary,
                    sqlHash,
                    null,
                    null
            );
        } catch (SQLException exception) {
            long durationMillis = elapsedMillis(started);
            String message = sanitize(exception);
            String summary = "EXPLAIN 执行失败，durationMillis=" + durationMillis + ", message=" + message;
            audit(request, classification.operation(), classification.riskLevel(), STATUS_FAILED, sqlHash, sqlText,
                    summary, exception.getSQLState(), message);
            return new SqlExplainResult(
                    request.projectKey(),
                    request.environmentKey(),
                    false,
                    STATUS_FAILED,
                    classification.operation(),
                    classification.riskLevel(),
                    "TRADITIONAL",
                    explainSql,
                    List.of(),
                    List.of(),
                    null,
                    durationMillis,
                    summary,
                    sqlHash,
                    exception.getSQLState(),
                    message
            );
        }
    }

    /**
     * 读取 JSON 格式执行计划。
     *
     * @param connection 目标库连接
     * @param sqlText    SQL 文本
     * @return JSON 计划摘要
     */
    private String explainJson(Connection connection, String sqlText) {
        try (Statement statement = connection.createStatement()) {
            applyStatementOptions(statement);
            try (ResultSet resultSet = statement.executeQuery("EXPLAIN FORMAT=JSON " + sqlText)) {
                if (resultSet.next()) {
                    return summarizeJsonPlan(resultSet.getString(1));
                }
                return null;
            }
        } catch (SQLException exception) {
            return null;
        }
    }

    /**
     * 读取传统格式执行计划。
     *
     * @param connection 目标库连接
     * @param sqlText    SQL 文本
     * @return 标准化执行计划行
     * @throws SQLException 读取失败时抛出
     */
    private List<SqlExplainPlanRow> explainTraditional(Connection connection, String sqlText) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            applyStatementOptions(statement);
            try (ResultSet resultSet = statement.executeQuery(traditionalExplainSql(sqlText))) {
                List<SqlExplainPlanRow> rows = new ArrayList<>();
                ResultSetMetaData metadata = resultSet.getMetaData();
                List<String> columns = columns(metadata);
                while (resultSet.next()) {
                    rows.add(planRow(resultSet, columns));
                }
                return List.copyOf(rows);
            }
        }
    }

    /**
     * 创建标准化执行计划行。
     *
     * @param resultSet 结果集
     * @param columns   结果列
     * @return 标准化执行计划行
     * @throws SQLException 读取失败时抛出
     */
    private SqlExplainPlanRow planRow(ResultSet resultSet, List<String> columns) throws SQLException {
        Map<String, Object> raw = rawRow(resultSet, columns);
        return new SqlExplainPlanRow(
                integerValue(raw, "id"),
                stringValue(raw, "select_type"),
                stringValue(raw, "table"),
                stringValue(raw, "type"),
                stringValue(raw, "possible_keys"),
                stringValue(raw, "key"),
                stringValue(raw, "key_len"),
                stringValue(raw, "ref"),
                longValue(raw, "rows"),
                decimalValue(raw, "filtered"),
                stringValue(raw, "extra"),
                raw
        );
    }

    /**
     * 创建基础索引建议。
     *
     * @param planRows 执行计划行
     * @return 建议列表
     */
    private List<SqlExplainAdvice> advice(List<SqlExplainPlanRow> planRows) {
        List<SqlExplainAdvice> advice = new ArrayList<>();
        for (SqlExplainPlanRow row : planRows) {
            String table = row.table();
            if (StringUtils.hasText(row.key())) {
                advice.add(new SqlExplainAdvice("INDEX_USED", "INFO", table, "执行计划使用索引 " + row.key()));
            }
            if ("ALL".equalsIgnoreCase(row.type()) || !StringUtils.hasText(row.key())) {
                advice.add(new SqlExplainAdvice("FULL_SCAN", "WARNING", table, "执行计划可能存在全表扫描或未命中索引"));
            }
            if (!StringUtils.hasText(row.possibleKeys()) && StringUtils.hasText(table)) {
                advice.add(new SqlExplainAdvice("NO_POSSIBLE_KEYS", "INFO", table, "优化器未发现可用候选索引"));
            }
        }
        return List.copyOf(advice);
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
     * 应用 EXPLAIN statement 限制。
     *
     * @param statement JDBC statement
     * @throws SQLException 设置失败时抛出
     */
    private void applyStatementOptions(Statement statement) throws SQLException {
        SqlExecutionOptions options = SqlExecutionOptions.defaults();
        statement.setQueryTimeout(options.queryTimeoutSeconds());
        statement.setMaxRows(Math.addExact(options.maxRows(), 1));
        statement.setFetchSize(options.fetchSize());
    }

    /**
     * 判断当前连接是否优先使用 JSON EXPLAIN。
     *
     * @param connection 目标库连接
     * @return MySQL 8 及以上返回 true
     * @throws SQLException 读取元数据失败时抛出
     */
    private boolean supportsPreferredJsonExplain(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        return metadata.getDatabaseProductName().toLowerCase(Locale.ROOT).contains("mysql")
                && metadata.getDatabaseMajorVersion() >= MYSQL_8_MAJOR_VERSION;
    }

    /**
     * 生成传统 EXPLAIN SQL。
     *
     * @param sqlText SQL 原文
     * @return EXPLAIN SQL
     */
    private String traditionalExplainSql(String sqlText) {
        return "EXPLAIN " + sqlText;
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
     * 读取原始行数据。
     *
     * @param resultSet 结果集
     * @param columns   列名
     * @return 原始行数据
     * @throws SQLException 读取失败时抛出
     */
    private Map<String, Object> rawRow(ResultSet resultSet, List<String> columns) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int index = 0; index < columns.size(); index++) {
            row.put(columns.get(index), resultSet.getObject(index + 1));
        }
        return Collections.unmodifiableMap(row);
    }

    /**
     * 按忽略大小写的 key 读取值。
     *
     * @param row 原始行
     * @param key 字段名
     * @return 字段值
     */
    private Object rawValue(Map<String, Object> row, String key) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 读取字符串值。
     *
     * @param row 原始行
     * @param key 字段名
     * @return 字符串值
     */
    private String stringValue(Map<String, Object> row, String key) {
        Object value = rawValue(row, key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text : null;
    }

    /**
     * 读取整数值。
     *
     * @param row 原始行
     * @param key 字段名
     * @return 整数值
     */
    private Integer integerValue(Map<String, Object> row, String key) {
        Long value = longValue(row, key);
        return value == null ? null : Math.toIntExact(value);
    }

    /**
     * 读取长整数值。
     *
     * @param row 原始行
     * @param key 字段名
     * @return 长整数值
     */
    private Long longValue(Map<String, Object> row, String key) {
        Object value = rawValue(row, key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null && StringUtils.hasText(String.valueOf(value))) {
            return Long.parseLong(String.valueOf(value));
        }
        return null;
    }

    /**
     * 读取小数值。
     *
     * @param row 原始行
     * @param key 字段名
     * @return 小数值
     */
    private BigDecimal decimalValue(Map<String, Object> row, String key) {
        Object value = rawValue(row, key);
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value != null && StringUtils.hasText(String.valueOf(value))) {
            return new BigDecimal(String.valueOf(value));
        }
        return null;
    }

    /**
     * 创建拒绝结果并审计。
     *
     * @param request      EXPLAIN 请求
     * @param operation    SQL 操作
     * @param riskLevel    风险级别
     * @param sqlHash      SQL hash
     * @param errorCode    错误码
     * @param errorMessage 错误摘要
     * @return 拒绝结果
     */
    private SqlExplainResult deny(
            SqlExplainRequest request,
            SqlOperation operation,
            SqlRiskLevel riskLevel,
            String sqlHash,
            String errorCode,
            String errorMessage
    ) {
        audit(request, operation, riskLevel, STATUS_DENIED, sqlHash, request.sql(), errorMessage, errorCode,
                errorMessage);
        return new SqlExplainResult(
                request.projectKey(),
                request.environmentKey(),
                false,
                STATUS_DENIED,
                operation,
                riskLevel,
                null,
                null,
                List.of(),
                List.of(),
                null,
                0L,
                errorMessage,
                sqlHash,
                errorCode,
                errorMessage
        );
    }

    /**
     * 记录 EXPLAIN 审计。
     *
     * @param request      EXPLAIN 请求
     * @param operation    SQL 操作
     * @param riskLevel    风险级别
     * @param status       审计状态
     * @param sqlHash      SQL hash
     * @param sqlText      SQL 原文
     * @param summary      摘要
     * @param errorCode    错误码
     * @param errorMessage 错误摘要
     */
    private void audit(
            SqlExplainRequest request,
            SqlOperation operation,
            SqlRiskLevel riskLevel,
            String status,
            String sqlHash,
            String sqlText,
            String summary,
            String errorCode,
            String errorMessage
    ) {
        AuditEventWriteRequest eventRequest = auditRequest(request, operation, riskLevel, sqlHash, sqlText, summary,
                errorCode, errorMessage);
        if (STATUS_DENIED.equals(status)) {
            auditEventWriter.policyDenied(eventRequest);
        } else if (STATUS_FAILED.equals(status)) {
            auditEventWriter.failed(eventRequest);
        } else {
            auditEventWriter.executed(eventRequest);
        }
    }

    /**
     * 创建审计写入请求。
     *
     * @param request      EXPLAIN 请求
     * @param operation    SQL 操作
     * @param riskLevel    风险等级
     * @param sqlHash      SQL hash
     * @param sqlText      SQL 原文
     * @param summary      结果摘要
     * @param errorCode    错误码
     * @param errorMessage 错误摘要
     * @return 审计写入请求
     */
    private AuditEventWriteRequest auditRequest(
            SqlExplainRequest request,
            SqlOperation operation,
            SqlRiskLevel riskLevel,
            String sqlHash,
            String sqlText,
            String summary,
            String errorCode,
            String errorMessage
    ) {
        return new AuditEventWriteRequest(
                request.requestId(),
                request.userId(),
                request.tokenId(),
                request.tokenPrefix(),
                request.auditContext(),
                request.projectKey(),
                request.environmentKey(),
                "EXPLAIN_" + operation.name(),
                auditRiskLevel(riskLevel),
                safeSqlText(sqlText),
                sqlHash,
                summary,
                0L,
                errorCode,
                errorMessage,
                null
        );
    }

    /**
     * 映射审计表允许的风险等级。
     *
     * @param riskLevel SQL 分类风险等级
     * @return 审计风险等级
     */
    private String auditRiskLevel(SqlRiskLevel riskLevel) {
        if (riskLevel == SqlRiskLevel.REJECTED) {
            return "FORBIDDEN";
        }
        return riskLevel.name();
    }

    /**
     * 生成 JSON 计划摘要。
     *
     * @param json 原始 JSON 计划
     * @return 截断后的 JSON 计划摘要
     */
    private String summarizeJsonPlan(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        String compact = json.replaceAll("\\s+", " ").strip();
        return compact.length() <= 2_000 ? compact : compact.substring(0, 2_000);
    }

    /**
     * 安全标准化 SQL 文本。
     *
     * @param sql SQL 原文
     * @return 标准化 SQL 或空字符串
     */
    private String safeSqlText(String sql) {
        return StringUtils.hasText(sql) ? sql.strip() : "";
    }

    /**
     * 生成 SQL SHA-256 hash。
     *
     * @param sql SQL 原文
     * @return Base64Url 编码 hash
     */
    private String sqlHash(String sql) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SQL_HASH_ALGORITHM);
            byte[] hashed = digest.digest(sql.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new DbflowException(ErrorCode.INTERNAL_ERROR, "SQL hash 算法不可用", exception);
        }
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
     * 清理异常摘要。
     *
     * @param exception SQL 异常
     * @return 错误摘要
     */
    private String sanitize(SQLException exception) {
        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) {
            return exception.getClass().getSimpleName();
        }
        String singleLine = message.replaceAll("\\s+", " ").strip();
        return singleLine.length() <= 240 ? singleLine : singleLine.substring(0, 240);
    }
}
