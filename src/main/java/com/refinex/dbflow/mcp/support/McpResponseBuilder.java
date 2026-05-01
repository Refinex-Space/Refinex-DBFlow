package com.refinex.dbflow.mcp.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refinex.dbflow.mcp.auth.McpAuthenticationContext;
import com.refinex.dbflow.mcp.auth.McpAuthorizationBoundary;
import com.refinex.dbflow.mcp.dto.DbflowMcpSkeletonResponse;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 响应构造工具，统一 skeleton、resource 和有序 Map 创建逻辑。
 *
 * @author refinex
 */
public final class McpResponseBuilder {

    /**
     * 工具类不允许实例化。
     */
    private McpResponseBuilder() {
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
    public static DbflowMcpSkeletonResponse skeleton(
            String surface,
            McpAuthenticationContext context,
            McpAuthorizationBoundary boundary,
            Map<String, Object> data
    ) {
        return DbflowMcpSkeletonResponse.of(surface, context, boundary, data);
    }

    /**
     * 创建 JSON resource 读取结果。
     *
     * @param objectMapper JSON 序列化器
     * @param uri          resource URI
     * @param payload      resource 数据
     * @return resource 读取结果
     */
    public static McpSchema.ReadResourceResult jsonResource(ObjectMapper objectMapper, String uri, Object payload) {
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
    public static Map<String, Object> data(Object... entries) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            values.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return values;
    }
}
