package com.refinex.dbflow.mcp.tool;

import com.refinex.dbflow.audit.dto.AuditRequestContext;
import com.refinex.dbflow.capacity.model.CapacityDecision;
import com.refinex.dbflow.capacity.model.CapacityRequest;
import com.refinex.dbflow.capacity.model.McpToolClass;
import com.refinex.dbflow.capacity.service.CapacityGuardService;
import com.refinex.dbflow.capacity.support.CapacityPermit;
import com.refinex.dbflow.common.DbflowException;
import com.refinex.dbflow.executor.dto.*;
import com.refinex.dbflow.executor.service.SchemaInspectService;
import com.refinex.dbflow.executor.service.SqlExecutionService;
import com.refinex.dbflow.executor.service.SqlExplainService;
import com.refinex.dbflow.mcp.auth.McpAccessBoundaryService;
import com.refinex.dbflow.mcp.auth.McpAuthenticationContext;
import com.refinex.dbflow.mcp.auth.McpAuthenticationContextResolver;
import com.refinex.dbflow.mcp.auth.McpAuthorizationBoundary;
import com.refinex.dbflow.mcp.dto.DbflowMcpSkeletonResponse;
import com.refinex.dbflow.mcp.support.DbflowMcpNames;
import com.refinex.dbflow.mcp.support.McpTargetPolicyProjectionService;
import com.refinex.dbflow.observability.service.DbflowMetricsService;
import com.refinex.dbflow.sqlpolicy.dto.TruncateConfirmationConfirmRequest;
import com.refinex.dbflow.sqlpolicy.dto.TruncateConfirmationDecision;
import com.refinex.dbflow.sqlpolicy.service.TruncateConfirmationService;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.refinex.dbflow.mcp.support.McpErrorMetadataFactory.*;
import static com.refinex.dbflow.mcp.support.McpResponseBuilder.data;
import static com.refinex.dbflow.mcp.support.McpResponseBuilder.skeleton;
import static com.refinex.dbflow.mcp.support.McpSchemaMetadataMapper.*;

/**
 * DBFlow MCP 工具 skeleton。
 *
 * @author refinex
 */
@Component
public class DbflowMcpTools {

    /**
     * MCP 认证上下文解析器。
     */
    private final McpAuthenticationContextResolver authenticationContextResolver;

    /**
     * MCP 访问授权边界服务。
     */
    private final McpAccessBoundaryService accessBoundaryService;

    /**
     * 容量治理服务。
     */
    private final CapacityGuardService capacityGuardService;

    /**
     * MCP 目标与策略投影服务。
     */
    private final McpTargetPolicyProjectionService targetPolicyProjectionService;

    /**
     * TRUNCATE 服务端二次确认服务。
     */
    private final TruncateConfirmationService truncateConfirmationService;

    /**
     * 受控 SQL 执行服务。
     */
    private final SqlExecutionService sqlExecutionService;

    /**
     * 受控 SQL EXPLAIN 服务。
     */
    private final SqlExplainService sqlExplainService;

    /**
     * schema inspect 服务。
     */
    private final SchemaInspectService schemaInspectService;

    /**
     * DBFlow 指标服务，部分 slice 测试中允许不存在。
     */
    private final DbflowMetricsService metricsService;

