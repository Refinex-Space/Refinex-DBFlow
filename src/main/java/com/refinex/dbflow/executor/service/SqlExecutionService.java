package com.refinex.dbflow.executor.service;

import com.refinex.dbflow.access.dto.AccessDecision;
import com.refinex.dbflow.access.dto.AccessDecisionRequest;
import com.refinex.dbflow.access.model.AccessDecisionReason;
import com.refinex.dbflow.access.service.AccessDecisionService;
import com.refinex.dbflow.common.DbflowException;
import com.refinex.dbflow.common.ErrorCode;
import com.refinex.dbflow.executor.dto.SqlExecutionRequest;
import com.refinex.dbflow.executor.dto.SqlExecutionResult;
import com.refinex.dbflow.executor.support.SqlExecutionAuditor;
import com.refinex.dbflow.executor.support.SqlExecutionAuditor.SqlExecutionAuditPayload;
import com.refinex.dbflow.executor.support.SqlExecutionResultFactory;
import com.refinex.dbflow.executor.support.SqlJdbcExecutionException;
import com.refinex.dbflow.executor.support.SqlJdbcExecutor;
import com.refinex.dbflow.observability.service.DbflowMetricsService;
import com.refinex.dbflow.observability.support.LogContext;
import com.refinex.dbflow.sqlpolicy.dto.DangerousDdlPolicyDecision;
import com.refinex.dbflow.sqlpolicy.dto.SqlClassification;
import com.refinex.dbflow.sqlpolicy.dto.TruncateConfirmationDecision;
import com.refinex.dbflow.sqlpolicy.dto.TruncateConfirmationRequest;
import com.refinex.dbflow.sqlpolicy.model.SqlOperation;
import com.refinex.dbflow.sqlpolicy.model.SqlRiskLevel;
import com.refinex.dbflow.sqlpolicy.service.DangerousDdlPolicyEngine;
import com.refinex.dbflow.sqlpolicy.service.SqlClassifier;
import com.refinex.dbflow.sqlpolicy.service.TruncateConfirmationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

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
     * SQL JDBC 执行器。
     */
    private final SqlJdbcExecutor sqlJdbcExecutor;

    /**
     * SQL 执行审计编排器。
     */
    private final SqlExecutionAuditor sqlExecutionAuditor;

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
     * @param sqlJdbcExecutor             SQL JDBC 执行器
     * @param sqlExecutionAuditor         SQL 执行审计编排器
     * @param metricsServiceProvider      DBFlow 指标服务 provider
     */
    public SqlExecutionService(
            AccessDecisionService accessDecisionService,
            SqlClassifier sqlClassifier,
            DangerousDdlPolicyEngine dangerousDdlPolicyEngine,
            TruncateConfirmationService truncateConfirmationService,
            SqlJdbcExecutor sqlJdbcExecutor,
            SqlExecutionAuditor sqlExecutionAuditor,
            ObjectProvider<DbflowMetricsService> metricsServiceProvider
    ) {
        this.accessDecisionService = accessDecisionService;
        this.sqlClassifier = sqlClassifier;
        this.dangerousDdlPolicyEngine = dangerousDdlPolicyEngine;
        this.truncateConfirmationService = truncateConfirmationService;
        this.sqlJdbcExecutor = sqlJdbcExecutor;
        this.sqlExecutionAuditor = sqlExecutionAuditor;
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
            } catch (DbflowException exception) {
                recordExecutionDuration(SqlOperation.UNKNOWN.name(), SqlRiskLevel.REJECTED.name(), STATUS_FAILED,
                        metricsStarted);
                throw new DbflowException(
                        exception.getErrorCode(),
                        executionFailureMessage(request, exception),
                        exception
                );
            } catch (RuntimeException exception) {
                recordExecutionDuration(SqlOperation.UNKNOWN.name(), SqlRiskLevel.REJECTED.name(), STATUS_FAILED,
                        metricsStarted);
                throw new DbflowException(
                        ErrorCode.INTERNAL_ERROR,
                        executionFailureMessage(request, exception),
                        exception
                );
            }
        }
    }

    /**
     * 构造执行失败上下文消息。
     *
     * @param request   执行请求
     * @param exception 原始异常
     * @return 带上下文的错误消息
     */
    private String executionFailureMessage(SqlExecutionRequest request, RuntimeException exception) {
        return "SQL 执行流程异常: requestId=" + request.requestId()
                + ", project=" + request.projectKey()
                + ", env=" + request.environmentKey()
                + ", error=" + exception.getClass().getSimpleName()
                + ", message=" + exception.getMessage();
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
        sqlExecutionAuditor.requestReceived(request, sqlHash, sqlText);
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
        try {
            SqlExecutionResult result = sqlJdbcExecutor.execute(request, classification, sqlText, sqlHash,
                    STATUS_EXECUTED);
            sqlExecutionAuditor.audit(request, new SqlExecutionAuditPayload(
                    classification.operation(),
                    classification.riskLevel(),
                    STATUS_EXECUTED,
                    sqlHash,
                    sqlText,
                    result.statementSummary(),
                    result.affectedRows(),
                    null,
                    null
            ));
            return result;
        } catch (SqlJdbcExecutionException exception) {
            sqlExecutionAuditor.audit(request, new SqlExecutionAuditPayload(
                    classification.operation(),
                    classification.riskLevel(),
                    STATUS_FAILED,
                    sqlHash,
                    sqlText,
                    exception.summary(),
                    0L,
                    exception.sqlState(),
                    exception.sanitizedMessage()
            ));
            throw new DbflowException(ErrorCode.INTERNAL_ERROR,
                    "SQL 执行失败: " + exception.sanitizedMessage(), exception.getCause());
        }
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
        return confirmationRequiredResult(request, classification, decision);
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
        sqlExecutionAuditor.audit(request, new SqlExecutionAuditPayload(
                classification.operation(),
                classification.riskLevel(),
                STATUS_DRY_RUN,
                sqlHash,
                request.sql(),
                summary,
                0L,
                null,
                null
        ));
        return dryRunResult(request, classification, summary, sqlHash);
    }

    /**
     * 创建确认需求结果。
     *
     * @param request        执行请求
     * @param classification SQL 分类结果
     * @param decision       确认决策
     * @return 确认需求结果
     */
    private SqlExecutionResult confirmationRequiredResult(
            SqlExecutionRequest request,
            SqlClassification classification,
            TruncateConfirmationDecision decision
    ) {
        return SqlExecutionResultFactory.create(
                request,
                classification,
                false,
                List.of(),
                List.of(),
                false,
                0L,
                List.of(),
                0L,
                "TRUNCATE 需要服务端二次确认",
                decision.sqlHash(),
                true,
                decision.confirmationId(),
                decision.expiresAt(),
                "REQUIRES_CONFIRMATION"
        );
    }

    /**
     * 创建 dry-run 结果。
     *
     * @param request        执行请求
     * @param classification SQL 分类结果
     * @param summary        执行摘要
     * @param sqlHash        SQL hash
     * @return dry-run 结果
     */
    private SqlExecutionResult dryRunResult(
            SqlExecutionRequest request,
            SqlClassification classification,
            String summary,
            String sqlHash
    ) {
        return SqlExecutionResultFactory.create(
                request,
                classification,
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
                STATUS_DRY_RUN
        );
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
        sqlExecutionAuditor.audit(request, new SqlExecutionAuditPayload(
                operation,
                riskLevel,
                STATUS_DENIED,
                sqlHash,
                request.sql(),
                summary,
                0L,
                "DENIED",
                summary
        ));
        return SqlExecutionResultFactory.denied(request, operation, riskLevel, sqlHash, summary, STATUS_DENIED);
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

}
