package com.refinex.dbflow.capacity.model;

/**
 * 容量治理请求上下文，来自已认证 MCP 调用和工具参数。
 *
 * @param requestId         请求标识，用于日志和排障关联
 * @param userId            用户主键，可为空表示尚无用户上下文
 * @param tokenId           Token 主键，可为空表示尚无 Token 上下文
 * @param toolName          MCP 工具、资源或 prompt 名称
 * @param toolClass         MCP 工具容量分级
 * @param projectKey        目标项目标识，可为空表示非目标类能力
 * @param environmentKey    目标环境标识，可为空表示非目标类能力
 * @param requestedMaxItems 客户端请求的最大条目数，主要用于重型只读降级
 * @author refinex
 */
public record CapacityRequest(
        String requestId,
        Long userId,
        Long tokenId,
        String toolName,
        McpToolClass toolClass,
        String projectKey,
        String environmentKey,
        Integer requestedMaxItems
) {

    /**
     * 创建不包含条目数上限的容量请求。
     *
     * @param requestId      请求标识
     * @param userId         用户主键
     * @param tokenId        Token 主键
     * @param toolName       MCP 工具、资源或 prompt 名称
     * @param toolClass      MCP 工具容量分级
     * @param projectKey     目标项目标识
     * @param environmentKey 目标环境标识
     */
    public CapacityRequest(
            String requestId,
            Long userId,
            Long tokenId,
            String toolName,
            McpToolClass toolClass,
            String projectKey,
            String environmentKey
    ) {
        this(requestId, userId, tokenId, toolName, toolClass, projectKey, environmentKey, null);
    }
}
