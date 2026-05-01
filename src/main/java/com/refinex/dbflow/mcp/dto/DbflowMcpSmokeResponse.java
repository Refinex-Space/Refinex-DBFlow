package com.refinex.dbflow.mcp.dto;

/**
 * MCP Server 启动探测响应。
 *
 * @param status        服务状态
 * @param serverName    MCP Server 名称
 * @param serverVersion MCP Server 版本
 * @param observedAt    探测时间
 * @author refinex
 */
public record DbflowMcpSmokeResponse(String status, String serverName, String serverVersion, String observedAt) {
}
