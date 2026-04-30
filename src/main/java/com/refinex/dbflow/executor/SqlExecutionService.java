package com.refinex.dbflow.executor;

import com.refinex.dbflow.access.service.AccessDecision;
import com.refinex.dbflow.access.service.AccessDecisionReason;
import com.refinex.dbflow.access.service.AccessDecisionRequest;
import com.refinex.dbflow.access.service.AccessDecisionService;
import com.refinex.dbflow.audit.service.AuditEventWriteRequest;
import com.refinex.dbflow.audit.service.AuditEventWriter;
import com.refinex.dbflow.common.DbflowException;
import com.refinex.dbflow.common.ErrorCode;
import com.refinex.dbflow.observability.DbflowMetricsService;
import com.refinex.dbflow.observability.LogContext;
import com.refinex.dbflow.sqlpolicy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * 受控 SQL 执行服务，统一执行授权、分类、策略、确认、目标库访问和审计。
 *
 * @author refinex
 */
@Service
public class SqlExecutionService {

    /**
     * 运维日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlExecutionService.class);

    /**
     * SQL hash 算法。
     */
    private static final String SQL_HASH_ALGORITHM = "SHA-256";

    /**
     * 审计状态：已授权并执行。
     */
    private static final String STATUS_EXECUTED = "ALLOWED_EXECUTED";

    /**
     * 审计状态：已拒绝。
     */
    private static final String STATUS_DENIED = "DENIED";

    /**
     * 审计状态：执行失败。
     */
    private static final String STATUS_FAILED = "FAILED";

    /**
     * 审计状态：只验证未执行。
     */
    private static final String STATUS_DRY_RUN = "ALLOWED_EXECUTED";

    /**
     * 项目环境访问判断服务。
     */
    private final AccessDecisionService accessDecisionService;

    /**
     * SQL 分类服务。
     */
    private final SqlClassifier sqlClassifier;

    /**
     * DROP 高危 DDL 策略引擎。
     */
    private final DangerousDdlPolicyEngine dangerousDdlPolicyEngine;

    /**
     * TRUNCATE 确认服务。
     */
    private final TruncateConfirmationService truncateConfirmationService;

    /**
     * 目标库数据源注册表。
     */
    private final ProjectEnvironmentDataSourceRegistry dataSourceRegistry;

    /**
     * 审计服务。
     */
    private final AuditEventWriter auditEventWriter;

    /**
     * DBFlow 指标服务，部分 slice 测试中允许不存在。
     */
    private final DbflowMetricsService metricsService;

    /**
     * 创建受控 SQL 执行服务。
     *
     * @param accessDecisionService       项目环境访问判断服务
     * @param sqlClassifier               SQL 分类服务
     * @param dangerousDdlPolicyEngine    DROP 高危 DDL 策略引擎
     * @param truncateConfirmationService TRUNCATE 确认服务
     * @param dataSourceRegistry          目标库数据源注册表
     * @param auditEventWriter            统一审计事件写入器
     * @param metricsServiceProvider      DBFlow 指标服务 provider
     */
    public SqlExecutionService(
            AccessDecisionService accessDecisionService,
            SqlClassifier sqlClassifier,
            DangerousDdlPolicyEngine dangerousDdlPolicyEngine,
            TruncateConfirmationService truncateConfirmationService,
            ProjectEnvironmentDataSourceRegistry dataSourceRegistry,
            AuditEventWriter auditEventWriter,
            ObjectProvider<DbflowMetricsService> metricsServiceProvider
    ) {
        this.accessDecisionService = accessDecisionService;
        this.sqlClassifier = sqlClassifier;
        this.dangerousDdlPolicyEngine = dangerousDdlPolicyEngine;
        this.truncateConfirmationService = truncateConfirmationService;
        this.dataSourceRegistry = dataSourceRegistry;
        this.auditEventWriter = auditEventWriter;
        this.metricsService = metricsServiceProvider.getIfAvailable();
    }