    /**
     * 创建 DBFlow MCP 工具 skeleton。
     *
     * @param authenticationContextResolver MCP 认证上下文解析器
     * @param accessBoundaryService         MCP 访问授权边界服务
     * @param capacityGuardService          容量治理服务
     * @param targetPolicyProjectionService MCP 目标与策略投影服务
     * @param truncateConfirmationService   TRUNCATE 服务端二次确认服务
     * @param sqlExecutionService           受控 SQL 执行服务
     * @param sqlExplainService             受控 SQL EXPLAIN 服务
     * @param schemaInspectService          schema inspect 服务
     * @param metricsServiceProvider        DBFlow 指标服务 provider
     */
    public DbflowMcpTools(
            McpAuthenticationContextResolver authenticationContextResolver,
            McpAccessBoundaryService accessBoundaryService,
            CapacityGuardService capacityGuardService,
            McpTargetPolicyProjectionService targetPolicyProjectionService,
            TruncateConfirmationService truncateConfirmationService,
            SqlExecutionService sqlExecutionService,
            SqlExplainService sqlExplainService,
            SchemaInspectService schemaInspectService,
            ObjectProvider<DbflowMetricsService> metricsServiceProvider
    ) {
        this.authenticationContextResolver = authenticationContextResolver;
        this.accessBoundaryService = accessBoundaryService;
        this.capacityGuardService = capacityGuardService;
        this.targetPolicyProjectionService = targetPolicyProjectionService;
        this.truncateConfirmationService = truncateConfirmationService;
        this.sqlExecutionService = sqlExecutionService;
        this.sqlExplainService = sqlExplainService;
        this.schemaInspectService = schemaInspectService;
        this.metricsService = metricsServiceProvider.getIfAvailable();
    }

    /**
     * 列出当前用户可访问的 DBFlow 目标项目环境。
     *
     * @return 可访问目标响应
     */
    @McpTool(
            name = DbflowMcpNames.TOOL_LIST_TARGETS,
            title = "List DBFlow targets",
            description = "List project/environment targets visible to the current MCP principal after authentication and grant checks. Datasource secrets and raw JDBC URLs are never returned.",
            annotations = @McpTool.McpAnnotations(
                    title = "List DBFlow targets",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            ),
            generateOutputSchema = true
    )
    public DbflowMcpSkeletonResponse listTargets() {
        recordMcpCall(DbflowMcpNames.TOOL_LIST_TARGETS);
        McpAuthenticationContext context = authenticationContextResolver.currentContext();
        McpAuthorizationBoundary boundary = accessBoundaryService.metadataBoundary(
                context,
                DbflowMcpNames.TOOL_LIST_TARGETS
        );
        if (!boundary.allowed()) {
            return boundaryDenied(DbflowMcpNames.TOOL_LIST_TARGETS, context, boundary);
        }
        CapacityDecision capacityDecision = evaluateCapacity(
                context,
                DbflowMcpNames.TOOL_LIST_TARGETS,
                McpToolClass.LIGHT_READ,
                null,
                null,
                null
        );
        if (!capacityDecision.allowed()) {
            return capacityRejected(DbflowMcpNames.TOOL_LIST_TARGETS, context, boundary, capacityDecision,
                    data("targets", List.of()));
        }
        try (CapacityPermit ignored = capacityDecision.permit()) {
            return response(DbflowMcpNames.TOOL_LIST_TARGETS, context, boundary, data(
                    "targets", targetPolicyProjectionService.visibleTargets(
                            context,
                            accessBoundaryService,
                            DbflowMcpNames.TOOL_LIST_TARGETS
                    ),
                    "capacity", capacityData(capacityDecision),
                    "notices", notices(false, capacityDecision)
            ));
        }
    }

