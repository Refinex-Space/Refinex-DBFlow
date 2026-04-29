package com.refinex.dbflow.mcp;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

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
     * 创建 DBFlow MCP 工具 skeleton。
     *
     * @param authenticationContextResolver MCP 认证上下文解析器
     * @param accessBoundaryService         MCP 访问授权边界服务
     */
    public DbflowMcpTools(
            McpAuthenticationContextResolver authenticationContextResolver,
            McpAccessBoundaryService accessBoundaryService
    ) {
        this.authenticationContextResolver = authenticationContextResolver;
        this.accessBoundaryService = accessBoundaryService;
    }

    /**
     * 列出当前用户可访问的 DBFlow 目标项目环境。
     *
     * @return 可访问目标 skeleton 响应
     */
    @McpTool(
            name = DbflowMcpNames.TOOL_LIST_TARGETS,
            title = "List DBFlow targets",
            description = "List project/environment targets visible to the current MCP principal. Skeleton returns an empty list until MCP Bearer Token authentication is connected.",
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
            description = "Inspect schema/table metadata for a project environment after authentication and authorization checks. Skeleton returns empty schema metadata.",
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
            @McpToolParam(description = "Database schema name to inspect.") String schema,
            @McpToolParam(required = false, description = "Optional table name. Omit it to inspect schema-level metadata.") String table
    ) {
        McpAuthenticationContext context = authenticationContextResolver.currentContext();
        McpAuthorizationBoundary boundary = accessBoundaryService.targetBoundary(
                context,
                project,
                env,
                DbflowMcpNames.TOOL_INSPECT_SCHEMA
        );
        return response(DbflowMcpNames.TOOL_INSPECT_SCHEMA, context, boundary, data(
                "project", project,
                "env", env,
                "schema", schema,
                "table", table,
                "columns", java.util.List.of(),
                "indexes", java.util.List.of()
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
     * 生成 SQL EXPLAIN skeleton。
     *
     * @param project 项目标识
     * @param env     环境标识
     * @param sql     SQL 原文
     * @param schema  默认 schema
     * @return EXPLAIN skeleton 响应
     */
    @McpTool(
            name = DbflowMcpNames.TOOL_EXPLAIN_SQL,
            title = "Explain DBFlow SQL",
            description = "Run a safe EXPLAIN boundary for SQL before execution. Skeleton does not connect to a target database and returns an empty plan.",
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
            @McpToolParam(description = "SQL text to explain. It will not be executed in this skeleton phase.") String sql,
            @McpToolParam(required = false, description = "Optional default schema for SQL resolution.") String schema
    ) {
        McpAuthenticationContext context = authenticationContextResolver.currentContext();
        McpAuthorizationBoundary boundary = accessBoundaryService.targetBoundary(
                context,
                project,
                env,
                DbflowMcpNames.TOOL_EXPLAIN_SQL
        );
        return response(DbflowMcpNames.TOOL_EXPLAIN_SQL, context, boundary, data(
                "project", project,
                "env", env,
                "schema", schema,
                "sqlReceived", sql != null && !sql.isBlank(),
                "plan", java.util.List.of()
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
            description = "Execute SQL only after authentication, authorization, SQL policy, confirmation, and audit checks. Skeleton never executes SQL and returns no result set.",
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
        McpAuthenticationContext context = authenticationContextResolver.currentContext();
        McpAuthorizationBoundary boundary = accessBoundaryService.targetBoundary(
                context,
                project,
                env,
                DbflowMcpNames.TOOL_EXECUTE_SQL
        );
        return response(DbflowMcpNames.TOOL_EXECUTE_SQL, context, boundary, data(
                "project", project,
                "env", env,
                "schema", schema,
                "sqlReceived", sql != null && !sql.isBlank(),
                "dryRun", Boolean.TRUE.equals(dryRun),
                "reason", reason,
                "resultRows", java.util.List.of(),
                "affectedRows", 0,
                "confirmationRequired", false
        ));
    }

    /**
     * 确认高风险 SQL skeleton。
     *
     * @param confirmationId   确认挑战标识
     * @param confirmationCode 确认码
     * @param reason           确认原因
     * @return SQL 确认 skeleton 响应
     */
    @McpTool(
            name = DbflowMcpNames.TOOL_CONFIRM_SQL,
            title = "Confirm DBFlow SQL",
            description = "Confirm a server-side SQL challenge before a future high-risk operation can proceed. Skeleton validates only the authentication boundary shape.",
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
            @McpToolParam(description = "Server-issued confirmation challenge id.") String confirmationId,
            @McpToolParam(description = "Confirmation code supplied by the operator.") String confirmationCode,
            @McpToolParam(required = false, description = "Human-readable reason for audit context.") String reason
    ) {
        McpAuthenticationContext context = authenticationContextResolver.currentContext();
        McpAuthorizationBoundary boundary = accessBoundaryService.metadataBoundary(
                context,
                DbflowMcpNames.TOOL_CONFIRM_SQL
        );
        return response(DbflowMcpNames.TOOL_CONFIRM_SQL, context, boundary, data(
                "confirmationId", confirmationId,
                "confirmationCodeReceived", confirmationCode != null && !confirmationCode.isBlank(),
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
}