    /**
     * 执行受控 SQL。
     *
     * @param request 执行请求
     * @return 执行结果
     */
    @Transactional(noRollbackFor = DbflowException.class)
    public SqlExecutionResult execute(SqlExecutionRequest request) {
        Objects.requireNonNull(request, "request");
        try (LogContext.Scope ignored = LogContext.withCorrelation(
                request.requestId(),
                LogContext.currentTraceIdOrDefault(request.requestId()))) {
            long metricsStarted = System.nanoTime();
            LOGGER.info("sql.execution.received project={} env={} dryRun={} schemaPresent={}",
                    request.projectKey(), request.environmentKey(), request.dryRun(), StringUtils.hasText(request.schema()));
            try {
                SqlExecutionResult result = executeInternal(request);
                recordExecutionDuration(result.operation().name(), result.riskLevel().name(), result.status(), metricsStarted);
                LOGGER.info(
                        "sql.execution.completed project={} env={} operation={} risk={} status={} sqlHash={} "
                                + "durationMillis={} affectedRows={} truncated={}",
                        result.projectKey(), result.environmentKey(), result.operation(), result.riskLevel(),
                        result.status(), result.sqlHash(), result.durationMillis(), result.affectedRows(),
                        result.truncated());
                return result;
            } catch (RuntimeException exception) {
                recordExecutionDuration(SqlOperation.UNKNOWN.name(), SqlRiskLevel.REJECTED.name(), STATUS_FAILED,
                        metricsStarted);
                LOGGER.warn("sql.execution.failed project={} env={} errorType={}",
                        request.projectKey(), request.environmentKey(), exception.getClass().getSimpleName());
                throw exception;
            }
        }
    }

    /**
     * 执行受控 SQL 主流程。
     *
     * @param request 执行请求
     * @return 执行结果
     */
    private SqlExecutionResult executeInternal(SqlExecutionRequest request) {
        AccessDecision accessDecision = authorize(request);
        String sqlText = safeSqlText(request.sql());
        String sqlHash = StringUtils.hasText(sqlText) ? sqlHash(sqlText) : null;
        auditEventWriter.requestReceived(auditRequest(request, SqlOperation.UNKNOWN, SqlRiskLevel.LOW, sqlHash,
                sqlText, "SQL 请求已接收", 0L, null, null, null));
        if (!accessDecision.allowed()) {
            LOGGER.warn("sql.policy.denied phase=authorization project={} env={} reason={}",
                    request.projectKey(), request.environmentKey(), accessDecision.reason());
            return deny(request, SqlOperation.UNKNOWN, SqlRiskLevel.REJECTED, sqlHash,
                    "授权拒绝: " + accessDecision.reason().name() + " - " + accessDecision.message());
        }
        if (!StringUtils.hasText(sqlText)) {
            LOGGER.warn("sql.policy.denied phase=validation project={} env={} reason=EMPTY_SQL",
                    request.projectKey(), request.environmentKey());
            return deny(request, SqlOperation.UNKNOWN, SqlRiskLevel.REJECTED, sqlHash, "SQL 不能为空");
        }

        SqlClassification classification = sqlClassifier.classify(sqlText);
        if (classification.rejectedByDefault()) {
            LOGGER.warn("sql.policy.denied phase=classification project={} env={} operation={} risk={} reason={} "
                            + "sqlHash={}",
                    request.projectKey(), request.environmentKey(), classification.operation(),
                    classification.riskLevel(), classification.auditReason(), sqlHash);
            return deny(request, classification.operation(), classification.riskLevel(), sqlHash,
                    "SQL 分类拒绝: " + classification.auditReason());
        }

        DangerousDdlPolicyDecision policyDecision = dangerousDdlPolicyEngine.decide(
                request.projectKey(),
                request.environmentKey(),
                classification
        );
        if (!policyDecision.allowed()) {
            LOGGER.warn("sql.policy.denied phase=dangerous-ddl project={} env={} operation={} risk={} reasonCode={} "
                            + "sqlHash={}",
                    request.projectKey(), request.environmentKey(), classification.operation(),
                    classification.riskLevel(), policyDecision.reasonCode(), sqlHash);
            return deny(request, classification.operation(), classification.riskLevel(), sqlHash,
                    "SQL 策略拒绝: " + policyDecision.reasonCode().name() + " - " + policyDecision.reason());
        }

        if (classification.operation() == SqlOperation.TRUNCATE) {
            LOGGER.info("sql.execution.requires-confirmation project={} env={} operation={} risk={} sqlHash={}",
                    request.projectKey(), request.environmentKey(), classification.operation(),
                    classification.riskLevel(), sqlHash);
            return requireConfirmation(request, classification);
        }
        if (request.dryRun()) {
            return dryRun(request, classification, sqlHash);
        }
        return executeJdbc(request, classification, sqlText, sqlHash);
    }

