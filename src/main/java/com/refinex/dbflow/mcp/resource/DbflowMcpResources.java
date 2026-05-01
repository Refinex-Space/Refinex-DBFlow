package com.refinex.dbflow.mcp.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.refinex.dbflow.executor.dto.SchemaInspectRequest;
import com.refinex.dbflow.executor.dto.SchemaInspectResult;
import com.refinex.dbflow.executor.service.SchemaInspectService;
import com.refinex.dbflow.mcp.auth.McpAccessBoundaryService;
import com.refinex.dbflow.mcp.auth.McpAuthenticationContext;
import com.refinex.dbflow.mcp.auth.McpAuthenticationContextResolver;
import com.refinex.dbflow.mcp.auth.McpAuthorizationBoundary;
import com.refinex.dbflow.mcp.support.DbflowMcpNames;
import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.refinex.dbflow.mcp.support.McpResponseBuilder.data;
import static com.refinex.dbflow.mcp.support.McpResponseBuilder.jsonResource;
import static com.refinex.dbflow.mcp.support.McpSchemaMetadataMapper.*;

/**
 * DBFlow MCP resource skeleton。
 *
 * @author refinex
 */
@Component
public class DbflowMcpResources {

    /**
     * JSON 序列化器。
     */
    private final ObjectMapper objectMapper;

    /**
     * MCP 认证上下文解析器。
     */
    private final McpAuthenticationContextResolver authenticationContextResolver;

    /**
     * MCP 访问授权边界服务。
     */
    private final McpAccessBoundaryService accessBoundaryService;

    /**
     * schema inspect 服务。
     */
    private final SchemaInspectService schemaInspectService;

    /**
     * 创建 DBFlow MCP resource skeleton。
     *
     * @param objectMapper                  JSON 序列化器
     * @param authenticationContextResolver MCP 认证上下文解析器
     * @param accessBoundaryService         MCP 访问授权边界服务
     * @param schemaInspectService          schema inspect 服务
     */
    public DbflowMcpResources(
            ObjectMapper objectMapper,
            McpAuthenticationContextResolver authenticationContextResolver,
            McpAccessBoundaryService accessBoundaryService,
            SchemaInspectService schemaInspectService
    ) {
        this.objectMapper = objectMapper;
        this.authenticationContextResolver = authenticationContextResolver;
        this.accessBoundaryService = accessBoundaryService;
        this.schemaInspectService = schemaInspectService;
    }

    /**
     * 返回目标项目环境列表 resource skeleton。
     *
     * @param request resource 读取请求
     * @return resource 读取结果
     */
    @McpResource(
            uri = DbflowMcpNames.RESOURCE_TARGETS,
            name = "dbflow_targets",
            title = "DBFlow targets",
            description = "Project/environment targets visible to the current MCP principal. Skeleton returns an empty list.",
            mimeType = "application/json"
    )
    public McpSchema.ReadResourceResult targets(McpSchema.ReadResourceRequest request) {
        McpAuthenticationContext context = authenticationContextResolver.currentContext();
        McpAuthorizationBoundary boundary = accessBoundaryService.metadataBoundary(context, "resource:" + request.uri());
        return jsonResource(objectMapper, request.uri(), Map.of(
                "status", "SKELETON",
                "uri", request.uri(),
                "authentication", context,
                "authorization", boundary,
                "targets", List.of()
        ));
    }

    /**
     * 返回项目环境 schema resource skeleton。
     *
     * @param request resource 读取请求
     * @param project 项目标识
     * @param env     环境标识
     * @return resource 读取结果
     */
    @McpResource(
            uri = DbflowMcpNames.RESOURCE_SCHEMA,
            name = "dbflow_schema",
            title = "DBFlow schema",
            description = "Bounded information_schema metadata for a DBFlow project environment.",
            mimeType = "application/json"
    )
    public McpSchema.ReadResourceResult schema(McpSchema.ReadResourceRequest request, String project, String env) {
        McpAuthenticationContext context = authenticationContextResolver.currentContext();
        McpAuthorizationBoundary boundary = accessBoundaryService.targetBoundary(context, project, env,
                "resource:schema");
        SchemaInspectResult result = schemaInspectService.inspect(new SchemaInspectRequest(
                context.requestId(),
                context.userId(),
                context.tokenId(),
                null,
                project,
                env,
                null,
                null,
                0
        ));
        return jsonResource(objectMapper, request.uri(), data(
                "status", result.status(),
                "uri", request.uri(),
                "project", result.projectKey(),
                "env", result.environmentKey(),
                "schema", result.schemaFilter(),
                "table", result.tableFilter(),
                "allowed", result.allowed(),
                "maxItems", result.maxItems(),
                "truncated", result.truncated(),
                "authentication", context,
                "authorization", boundary,
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
     * 返回项目环境 policy resource skeleton。
     *
     * @param request resource 读取请求
     * @param project 项目标识
     * @param env     环境标识
     * @return resource 读取结果
     */
    @McpResource(
            uri = DbflowMcpNames.RESOURCE_POLICY,
            name = "dbflow_policy",
            title = "DBFlow policy",
            description = "Effective SQL policy view for a DBFlow project environment. Skeleton returns safe defaults.",
            mimeType = "application/json"
    )
    public McpSchema.ReadResourceResult policy(McpSchema.ReadResourceRequest request, String project, String env) {
        McpAuthenticationContext context = authenticationContextResolver.currentContext();
        McpAuthorizationBoundary boundary = accessBoundaryService.targetBoundary(context, project, env,
                "resource:policy");
        return jsonResource(objectMapper, request.uri(), data(
                "status", "SKELETON",
                "uri", request.uri(),
                "project", project,
                "env", env,
                "authentication", context,
                "authorization", boundary,
                "defaults", Map.of("DROP_TABLE", "DENY", "DROP_DATABASE", "DENY", "TRUNCATE", "REQUIRE_CONFIRMATION"),
                "whitelist", List.of()
        ));
    }

}
