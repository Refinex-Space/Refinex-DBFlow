package com.refinex.dbflow.sqlpolicy;

import com.refinex.dbflow.audit.service.AuditRequestContext;

import java.time.Instant;

/**
 * TRUNCATE 确认挑战消费请求。
 *
 * @param requestId      请求标识
 * @param userId         用户主键
 * @param tokenId        MCP Token 主键
 * @param tokenPrefix    Token 前缀
 * @param projectKey     项目标识
 * @param environmentKey 环境标识
 * @param confirmationId 确认挑战标识
 * @param sql            SQL 原文
 * @param confirmedAt    确认时间
 * @param auditContext   审计请求来源上下文
 * @author refinex
 */
public record TruncateConfirmationConfirmRequest(
        String requestId,
        Long userId,
        Long tokenId,
        String tokenPrefix,
        String projectKey,
        String environmentKey,
        String confirmationId,
        String sql,
        Instant confirmedAt,
        AuditRequestContext auditContext
) {

    /**
     * 创建不含显式审计来源上下文的确认消费请求。
     *
     * @param requestId      请求标识
     * @param userId         用户主键
     * @param tokenId        MCP Token 主键
     * @param tokenPrefix    Token 前缀
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @param confirmationId 确认挑战标识
     * @param sql            SQL 原文
     * @param confirmedAt    确认时间
     */
    public TruncateConfirmationConfirmRequest(
            String requestId,
            Long userId,
            Long tokenId,
            String tokenPrefix,
            String projectKey,
            String environmentKey,
            String confirmationId,
            String sql,
            Instant confirmedAt
    ) {
        this(requestId, userId, tokenId, tokenPrefix, projectKey, environmentKey, confirmationId, sql, confirmedAt,
                AuditRequestContext.unknown("dbflow_confirm_sql"));
    }
}