    /**
     * 查看目标项目环境 schema。
     *
     * @param project 项目标识
     * @param env     环境标识
     * @param schema  schema 名称
     * @param table   表名，空值表示返回 schema 摘要
     * @return schema skeleton 响应
     */
    @McpTool(
            name = DbflowMcpNames.TOOL_INSPECT_SCHEMA,
            title = "Inspect DBFlow schema",
            description = "Inspect schema/table metadata for a project environment after authentication and authorization checks. Returns bounded information_schema metadata without datasource secrets.",
            annotations = @McpTool.McpAnnotations(
                    title = "Inspect DBFlow schema",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            ),
            generateOutputSchema = true
    )
    public DbflowMcpSkeletonResponse inspectSchema(
            @McpToolParam(description = "Project key configured in dbflow.projects[].key.") String project,
            @McpToolParam(description = "Environment key configured under the project.") String env,
            @McpToolParam(required = false, description = "Database schema name to inspect. Omit it to use the target connection catalog.") String schema,
            @McpToolParam(required = false, description = "Optional table name. Omit it to inspect schema-level metadata.") String table,
            @McpToolParam(required = false, description = "Maximum rows returned per metadata category. Defaults to 100 and is capped server-side.") Integer maxItems
    ) {
        recordMcpCall(DbflowMcpNames.TOOL_INSPECT_SCHEMA);
        McpAuthenticationContext context = authenticationContextResolver.currentContext();
        McpAuthorizationBoundary boundary = accessBoundaryService.targetBoundary(
                context,
                project,
                env,
                DbflowMcpNames.TOOL_INSPECT_SCHEMA
        );
        if (!boundary.allowed()) {
            return boundaryDenied(DbflowMcpNames.TOOL_INSPECT_SCHEMA, context, boundary,
                    data("project", project, "env", env, "schema", schema, "table", table));
        }
        CapacityDecision capacityDecision = evaluateCapacity(
                context,
                DbflowMcpNames.TOOL_INSPECT_SCHEMA,
                McpToolClass.HEAVY_READ,
                project,
                env,
                maxItems
        );
        if (!capacityDecision.allowed()) {
            return capacityRejected(DbflowMcpNames.TOOL_INSPECT_SCHEMA, context, boundary, capacityDecision,
                    data("project", project, "env", env, "schema", schema, "table", table));
        }
        try (CapacityPermit ignored = capacityDecision.permit()) {
            SchemaInspectResult result = schemaInspectService.inspect(new SchemaInspectRequest(
                    context.requestId(),
                    context.userId(),
                    context.tokenId(),
                    context.tokenPrefix(),
                    project,
                    env,
                    schema,
                    table,
                    effectiveMaxItems(maxItems, capacityDecision)
            ));
            return response(DbflowMcpNames.TOOL_INSPECT_SCHEMA, context, boundary, data(
                    "project", result.projectKey(),
                    "env", result.environmentKey(),
                    "schema", result.schemaFilter(),
                    "table", result.tableFilter(),
                    "allowed", result.allowed(),
                    "status", result.status(),
                    "maxItems", result.maxItems(),
                    "truncated", result.truncated(),
                    "schemas", schemaData(result.schemas()),
                    "tables", tableData(result.tables()),
                    "columns", columnData(result.columns()),
                    "indexes", indexData(result.indexes()),
                    "views", viewData(result.views()),
                    "routines", routineData(result.routines()),
                    "durationMillis", result.durationMillis(),
                    "errorCode", result.errorCode(),
                    "errorMessage", sanitize(result.errorMessage()),
                    "error", errorData(result.errorCode(), result.errorMessage()),
                    "capacity", capacityData(capacityDecision),
                    "notices", notices(result.truncated(), capacityDecision)
            ));
        }
    }

