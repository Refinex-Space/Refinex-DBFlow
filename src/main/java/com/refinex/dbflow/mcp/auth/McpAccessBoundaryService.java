package com.refinex.dbflow.mcp.auth;

/**
 * MCP 访问授权边界服务。
 *
 * @author refinex
 */
public interface McpAccessBoundaryService {

    /**
     * 记录只读元数据访问边界。
     *
     * @param context   MCP 请求认证上下文
     * @param operation MCP 操作名称
     * @return 授权边界结果
     */
    McpAuthorizationBoundary metadataBoundary(McpAuthenticationContext context, String operation);

    /**
     * 检查目标项目环境访问边界。
     *
     * @param context     MCP 请求认证上下文
     * @param project     项目标识
     * @param environment 环境标识
     * @param operation   MCP 操作名称
     * @return 授权边界结果
     */
    McpAuthorizationBoundary targetBoundary(
            McpAuthenticationContext context,
            String project,
            String environment,
            String operation
    );
}