    /**
     * 记录 SQL 执行耗时指标。
     *
     * @param operation      SQL 操作
     * @param riskLevel      风险等级
     * @param status         执行状态
     * @param metricsStarted 指标开始时间纳秒
     */
    private void recordExecutionDuration(String operation, String riskLevel, String status, long metricsStarted) {
        if (metricsService != null) {
            metricsService.recordSqlExecutionDuration(operation, riskLevel, status, metricsStarted);
        }
    }

    /**
     * 执行授权检查。
     *
     * @param request 执行请求
     * @return 授权结果
     */
    private AccessDecision authorize(SqlExecutionRequest request) {
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
     * 通过 JDBC 执行目标 SQL。
     *
     * @param request        执行请求
     * @param classification SQL 分类结果
     * @param sqlText        标准化 SQL
     * @param sqlHash        SQL hash
     * @return 执行结果
     */
    private SqlExecutionResult executeJdbc(
            SqlExecutionRequest request,
            SqlClassification classification,
            String sqlText,
            String sqlHash
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
            SqlExecutionResult result = hasResultSet
                    ? queryResult(request, classification, sqlHash, statement, options, durationMillis)
                    : updateResult(request, classification, sqlHash, statement, durationMillis);
            audit(request, classification.operation(), classification.riskLevel(), STATUS_EXECUTED, sqlHash, sqlText,
                    result.statementSummary(), result.affectedRows(), null, null);
            return result;
        } catch (SQLException exception) {
            long durationMillis = elapsedMillis(started);
            String summary = "SQL 执行失败，durationMillis=" + durationMillis + ", message=" + sanitize(exception);
            audit(request, classification.operation(), classification.riskLevel(), STATUS_FAILED, sqlHash, sqlText,
                    summary, 0L, exception.getSQLState(), sanitize(exception));
            throw new DbflowException(ErrorCode.INTERNAL_ERROR, "SQL 执行失败: " + sanitize(exception), exception);
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
     * @return 查询结果
     * @throws SQLException 查询结果读取失败时抛出
     */
    private SqlExecutionResult queryResult(
            SqlExecutionRequest request,
            SqlClassification classification,
            String sqlHash,
            java.sql.Statement statement,
            SqlExecutionOptions options,
            long durationMillis
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
            return result(request, classification, true, columns, rows, truncated, 0L, warnings,
                    durationMillis, summary, sqlHash, false, null, null, STATUS_EXECUTED);
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
     * @return 更新或 DDL 结果
     * @throws SQLException 结果读取失败时抛出
     */
    private SqlExecutionResult updateResult(
            SqlExecutionRequest request,
            SqlClassification classification,
            String sqlHash,
            java.sql.Statement statement,
            long durationMillis
    ) throws SQLException {
        long updateCount = statement.getUpdateCount();
        long affectedRows = updateCount < 0 ? 0L : updateCount;
        List<SqlExecutionWarning> warnings = warnings(statement.getWarnings());
        String summary = classification.operation().name() + " " + targetSummary(classification)
                + " affectedRows=" + affectedRows
                + ", warnings=" + warnings.size()
                + ", durationMillis=" + durationMillis;
        return result(request, classification, false, List.of(), List.of(), false, affectedRows, warnings,
                durationMillis, summary, sqlHash, false, null, null, STATUS_EXECUTED);
    }

    /**
     * 创建确认需求结果。
     *
     * @param request        执行请求
     * @param classification SQL 分类结果
     * @return 确认需求结果
     */
    private SqlExecutionResult requireConfirmation(SqlExecutionRequest request, SqlClassification classification) {
        TruncateConfirmationDecision decision = truncateConfirmationService.createChallenge(
                new TruncateConfirmationRequest(
                        request.requestId(),
                        request.userId(),
                        request.tokenId(),
                        request.tokenPrefix(),
                        request.projectKey(),
                        request.environmentKey(),
                        request.sql(),
                        Instant.now()
                )
        );
        return result(request, classification, false, List.of(), List.of(), false, 0L, List.of(),
                0L, "TRUNCATE 需要服务端二次确认", decision.sqlHash(), true, decision.confirmationId(),
                decision.expiresAt(), "REQUIRES_CONFIRMATION");
    }

    /**
     * 创建 dry-run 结果。
     *
     * @param request        执行请求
     * @param classification SQL 分类结果
     * @param sqlHash        SQL hash
     * @return dry-run 结果
     */
    private SqlExecutionResult dryRun(SqlExecutionRequest request, SqlClassification classification, String sqlHash) {
        String summary = "dry-run 已完成授权、分类和策略检查，未访问目标库";
        audit(request, classification.operation(), classification.riskLevel(), STATUS_DRY_RUN, sqlHash, request.sql(),
                summary, 0L, null, null);
        return result(request, classification, false, List.of(), List.of(), false, 0L, List.of(),
                0L, summary, sqlHash, false, null, null, STATUS_DRY_RUN);
    }

    /**
     * 创建拒绝结果并记录审计。
     *
     * @param request   执行请求
     * @param operation SQL 操作
     * @param riskLevel 风险等级
     * @param sqlHash   SQL hash
     * @param summary   拒绝摘要
     * @return 拒绝结果
     */
    private SqlExecutionResult deny(
            SqlExecutionRequest request,
            SqlOperation operation,
            SqlRiskLevel riskLevel,
            String sqlHash,
            String summary
    ) {
        audit(request, operation, riskLevel, STATUS_DENIED, sqlHash, request.sql(), summary, 0L, "DENIED", summary);
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
                STATUS_DENIED
        );
    }

    /**
     * 记录 SQL 执行审计。
     *
     * @param request      执行请求
     * @param operation    SQL 操作
     * @param riskLevel    风险等级
     * @param status       审计状态
     * @param sqlHash      SQL hash
     * @param sqlText      SQL 原文
     * @param summary      结果摘要
     * @param affectedRows 影响行数
     * @param errorCode    错误码
     * @param errorMessage 错误摘要
     */
    private void audit(
            SqlExecutionRequest request,
            SqlOperation operation,
            SqlRiskLevel riskLevel,
            String status,
            String sqlHash,
            String sqlText,
            String summary,
            long affectedRows,
            String errorCode,
            String errorMessage
    ) {
        AuditEventWriteRequest eventRequest = auditRequest(request, operation, riskLevel, sqlHash, sqlText, summary,
                affectedRows, errorCode, errorMessage, null);
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
     * @param request        执行请求
     * @param operation      SQL 操作
     * @param riskLevel      风险等级
     * @param sqlHash        SQL hash
     * @param sqlText        SQL 原文
     * @param summary        结果摘要
     * @param affectedRows   影响行数
     * @param errorCode      错误码
     * @param errorMessage   错误摘要
     * @param confirmationId 确认挑战标识
     * @return 审计写入请求
     */
    private AuditEventWriteRequest auditRequest(
            SqlExecutionRequest request,
            SqlOperation operation,
            SqlRiskLevel riskLevel,
            String sqlHash,
            String sqlText,
            String summary,
            Long affectedRows,
            String errorCode,
            String errorMessage,
            String confirmationId
    ) {
        return new AuditEventWriteRequest(
                request.requestId(),
                request.userId(),
                request.tokenId(),
                request.tokenPrefix(),
                request.auditContext(),
                request.projectKey(),
                request.environmentKey(),
                operation.name(),
                auditRiskLevel(riskLevel),
                safeSqlText(sqlText),
                sqlHash,
                summary,
                affectedRows,
                errorCode,
                errorMessage,
                confirmationId
        );
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
     * 创建执行结果。
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
     * @return 执行结果
     */
    private SqlExecutionResult result(
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
     * 标准化 SQL 文本。
     *
     * @param sql SQL 原文
     * @return 标准化 SQL
     */
    private String normalizeSql(String sql) {
        if (!StringUtils.hasText(sql)) {
            throw new DbflowException(ErrorCode.INVALID_REQUEST, "SQL 不能为空");
        }
        return sql.strip();
    }

    /**
     * 安全标准化 SQL 文本，允许空值进入授权拒绝审计。
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
     * 清理异常摘要，避免返回多行或过长错误。
     *
     * @param exception SQL 异常
     * @return 错误摘要
     */
    private String sanitize(SQLException exception) {
        return sanitize((Throwable) exception);
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