    /**
     * 获取目标项目环境的有效 SQL policy。
     *
     * @param project   项目标识
     * @param env       环境标识
     * @param schema    schema 名称
     * @param table     表名
     * @param operation SQL 操作类型
     * @return policy 响应
     */
    @McpTool(
            name = DbflowMcpNames.TOOL_GET_EFFECTIVE_POLICY,
            title = "Get effective DBFlow SQL policy",
            description = "Return the effective SQL policy for project/environment/schema/table/operation scope from current DBFlow configuration after target authorization.",
            annotations = @McpTool.McpAnnotations(
                    title = "Get effective DBFlow SQL policy",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            ),
            generateOutputSchema = true
    )
    public DbflowMcpSkeletonResponse getEffectivePolicy(
            @McpToolParam(description = "Project key configured in dbflow.projects[].key.") String project,
            @McpToolParam(description = "Environment key configured under the project.") String env,
            @McpToolParam(required = false, description = "Optional schema name for policy narrowing.") String schema,
            @McpToolParam(required = false, description = "Optional table name for policy narrowing.") String table,
            @McpToolParam(required = false, description = "Optional SQL operation such as SELECT, UPDATE, TRUNCATE, DROP_TABLE, or DROP_DATABASE.") String operation
    ) {
        recordMcpCall(DbflowMcpNames.TOOL_GET_EFFECTIVE_POLICY);
        McpAuthenticationContext context = authenticationContextResolver.currentContext();
        McpAuthorizationBoundary boundary = accessBoundaryService.targetBoundary(
                context,
                project,
                env,
                DbflowMcpNames.TOOL_GET_EFFECTIVE_POLICY
        );
        if (!boundary.allowed()) {
            Map<String, Object> policy = targetPolicyProjectionService.effectivePolicy(
                    project,
                    env,
                    schema,
                    table,
                    operation,
                    boundary
            );
            policy.put("error", authorizationErrorData(boundary));
            return response(DbflowMcpNames.TOOL_GET_EFFECTIVE_POLICY, context, boundary, policy);
        }
        CapacityDecision capacityDecision = evaluateCapacity(
                context,
                DbflowMcpNames.TOOL_GET_EFFECTIVE_POLICY,
                McpToolClass.LIGHT_READ,
                project,
                env,
                null
        );
        if (!capacityDecision.allowed()) {
            return capacityRejected(DbflowMcpNames.TOOL_GET_EFFECTIVE_POLICY, context, boundary, capacityDecision,
                    data("project", project, "env", env, "schema", schema, "table", table, "operation", operation));
        }
        try (CapacityPermit ignored = capacityDecision.permit()) {
            Map<String, Object> policy = targetPolicyProjectionService.effectivePolicy(
                    project,
                    env,
                    schema,
                    table,
                    operation,
                    boundary
            );
            policy.put("capacity", capacityData(capacityDecision));
            policy.put("notices", notices(false, capacityDecision));
            return response(DbflowMcpNames.TOOL_GET_EFFECTIVE_POLICY, context, boundary, policy);
        }
    }

    /**
     * 生成 SQL EXPLAIN。
     *
     * @param project 项目标识
     * @param env     环境标识
     * @param sql     SQL 原文
     * @param schema  默认 schema
     * @return EXPLAIN 响应
     */
    @McpTool(
            name = DbflowMcpNames.TOOL_EXPLAIN_SQL,
            title = "Explain DBFlow SQL",
            description = "Run a safe EXPLAIN for SELECT and explainable DML after authentication, authorization, SQL classification, target datasource access, and audit. Target DML is not executed.",
            annotations = @McpTool.McpAnnotations(
                    title = "Explain DBFlow SQL",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            ),
            generateOutputSchema = true
    )
    public DbflowMcpSkeletonResponse explainSql(
            @McpToolParam(description = "Project key configured in dbflow.projects[].key.") String project,
            @McpToolParam(description = "Environment key configured under the project.") String env,
            @McpToolParam(description = "SQL text to explain. DML is only explained and is not executed.") String sql,
            @McpToolParam(required = false, description = "Optional default schema for SQL resolution.") String schema
    ) {
        recordMcpCall(DbflowMcpNames.TOOL_EXPLAIN_SQL);
        McpAuthenticationContext context = authenticationContextResolver.currentContext();
        McpAuthorizationBoundary boundary = accessBoundaryService.targetBoundary(
                context,
                project,
                env,
                DbflowMcpNames.TOOL_EXPLAIN_SQL
        );
        if (!boundary.allowed()) {
            return boundaryDenied(DbflowMcpNames.TOOL_EXPLAIN_SQL, context, boundary,
                    data("project", project, "env", env, "schema", schema, "sqlReceived",
                            sql != null && !sql.isBlank()));
        }
        CapacityDecision capacityDecision = evaluateCapacity(
                context,
                DbflowMcpNames.TOOL_EXPLAIN_SQL,
                McpToolClass.EXPLAIN,
                project,
                env,
                null
        );
        if (!capacityDecision.allowed()) {
            return capacityRejected(DbflowMcpNames.TOOL_EXPLAIN_SQL, context, boundary, capacityDecision,
                    data("project", project, "env", env, "schema", schema, "sqlReceived",
                            sql != null && !sql.isBlank()));
        }
        try (CapacityPermit ignored = capacityDecision.permit()) {
            SqlExplainResult result = sqlExplainService.explain(new SqlExplainRequest(
                    context.requestId(),
                    context.userId(),
                    context.tokenId(),
                    context.tokenPrefix(),
                    project,
                    env,
                    sql,
                    schema,
                    auditContext(context, DbflowMcpNames.TOOL_EXPLAIN_SQL)
            ));
            return response(DbflowMcpNames.TOOL_EXPLAIN_SQL, context, boundary, data(
                    "project", result.projectKey(),
                    "env", result.environmentKey(),
                    "schema", schema,
                    "sqlReceived", sql != null && !sql.isBlank(),
                    "allowed", result.allowed(),
                    "status", result.status(),
                    "operation", result.operation().name(),
                    "riskLevel", result.riskLevel().name(),
                    "format", result.format(),
                    "explainSql", result.explainSql(),
                    "planRows", planRowData(result.planRows()),
                    "advice", adviceData(result.advice()),
                    "jsonPlanSummary", result.jsonPlanSummary(),
                    "durationMillis", result.durationMillis(),
                    "statementSummary", result.statementSummary(),
                    "sqlHash", result.sqlHash(),
                    "errorCode", result.errorCode(),
                    "errorMessage", sanitize(result.errorMessage()),
                    "error", errorData(result.errorCode(), result.errorMessage()),
                    "capacity", capacityData(capacityDecision),
                    "notices", notices(false, capacityDecision)
            ));
        }
    }

