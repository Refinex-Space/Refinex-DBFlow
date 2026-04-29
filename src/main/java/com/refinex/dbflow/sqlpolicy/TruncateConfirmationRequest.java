package com.refinex.dbflow.sqlpolicy;

import com.refinex.dbflow.audit.service.AuditRequestContext;

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
 * @param auditContext   审计请求来源上下文
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
        Instant requestedAt,
        AuditRequestContext auditContext
) {

    /**
     * 创建不含显式审计来源上下文的确认挑战请求。
     *
     * @param requestId      请求标识
     * @param userId         用户主键
     * @param tokenId        MCP Token 主键
     * @param tokenPrefix    Token 前缀
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @param sql            SQL 原文
     * @param requestedAt    请求时间
     */
    public TruncateConfirmationRequest(
            String requestId,
            Long userId,
            Long tokenId,
            String tokenPrefix,
            String projectKey,
            String environmentKey,
            String sql,
            Instant requestedAt
    ) {
        this(requestId, userId, tokenId, tokenPrefix, projectKey, environmentKey, sql, requestedAt,
                AuditRequestContext.unknown("dbflow_execute_sql"));
    }
}
