package com.refinex.dbflow.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
     * 创建 DBFlow MCP resource skeleton。
     *
     * @param objectMapper                  JSON 序列化器
     * @param authenticationContextResolver MCP 认证上下文解析器
     * @param accessBoundaryService         MCP 访问授权边界服务
     */
    public DbflowMcpResources(
            ObjectMapper objectMapper,
            McpAuthenticationContextResolver authenticationContextResolver,
            McpAccessBoundaryService accessBoundaryService
    ) {
        this.objectMapper = objectMapper;
        this.authenticationContextResolver = authenticationContextResolver;
        this.accessBoundaryService = accessBoundaryService;
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
        return jsonResource(request.uri(), Map.of(
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
            description = "Schema metadata for a DBFlow project environment. Skeleton returns empty tables and columns.",
            mimeType = "application/json"
    )
    public McpSchema.ReadResourceResult schema(McpSchema.ReadResourceRequest request, String project, String env) {
        McpAuthenticationContext context = authenticationContextResolver.currentContext();
        McpAuthorizationBoundary boundary = accessBoundaryService.targetBoundary(context, project, env,
                "resource:schema");
        return jsonResource(request.uri(), data(
                "status", "SKELETON",
                "uri", request.uri(),
                "project", project,
                "env", env,
                "authentication", context,
                "authorization", boundary,
                "schemas", List.of()
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
        return jsonResource(request.uri(), data(
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

    /**
     * 创建 JSON resource 读取结果。
     *
     * @param uri     resource URI
     * @param payload resource 数据
     * @return resource 读取结果
     */
    private McpSchema.ReadResourceResult jsonResource(String uri, Object payload) {
        try {
            return new McpSchema.ReadResourceResult(List.of(
                    new McpSchema.TextResourceContents(uri, "application/json", objectMapper.writeValueAsString(payload))
            ));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化 DBFlow MCP resource 失败", ex);
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
}