    /**
     * 执行 SQL skeleton。
     *
     * @param project 项目标识
     * @param env     环境标识
     * @param sql     SQL 原文
     * @param schema  默认 schema
     * @param dryRun  是否只做试运行
     * @param reason  操作原因
     * @return SQL 执行 skeleton 响应
     */
    @McpTool(
            name = DbflowMcpNames.TOOL_EXECUTE_SQL,
            title = "Execute DBFlow SQL",
            description = "Execute SQL only after authentication, authorization, SQL policy, confirmation, target datasource, bounded result, and audit checks.",
            annotations = @McpTool.McpAnnotations(
                    title = "Execute DBFlow SQL",
                    readOnlyHint = false,
                    destructiveHint = true,
                    idempotentHint = false,
                    openWorldHint = false
            ),
            generateOutputSchema = true
    )
    public DbflowMcpSkeletonResponse executeSql(
            @McpToolParam(description = "Project key configured in dbflow.projects[].key.") String project,
            @McpToolParam(description = "Environment key configured under the project.") String env,
            @McpToolParam(description = "SQL text to execute after future policy checks.") String sql,
            @McpToolParam(required = false, description = "Optional default schema for SQL resolution.") String schema,
            @McpToolParam(required = false, description = "When true, future implementation should validate and explain without applying changes.") Boolean dryRun,
            @McpToolParam(required = false, description = "Human-readable reason for audit and confirmation context.") String reason
    ) {
        recordMcpCall(DbflowMcpNames.TOOL_EXECUTE_SQL);
        McpAuthenticationContext context = authenticationContextResolver.currentContext();
        McpAuthorizationBoundary boundary = accessBoundaryService.targetBoundary(
                context,
                project,
                env,
                DbflowMcpNames.TOOL_EXECUTE_SQL
        );
        if (!boundary.allowed()) {
            return boundaryDenied(DbflowMcpNames.TOOL_EXECUTE_SQL, context, boundary, data(
                    "project", project,
                    "env", env,
                    "schema", schema,
                    "sqlReceived", sql != null && !sql.isBlank(),
                    "dryRun", Boolean.TRUE.equals(dryRun),
                    "reason", reason
            ));
        }
        CapacityDecision capacityDecision = evaluateCapacity(
                context,
                DbflowMcpNames.TOOL_EXECUTE_SQL,
                McpToolClass.EXECUTE,
                project,
                env,
                null
        );
        if (!capacityDecision.allowed()) {
            return capacityRejected(DbflowMcpNames.TOOL_EXECUTE_SQL, context, boundary, capacityDecision, data(
                    "project", project,
                    "env", env,
                    "schema", schema,
                    "sqlReceived", sql != null && !sql.isBlank(),
                    "dryRun", Boolean.TRUE.equals(dryRun),
                    "reason", reason
            ));
        }
        SqlExecutionResult result;
        try (CapacityPermit ignored = capacityDecision.permit()) {
            result = sqlExecutionService.execute(new SqlExecutionRequest(
                    context.requestId(),
                    context.userId(),
                    context.tokenId(),
                    null,
                    project,
                    env,
                    sql,
                    schema,
                    Boolean.TRUE.equals(dryRun),
                    reason,
                    SqlExecutionOptions.defaults(),
                    auditContext(context, DbflowMcpNames.TOOL_EXECUTE_SQL)
            ));
        } catch (DbflowException exception) {
            return response(DbflowMcpNames.TOOL_EXECUTE_SQL, context, boundary, data(
                    "project", project,
                    "env", env,
                    "schema", schema,
                    "sqlReceived", sql != null && !sql.isBlank(),
                    "dryRun", Boolean.TRUE.equals(dryRun),
                    "reason", reason,
                    "query", false,
                    "truncated", false,
                    "affectedRows", 0L,
                    "status", "FAILED",
                    "error", errorData("SQL_EXECUTION_FAILED", exception.getMessage()),
                    "capacity", capacityData(capacityDecision)
            ));
        } catch (RuntimeException exception) {
            return response(DbflowMcpNames.TOOL_EXECUTE_SQL, context, boundary, data(
                    "project", project,
                    "env", env,
                    "schema", schema,
                    "sqlReceived", sql != null && !sql.isBlank(),
                    "dryRun", Boolean.TRUE.equals(dryRun),
                    "reason", reason,
                    "query", false,
                    "truncated", false,
                    "affectedRows", 0L,
                    "status", "FAILED",
                    "error", errorData("SQL_EXECUTION_FAILED", "SQL 执行失败"),
                    "capacity", capacityData(capacityDecision)
            ));
        }
        return response(DbflowMcpNames.TOOL_EXECUTE_SQL, context, boundary, data(
                "project", result.projectKey(),
                "env", result.environmentKey(),
                "schema", schema,
                "sqlReceived", sql != null && !sql.isBlank(),
                "dryRun", Boolean.TRUE.equals(dryRun),
                "reason", reason,
                "operation", result.operation().name(),
                "riskLevel", result.riskLevel().name(),
                "query", result.query(),
                "columns", result.columns(),
                "resultRows", result.rows(),
                "truncated", result.truncated(),
                "affectedRows", result.affectedRows(),
                "warnings", warningData(result.warnings()),
                "durationMillis", result.durationMillis(),
                "statementSummary", result.statementSummary(),
                "sqlHash", result.sqlHash(),
                "status", result.status(),
                "confirmationRequired", result.confirmationRequired(),
                "confirmationId", result.confirmationId(),
                "expiresAt", result.expiresAt(),
                "error", executionError(result),
                "capacity", capacityData(capacityDecision),
                "notices", notices(result.truncated(), capacityDecision)
        ));
    }

