package com.refinex.dbflow.executor;

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
        String schema
) {
}
