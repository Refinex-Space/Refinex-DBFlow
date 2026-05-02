package com.refinex.dbflow.capacity.support;

import com.refinex.dbflow.capacity.model.McpToolClass;
import com.refinex.dbflow.mcp.support.DbflowMcpNames;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * MCP 暴露面容量分级解析器，将工具、资源和 prompt 名称映射到容量类别。
 *
 * @author refinex
 */
@Component
public class ToolClassResolver {

    /**
     * resource 名称前缀。
     */
    private static final String RESOURCE_PREFIX = "resource:";

    /**
     * prompt 名称前缀。
     */
    private static final String PROMPT_PREFIX = "prompt:";

    /**
     * 解析 MCP 暴露面的容量分级。
     *
     * @param surfaceName MCP 工具、资源或 prompt 名称
     * @return 容量分级
     */
    public McpToolClass resolve(String surfaceName) {
        String normalized = normalize(surfaceName);
        if (DbflowMcpNames.TOOL_EXECUTE_SQL.equals(normalized)
                || DbflowMcpNames.TOOL_CONFIRM_SQL.equals(normalized)) {
            return McpToolClass.EXECUTE;
        }
        if (DbflowMcpNames.TOOL_EXPLAIN_SQL.equals(normalized)) {
            return McpToolClass.EXPLAIN;
        }
        if (DbflowMcpNames.TOOL_INSPECT_SCHEMA.equals(normalized)
                || "resource:schema".equals(normalized)
                || normalizeResource(DbflowMcpNames.RESOURCE_SCHEMA).equals(normalized)) {
            return McpToolClass.HEAVY_READ;
        }
        if (DbflowMcpNames.TOOL_LIST_TARGETS.equals(normalized)
                || DbflowMcpNames.TOOL_GET_EFFECTIVE_POLICY.equals(normalized)
                || normalizeResource(DbflowMcpNames.RESOURCE_TARGETS).equals(normalized)
                || "resource:policy".equals(normalized)
                || normalizeResource(DbflowMcpNames.RESOURCE_POLICY).equals(normalized)
                || normalized.startsWith(PROMPT_PREFIX)) {
            return McpToolClass.LIGHT_READ;
        }
        if (normalized.startsWith(RESOURCE_PREFIX)) {
            return McpToolClass.LIGHT_READ;
        }
        return McpToolClass.EXECUTE;
    }

    /**
     * 标准化 resource 名称。
     *
     * @param uri resource URI
     * @return 标准化 resource 名称
     */
    private String normalizeResource(String uri) {
        return RESOURCE_PREFIX + normalize(uri);
    }

    /**
     * 标准化 MCP 暴露面名称。
     *
     * @param surfaceName 原始名称
     * @return 标准化名称
     */
    private String normalize(String surfaceName) {
        if (!StringUtils.hasText(surfaceName)) {
            return "";
        }
        return surfaceName.strip().toLowerCase(Locale.ROOT);
    }
}
