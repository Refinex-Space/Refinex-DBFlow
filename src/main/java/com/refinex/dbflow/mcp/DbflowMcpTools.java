package com.refinex.dbflow.mcp;

import com.refinex.dbflow.audit.service.AuditRequestContext;
import com.refinex.dbflow.executor.*;
import com.refinex.dbflow.observability.DbflowMetricsService;
import com.refinex.dbflow.sqlpolicy.TruncateConfirmationConfirmRequest;
import com.refinex.dbflow.sqlpolicy.TruncateConfirmationDecision;
import com.refinex.dbflow.sqlpolicy.TruncateConfirmationService;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

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
     * @param truncateConfirmationService   TRUNCATE 服务端二次确认服务
     * @param sqlExecutionService           受控 SQL 执行服务
     * @param sqlExplainService             受控 SQL EXPLAIN 服务
     * @param schemaInspectService          schema inspect 服务
     * @param metricsServiceProvider        DBFlow 指标服务 provider
     */
    public DbflowMcpTools(
            McpAuthenticationContextResolver authenticationContextResolver,
            McpAccessBoundaryService accessBoundaryService,
            TruncateConfirmationService truncateConfirmationService,
            SqlExecutionService sqlExecutionService,
            SqlExplainService sqlExplainService,
            SchemaInspectService schemaInspectService,
            ObjectProvider<DbflowMetricsService> metricsServiceProvider
    ) {
        this.authenticationContextResolver = authenticationContextResolver;
        this.accessBoundaryService = accessBoundaryService;
        this.truncateConfirmationService = truncateConfirmationService;
        this.sqlExecutionService = sqlExecutionService;
        this.sqlExplainService = sqlExplainService;
        this.schemaInspectService = schemaInspectService;
        this.metricsService = metricsServiceProvider.getIfAvailable();
    }

    /**
     * 列出当前用户可访问的 DBFlow 目标项目环境。
     *
     * @return 可访问目标 skeleton 响应
     */
    @McpTool(
            name = DbflowMcpNames.TOOL_LIST_TARGETS,
            title = "List DBFlow targets",
            description = "List project/environment targets visible to the current MCP principal. Skeleton returns an empty list until target catalog projection is connected.",
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
        return response(DbflowMcpNames.TOOL_LIST_TARGETS, context, boundary, Map.of("targets", java.util.List.of()));
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
        SchemaInspectResult result = schemaInspectService.inspect(new SchemaInspectRequest(
                context.requestId(),
                context.userId(),
                context.tokenId(),
                context.tokenPrefix(),
                project,
                env,
                schema,
                table,
                maxItems == null ? 0 : maxItems
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
                "errorMessage", result.errorMessage()
        ));
    }

    /**
     * 获取目标项目环境的有效 SQL policy。
     *
     * @param project   项目标识
     * @param env       环境标识
     * @param schema    schema 名称
     * @param table     表名
     * @param operation SQL 操作类型
     * @return policy skeleton 响应
     */
    @McpTool(
            name = DbflowMcpNames.TOOL_GET_EFFECTIVE_POLICY,
            title = "Get effective DBFlow SQL policy",
            description = "Return the effective SQL policy for project/environment/schema/table/operation scope. Skeleton exposes default safe-policy shape only.",
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
        return response(DbflowMcpNames.TOOL_GET_EFFECTIVE_POLICY, context, boundary, data(
                "project", project,
                "env", env,
                "schema", schema,
                "table", table,
                "operation", operation,
                "defaults", Map.of("DROP_TABLE", "DENY", "DROP_DATABASE", "DENY", "TRUNCATE", "REQUIRE_CONFIRMATION"),
                "whitelist", java.util.List.of()
        ));
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
                "errorMessage", result.errorMessage()
        ));
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
        SqlExecutionResult result = sqlExecutionService.execute(new SqlExecutionRequest(
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
                "expiresAt", result.expiresAt()
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
        if (boundary.allowed()) {
            TruncateConfirmationDecision decision = truncateConfirmationService.confirm(
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
            return response(DbflowMcpNames.TOOL_CONFIRM_SQL, context, boundary, data(
                    "project", project,
                    "env", env,
                    "confirmationId", decision.confirmationId(),
                    "sqlReceived", sql != null && !sql.isBlank(),
                    "reason", reason,
                    "confirmed", decision.confirmed(),
                    "status", decision.status(),
                    "sqlHash", decision.sqlHash(),
                    "riskLevel", decision.riskLevel().name()
            ));
        }
        return response(DbflowMcpNames.TOOL_CONFIRM_SQL, context, boundary, data(
                "project", project,
                "env", env,
                "confirmationId", confirmationId,
                "sqlReceived", sql != null && !sql.isBlank(),
                "reason", reason,
                "confirmed", false
        ));
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
        return DbflowMcpSkeletonResponse.of(surface, context, boundary, data);
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

    /**
     * 创建允许 null value 的有序数据 Map。
     *
     * @param entries key/value 交替参数
     * @return 有序数据 Map
     */
    private Map<String, Object> data(Object... entries) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            values.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return values;
    }

    /**
     * 转换 warning 输出数据。
     *
     * @param warnings warning 摘要
     * @return MCP 响应数据
     */
    private java.util.List<Map<String, Object>> warningData(java.util.List<SqlExecutionWarning> warnings) {
        return warnings.stream()
                .map(warning -> data(
                        "level", warning.level(),
                        "code", warning.code(),
                        "message", warning.message()
                ))
                .toList();
    }

    /**
     * 转换 schema 输出数据。
     *
     * @param schemas schema 元数据
     * @return MCP 响应数据
     */
    private java.util.List<Map<String, Object>> schemaData(java.util.List<SchemaDatabaseMetadata> schemas) {
        return schemas.stream()
                .map(schema -> data(
                        "name", schema.name(),
                        "defaultCharacterSetName", schema.defaultCharacterSetName(),
                        "defaultCollationName", schema.defaultCollationName()
                ))
                .toList();
    }

    /**
     * 转换表输出数据。
     *
     * @param tables 表元数据
     * @return MCP 响应数据
     */
    private java.util.List<Map<String, Object>> tableData(java.util.List<SchemaTableMetadata> tables) {
        return tables.stream()
                .map(table -> data(
                        "schemaName", table.schemaName(),
                        "name", table.name(),
                        "type", table.type(),
                        "engine", table.engine(),
                        "rows", table.rows(),
                        "comment", table.comment()
                ))
                .toList();
    }

    /**
     * 转换字段输出数据。
     *
     * @param columns 字段元数据
     * @return MCP 响应数据
     */
    private java.util.List<Map<String, Object>> columnData(java.util.List<SchemaColumnMetadata> columns) {
        return columns.stream()
                .map(column -> data(
                        "schemaName", column.schemaName(),
                        "tableName", column.tableName(),
                        "name", column.name(),
                        "ordinalPosition", column.ordinalPosition(),
                        "dataType", column.dataType(),
                        "columnType", column.columnType(),
                        "nullable", column.nullable(),
                        "defaultValue", column.defaultValue(),
                        "comment", column.comment(),
                        "columnKey", column.columnKey(),
                        "extra", column.extra(),
                        "characterMaximumLength", column.characterMaximumLength(),
                        "numericPrecision", column.numericPrecision(),
                        "numericScale", column.numericScale()
                ))
                .toList();
    }

    /**
     * 转换索引输出数据。
     *
     * @param indexes 索引元数据
     * @return MCP 响应数据
     */
    private java.util.List<Map<String, Object>> indexData(java.util.List<SchemaIndexMetadata> indexes) {
        return indexes.stream()
                .map(index -> data(
                        "schemaName", index.schemaName(),
                        "tableName", index.tableName(),
                        "name", index.name(),
                        "nonUnique", index.nonUnique(),
                        "unique", index.unique(),
                        "seqInIndex", index.seqInIndex(),
                        "columnName", index.columnName(),
                        "indexType", index.indexType(),
                        "cardinality", index.cardinality(),
                        "nullable", index.nullable(),
                        "comment", index.comment(),
                        "indexComment", index.indexComment()
                ))
                .toList();
    }

    /**
     * 转换视图输出数据。
     *
     * @param views 视图元数据
     * @return MCP 响应数据
     */
    private java.util.List<Map<String, Object>> viewData(java.util.List<SchemaViewMetadata> views) {
        return views.stream()
                .map(view -> data(
                        "schemaName", view.schemaName(),
                        "name", view.name(),
                        "checkOption", view.checkOption(),
                        "updatable", view.updatable(),
                        "securityType", view.securityType(),
                        "definition", view.definition()
                ))
                .toList();
    }

    /**
     * 转换 routine 输出数据。
     *
     * @param routines routine 元数据
     * @return MCP 响应数据
     */
    private java.util.List<Map<String, Object>> routineData(java.util.List<SchemaRoutineMetadata> routines) {
        return routines.stream()
                .map(routine -> data(
                        "schemaName", routine.schemaName(),
                        "name", routine.name(),
                        "type", routine.type(),
                        "dataType", routine.dataType(),
                        "comment", routine.comment(),
                        "sqlDataAccess", routine.sqlDataAccess(),
                        "securityType", routine.securityType()
                ))
                .toList();
    }

    /**
     * 转换 EXPLAIN plan row 输出数据。
     *
     * @param planRows 执行计划行
     * @return MCP 响应数据
     */
    private java.util.List<Map<String, Object>> planRowData(java.util.List<SqlExplainPlanRow> planRows) {
        return planRows.stream()
                .map(row -> data(
                        "id", row.id(),
                        "selectType", row.selectType(),
                        "table", row.table(),
                        "type", row.type(),
                        "possibleKeys", row.possibleKeys(),
                        "key", row.key(),
                        "keyLen", row.keyLen(),
                        "ref", row.ref(),
                        "rows", row.rows(),
                        "filtered", row.filtered(),
                        "extra", row.extra(),
                        "raw", row.raw()
                ))
                .toList();
    }

    /**
     * 转换 EXPLAIN 建议输出数据。
     *
     * @param advice 建议列表
     * @return MCP 响应数据
     */
    private java.util.List<Map<String, Object>> adviceData(java.util.List<SqlExplainAdvice> advice) {
        return advice.stream()
                .map(item -> data(
                        "code", item.code(),
                        "severity", item.severity(),
                        "table", item.table(),
                        "message", item.message()
                ))
                .toList();
    }
}
