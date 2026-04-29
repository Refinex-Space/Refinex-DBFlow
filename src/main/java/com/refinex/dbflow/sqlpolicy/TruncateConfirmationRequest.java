package com.refinex.dbflow.sqlpolicy;

import java.time.Instant;

/**
 * TRUNCATE 确认挑战创建请求。
 *
 * @param requestId      请求标识
 * @param userId         用户主键
 * @param tokenId        MCP Token 主键
 * @param tokenPrefix    Token 前缀
 * @param projectKey     项目标识
 * @param environmentKey 环境标识
 * @param sql            SQL 原文
 * @param requestedAt    请求时间
 * @author refinex
 */
public record TruncateConfirmationRequest(
        String requestId,
        Long userId,
        Long tokenId,
        String tokenPrefix,
        String projectKey,
        String environmentKey,
        String sql,
        Instant requestedAt
) {
}
