package com.refinex.dbflow.executor.dto;

import com.refinex.dbflow.audit.dto.AuditRequestContext;

/**
 * SQL EXPLAIN 请求。
 *
 * @param requestId      请求标识
 * @param userId         用户主键
 * @param tokenId        Token 主键
 * @param tokenPrefix    Token 展示前缀，禁止传入明文 Token
 * @param projectKey     项目标识
 * @param environmentKey 环境标识
 * @param sql            SQL 原文
 * @param schema         默认 schema，可为空
 * @param auditContext   审计请求来源上下文
 * @author refinex
 */
public record SqlExplainRequest(
        String requestId,
        Long userId,
        Long tokenId,
        String tokenPrefix,
        String projectKey,
        String environmentKey,
        String sql,
        String schema,
        AuditRequestContext auditContext
) {

    /**
     * 创建不含显式审计来源上下文的 EXPLAIN 请求。
     *
     * @param requestId      请求标识
     * @param userId         用户主键
     * @param tokenId        Token 主键
     * @param tokenPrefix    Token 展示前缀
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @param sql            SQL 原文
     * @param schema         默认 schema
     */
    public SqlExplainRequest(
            String requestId,
            Long userId,
            Long tokenId,
            String tokenPrefix,
            String projectKey,
            String environmentKey,
            String sql,
            String schema
    ) {
        this(requestId, userId, tokenId, tokenPrefix, projectKey, environmentKey, sql, schema,
                AuditRequestContext.unknown("dbflow_explain_sql"));
    }
}