    /**
     * 确认高风险 SQL skeleton。
     *
     * @param project        项目标识
     * @param env            环境标识
     * @param confirmationId 确认挑战标识
     * @param sql            SQL 原文
     * @param reason         确认原因
     * @return SQL 确认 skeleton 响应
     */
    @McpTool(
            name = DbflowMcpNames.TOOL_CONFIRM_SQL,
            title = "Confirm DBFlow SQL",
            description = "Confirm a server-side SQL challenge before a future high-risk operation can proceed. The same user, token, project, environment, SQL hash, and non-expired challenge must match.",
            annotations = @McpTool.McpAnnotations(
                    title = "Confirm DBFlow SQL",
                    readOnlyHint = false,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            ),
            generateOutputSchema = true
    )
    public DbflowMcpSkeletonResponse confirmSql(
            @McpToolParam(description = "Project key configured in dbflow.projects[].key.") String project,
            @McpToolParam(description = "Environment key configured under the project.") String env,
            @McpToolParam(description = "Server-issued confirmation challenge id.") String confirmationId,
            @McpToolParam(description = "Original TRUNCATE SQL text. Server compares its hash with the challenge.") String sql,
            @McpToolParam(required = false, description = "Human-readable reason for audit context.") String reason
    ) {
        recordMcpCall(DbflowMcpNames.TOOL_CONFIRM_SQL);
        McpAuthenticationContext context = authenticationContextResolver.currentContext();
        McpAuthorizationBoundary boundary = accessBoundaryService.targetBoundary(
                context,
                project,
                env,
                DbflowMcpNames.TOOL_CONFIRM_SQL
        );
        if (!boundary.allowed()) {
            return response(DbflowMcpNames.TOOL_CONFIRM_SQL, context, boundary, data(
                    "project", project,
                    "env", env,
                    "confirmationId", confirmationId,
                    "sqlReceived", sql != null && !sql.isBlank(),
                    "reason", reason,
                    "confirmed", false,
                    "status", "DENIED",
                    "error", errorData(boundary.reason(), boundary.message())
            ));
        }
        CapacityDecision capacityDecision = evaluateCapacity(
                context,
                DbflowMcpNames.TOOL_CONFIRM_SQL,
                McpToolClass.EXECUTE,
                project,
                env,
                null
        );
        if (!capacityDecision.allowed()) {
            return capacityRejected(DbflowMcpNames.TOOL_CONFIRM_SQL, context, boundary, capacityDecision, data(
                    "project", project,
                    "env", env,
                    "confirmationId", confirmationId,
                    "sqlReceived", sql != null && !sql.isBlank(),
                    "reason", reason,
                    "confirmed", false
            ));
        }
        try (CapacityPermit ignored = capacityDecision.permit()) {
            TruncateConfirmationDecision decision;
            try {
                decision = truncateConfirmationService.confirm(
                        new TruncateConfirmationConfirmRequest(
                                context.requestId(),
                                context.userId(),
                                context.tokenId(),
                                context.tokenPrefix(),
                                project,
                                env,
                                confirmationId,
                                sql,
                                Instant.now(),
                                auditContext(context, DbflowMcpNames.TOOL_CONFIRM_SQL)
                        )
                );
            } catch (DbflowException exception) {
                String code = confirmationErrorCode(exception.getMessage());
                return response(DbflowMcpNames.TOOL_CONFIRM_SQL, context, boundary, data(
                        "project", project,
                        "env", env,
                        "confirmationId", confirmationId,
                        "sqlReceived", sql != null && !sql.isBlank(),
                        "reason", reason,
                        "confirmed", false,
                        "status", code,
                        "error", errorData(code, exception.getMessage()),
                        "capacity", capacityData(capacityDecision)
                ));
            }
            return response(DbflowMcpNames.TOOL_CONFIRM_SQL, context, boundary, data(
                    "project", project,
                    "env", env,
                    "confirmationId", decision.confirmationId(),
                    "sqlReceived", sql != null && !sql.isBlank(),
                    "reason", reason,
                    "confirmed", decision.confirmed(),
                    "status", decision.status(),
                    "sqlHash", decision.sqlHash(),
                    "riskLevel", decision.riskLevel().name(),
                    "capacity", capacityData(capacityDecision),
                    "notices", notices(false, capacityDecision)
            ));
        }
    }

