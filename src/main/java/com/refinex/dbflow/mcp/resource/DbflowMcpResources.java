package com.refinex.dbflow.mcp.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.refinex.dbflow.capacity.model.CapacityDecision;
import com.refinex.dbflow.capacity.model.CapacityRequest;
import com.refinex.dbflow.capacity.model.McpToolClass;
import com.refinex.dbflow.capacity.service.CapacityGuardService;
import com.refinex.dbflow.capacity.support.CapacityPermit;
import com.refinex.dbflow.executor.dto.SchemaInspectRequest;
import com.refinex.dbflow.executor.dto.SchemaInspectResult;
import com.refinex.dbflow.executor.service.SchemaInspectService;
import com.refinex.dbflow.mcp.auth.McpAccessBoundaryService;
import com.refinex.dbflow.mcp.auth.McpAuthenticationContext;
import com.refinex.dbflow.mcp.auth.McpAuthenticationContextResolver;
import com.refinex.dbflow.mcp.auth.McpAuthorizationBoundary;
import com.refinex.dbflow.mcp.support.DbflowMcpNames;
import com.refinex.dbflow.mcp.support.McpTargetPolicyProjectionService;
import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.refinex.dbflow.mcp.support.McpErrorMetadataFactory.*;
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
     * 容量治理服务。
     */
    private final CapacityGuardService capacityGuardService;

    /**
     * MCP 目标与策略投影服务。
     */
    private final McpTargetPolicyProjectionService targetPolicyProjectionService;

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
     * @param capacityGuardService          容量治理服务
     * @param targetPolicyProjectionService MCP 目标与策略投影服务
     * @param schemaInspectService          schema inspect 服务
     */
    public DbflowMcpResources(
            ObjectMapper objectMapper,
            McpAuthenticationContextResolver authenticationContextResolver,
            McpAccessBoundaryService accessBoundaryService,
            CapacityGuardService capacityGuardService,
            McpTargetPolicyProjectionService targetPolicyProjectionService,
            SchemaInspectService schemaInspectService
    ) {
        this.objectMapper = objectMapper;
        this.authenticationContextResolver = authenticationContextResolver;
        this.accessBoundaryService = accessBoundaryService;
        this.capacityGuardService = capacityGuardService;
        this.targetPolicyProjectionService = targetPolicyProjectionService;
        this.schemaInspectService = schemaInspectService;
    }

    /**
     * 返回目标项目环境列表 resource。
     *
     * @param request resource 读取请求
     * @return resource 读取结果
     */
    @McpResource(
            uri = DbflowMcpNames.RESOURCE_TARGETS,
            name = "dbflow_targets",
            title = "DBFlow targets",
            description = "Project/environment targets visible to the current MCP principal after authentication and grant checks.",
            mimeType = "application/json"
    )
    public McpSchema.ReadResourceResult targets(McpSchema.ReadResourceRequest request) {
        McpAuthenticationContext context = authenticationContextResolver.currentContext();
        McpAuthorizationBoundary boundary = accessBoundaryService.metadataBoundary(context, "resource:" + request.uri());
        if (!boundary.allowed()) {
            return jsonResource(objectMapper, request.uri(), boundaryDeniedPayload(request.uri(), context, boundary));
        }
        CapacityDecision capacityDecision = evaluateCapacity(
                context,
                "resource:" + request.uri(),
                McpToolClass.LIGHT_READ,
                null,
                null,
                null
        );
        if (!capacityDecision.allowed()) {
            return jsonResource(objectMapper, request.uri(),
                    capacityRejectedPayload(request.uri(), context, boundary, capacityDecision));
        }
        try (CapacityPermit ignored = capacityDecision.permit()) {
            return jsonResource(objectMapper, request.uri(), data(
                    "status", "AUTHORIZED",
                    "uri", request.uri(),
                    "authentication", context,
                    "authorization", boundary,
                    "targets", targetPolicyProjectionService.visibleTargets(
                            context,
                            accessBoundaryService,
                            "resource:" + request.uri()
                    ),
                    "capacity", capacityData(capacityDecision),
                    "notices", notices(false, capacityDecision)
            ));
        }
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
        if (!boundary.allowed()) {
            return jsonResource(objectMapper, request.uri(), boundaryDeniedPayload(request.uri(), context, boundary,
                    data("project", project, "env", env)));
        }
        CapacityDecision capacityDecision = evaluateCapacity(
                context,
                "resource:schema",
                McpToolClass.HEAVY_READ,
                project,
                env,
                null
        );
        if (!capacityDecision.allowed()) {
            return jsonResource(objectMapper, request.uri(), capacityRejectedPayload(
                    request.uri(),
                    context,
                    boundary,
                    capacityDecision,
                    data("project", project, "env", env)
            ));
        }
        try (CapacityPermit ignored = capacityDecision.permit()) {
            SchemaInspectResult result = schemaInspectService.inspect(new SchemaInspectRequest(
                    context.requestId(),
                    context.userId(),
                    context.tokenId(),
                    null,
                    project,
                    env,
                    null,
                    null,
                    capacityDecision.maxItemsOverride() == null ? 0 : capacityDecision.maxItemsOverride()
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
                    "errorMessage", result.errorMessage(),
                    "capacity", capacityData(capacityDecision),
                    "notices", notices(result.truncated(), capacityDecision)
            ));
        }
    }

    /**
     * 返回项目环境 policy resource。
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
            description = "Effective SQL policy view for a DBFlow project environment from current configuration.",
            mimeType = "application/json"
    )
    public McpSchema.ReadResourceResult policy(McpSchema.ReadResourceRequest request, String project, String env) {
        McpAuthenticationContext context = authenticationContextResolver.currentContext();
        McpAuthorizationBoundary boundary = accessBoundaryService.targetBoundary(context, project, env,
                "resource:policy");
        if (!boundary.allowed()) {
            Map<String, Object> policy = targetPolicyProjectionService.effectivePolicy(
                    project,
                    env,
                    null,
                    null,
                    null,
                    boundary
            );
            policy.put("uri", request.uri());
            policy.put("authentication", context);
            policy.put("authorization", boundary);
            policy.put("error", errorData("POLICY_DENIED", boundary.message()));
            return jsonResource(objectMapper, request.uri(), policy);
        }
        CapacityDecision capacityDecision = evaluateCapacity(
                context,
                "resource:policy",
                McpToolClass.LIGHT_READ,
                project,
                env,
                null
        );
        if (!capacityDecision.allowed()) {
            return jsonResource(objectMapper, request.uri(), capacityRejectedPayload(
                    request.uri(),
                    context,
                    boundary,
                    capacityDecision,
                    data("project", project, "env", env)
            ));
        }
        try (CapacityPermit ignored = capacityDecision.permit()) {
            Map<String, Object> policy = targetPolicyProjectionService.effectivePolicy(
                    project,
                    env,
                    null,
                    null,
                    null,
                    boundary
            );
            policy.put("uri", request.uri());
            policy.put("authentication", context);
            policy.put("authorization", boundary);
            policy.put("capacity", capacityData(capacityDecision));
            policy.put("notices", notices(false, capacityDecision));
            return jsonResource(objectMapper, request.uri(), policy);
        }
    }

    /**
     * 执行容量治理评估。
     *
     * @param context       MCP 认证上下文
     * @param resource      MCP resource 名称
     * @param toolClass     MCP 暴露面容量分级
     * @param project       项目标识
     * @param env           环境标识
     * @param requestedSize 客户端请求条目数
     * @return 容量治理决策
     */
    private CapacityDecision evaluateCapacity(
            McpAuthenticationContext context,
            String resource,
            McpToolClass toolClass,
            String project,
            String env,
            Integer requestedSize
    ) {
        return capacityGuardService.evaluate(new CapacityRequest(
                context.requestId(),
                context.userId(),
                context.tokenId(),
                resource,
                toolClass,
                project,
                env,
                requestedSize
        ));
    }

    /**
     * 创建授权拒绝 resource 数据。
     *
     * @param uri      resource URI
     * @param context  MCP 认证上下文
     * @param boundary MCP 授权边界结果
     * @return resource 数据
     */
    private Map<String, Object> boundaryDeniedPayload(
            String uri,
            McpAuthenticationContext context,
            McpAuthorizationBoundary boundary
    ) {
        return boundaryDeniedPayload(uri, context, boundary, data());
    }

    /**
     * 创建带基础字段的授权拒绝 resource 数据。
     *
     * @param uri      resource URI
     * @param context  MCP 认证上下文
     * @param boundary MCP 授权边界结果
     * @param values   基础响应字段
     * @return resource 数据
     */
    private Map<String, Object> boundaryDeniedPayload(
            String uri,
            McpAuthenticationContext context,
            McpAuthorizationBoundary boundary,
            Map<String, Object> values
    ) {
        values.put("status", "DENIED");
        values.put("uri", uri);
        values.put("authentication", context);
        values.put("authorization", boundary);
        values.put("error", errorData(boundary.reason(), boundary.message()));
        return values;
    }

    /**
     * 创建容量拒绝 resource 数据。
     *
     * @param uri              resource URI
     * @param context          MCP 认证上下文
     * @param boundary         MCP 授权边界结果
     * @param capacityDecision 容量治理决策
     * @return resource 数据
     */
    private Map<String, Object> capacityRejectedPayload(
            String uri,
            McpAuthenticationContext context,
            McpAuthorizationBoundary boundary,
            CapacityDecision capacityDecision
    ) {
        return capacityRejectedPayload(uri, context, boundary, capacityDecision, data());
    }

    /**
     * 创建带基础字段的容量拒绝 resource 数据。
     *
     * @param uri              resource URI
     * @param context          MCP 认证上下文
     * @param boundary         MCP 授权边界结果
     * @param capacityDecision 容量治理决策
     * @param values           基础响应字段
     * @return resource 数据
     */
    private Map<String, Object> capacityRejectedPayload(
            String uri,
            McpAuthenticationContext context,
            McpAuthorizationBoundary boundary,
            CapacityDecision capacityDecision,
            Map<String, Object> values
    ) {
        values.put("allowed", false);
        values.put("status", "CAPACITY_REJECTED");
        values.put("uri", uri);
        values.put("authentication", context);
        values.put("authorization", boundary);
        values.put("error", capacityError(capacityDecision));
        values.put("capacity", capacityData(capacityDecision));
        values.put("notices", notices(false, capacityDecision));
        return values;
    }

}
