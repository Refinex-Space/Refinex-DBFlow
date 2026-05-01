package com.refinex.dbflow.executor.dto;

import com.refinex.dbflow.audit.dto.AuditRequestContext;

/**
 * 受控 SQL 执行请求。
 *
 * @param requestId      请求标识
 * @param userId         用户主键
 * @param tokenId        Token 主键
 * @param tokenPrefix    Token 展示前缀，禁止传入明文 Token
 * @param projectKey     项目标识
 * @param environmentKey 环境标识
 * @param sql            SQL 原文
 * @param schema         默认 schema，可为空
 * @param dryRun         是否仅验证不执行
 * @param reason         操作原因
 * @param options        执行限制选项
 * @param auditContext   审计请求来源上下文
 * @author refinex
 */
public record SqlExecutionRequest(
        String requestId,
        Long userId,
        Long tokenId,
        String tokenPrefix,
        String projectKey,
        String environmentKey,
        String sql,
        String schema,
        boolean dryRun,
        String reason,
        SqlExecutionOptions options,
        AuditRequestContext auditContext
) {

    /**
     * 创建不含显式审计来源上下文的执行请求。
     *
     * @param requestId      请求标识
     * @param userId         用户主键
     * @param tokenId        Token 主键
     * @param tokenPrefix    Token 展示前缀
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @param sql            SQL 原文
     * @param schema         默认 schema
     * @param dryRun         是否仅验证不执行
     * @param reason         操作原因
     * @param options        执行限制选项
     */
    public SqlExecutionRequest(
            String requestId,
            Long userId,
            Long tokenId,
            String tokenPrefix,
            String projectKey,
            String environmentKey,
            String sql,
            String schema,
            boolean dryRun,
            String reason,
            SqlExecutionOptions options
    ) {
        this(requestId, userId, tokenId, tokenPrefix, projectKey, environmentKey, sql, schema, dryRun, reason, options,
                AuditRequestContext.unknown("dbflow_execute_sql"));
    }

    /**
     * 读取非空执行限制选项。
     *
     * @return 执行限制选项
     */
    public SqlExecutionOptions effectiveOptions() {
        return options == null ? SqlExecutionOptions.defaults() : options;
    }
}