    /**
     * 创建 MCP skeleton 响应。
     *
     * @param surface  MCP 暴露面名称
     * @param context  MCP 认证上下文
     * @param boundary MCP 授权边界结果
     * @param data     响应数据
     * @return MCP skeleton 响应
     */
    private DbflowMcpSkeletonResponse response(
            String surface,
            McpAuthenticationContext context,
            McpAuthorizationBoundary boundary,
            Map<String, Object> data
    ) {
        return skeleton(surface, context, boundary, data);
    }

    /**
     * 创建授权拒绝响应。
     *
     * @param surface  MCP 暴露面名称
     * @param context  MCP 认证上下文
     * @param boundary MCP 授权边界结果
     * @return MCP skeleton 响应
     */
    private DbflowMcpSkeletonResponse boundaryDenied(
            String surface,
            McpAuthenticationContext context,
            McpAuthorizationBoundary boundary
    ) {
        return boundaryDenied(surface, context, boundary, data());
    }

    /**
     * 创建带基础字段的授权拒绝响应。
     *
     * @param surface  MCP 暴露面名称
     * @param context  MCP 认证上下文
     * @param boundary MCP 授权边界结果
     * @param values   基础响应字段
     * @return MCP skeleton 响应
     */
    private DbflowMcpSkeletonResponse boundaryDenied(
            String surface,
            McpAuthenticationContext context,
            McpAuthorizationBoundary boundary,
            Map<String, Object> values
    ) {
        values.put("status", "DENIED");
        values.put("error", authorizationErrorData(boundary));
        return response(surface, context, boundary, values);
    }

    /**
     * 创建兼容 MCP tool 既有协议的授权错误数据。
     *
     * @param boundary MCP 授权边界结果
     * @return 授权错误数据
     */
    private Map<String, Object> authorizationErrorData(McpAuthorizationBoundary boundary) {
        if ("AUTHENTICATION_REQUIRED".equals(boundary.reason())) {
            return errorData(boundary.reason(), boundary.message());
        }
        return errorData("POLICY_DENIED", boundary.message());
    }

    /**
     * 创建容量治理拒绝响应。
     *
     * @param surface          MCP 暴露面名称
     * @param context          MCP 认证上下文
     * @param boundary         MCP 授权边界结果
     * @param capacityDecision 容量治理决策
     * @param values           基础响应字段
     * @return MCP skeleton 响应
     */
    private DbflowMcpSkeletonResponse capacityRejected(
            String surface,
            McpAuthenticationContext context,
            McpAuthorizationBoundary boundary,
            CapacityDecision capacityDecision,
            Map<String, Object> values
    ) {
        values.put("allowed", false);
        values.put("status", "CAPACITY_REJECTED");
        values.put("error", capacityError(capacityDecision));
        values.put("capacity", capacityData(capacityDecision));
        values.put("notices", notices(false, capacityDecision));
        return response(surface, context, boundary, values);
    }

    /**
     * 执行容量治理评估。
     *
     * @param context       MCP 认证上下文
     * @param tool          MCP 工具名称
     * @param toolClass     MCP 工具容量分级
     * @param project       项目标识
     * @param env           环境标识
     * @param requestedSize 客户端请求条目数
     * @return 容量治理决策
     */
    private CapacityDecision evaluateCapacity(
            McpAuthenticationContext context,
            String tool,
            McpToolClass toolClass,
            String project,
            String env,
            Integer requestedSize
    ) {
        return capacityGuardService.evaluate(new CapacityRequest(
                context.requestId(),
                context.userId(),
                context.tokenId(),
                tool,
                toolClass,
                project,
                env,
                requestedSize
        ));
    }

    /**
     * 计算容量降级后的最大条目数。
     *
     * @param requested        客户端请求条目数
     * @param capacityDecision 容量治理决策
     * @return 实际传入服务层的条目数
     */
    private int effectiveMaxItems(Integer requested, CapacityDecision capacityDecision) {
        int requestedValue = requested == null ? 0 : requested;
        Integer override = capacityDecision.maxItemsOverride();
        if (override == null) {
            return requestedValue;
        }
        if (requestedValue <= 0) {
            return override;
        }
        return Math.min(requestedValue, override);
    }

    /**
     * 创建审计请求来源上下文。
     *
     * @param context MCP 认证上下文
     * @param tool    工具名称
     * @return 审计请求来源上下文
     */
    private AuditRequestContext auditContext(McpAuthenticationContext context, String tool) {
        return AuditRequestContext.fromClientInfo(
                context.clientInfo(),
                context.userAgent(),
                context.sourceIp(),
                tool
        );
    }

    /**
     * 记录 MCP 工具调用指标。
     *
     * @param tool 工具名称
     */
    private void recordMcpCall(String tool) {
        if (metricsService != null) {
            metricsService.recordMcpCall(tool);
        }
    }

}
