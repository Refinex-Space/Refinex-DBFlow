package com.refinex.dbflow.audit.service;

/**
 * 审计事件写入请求。
 *
 * @param requestId      请求标识
 * @param userId         用户主键
 * @param tokenId        Token 元数据主键，禁止传入 Token 明文
 * @param tokenPrefix    Token 展示前缀，禁止传入 Token 明文
 * @param context        请求来源上下文
 * @param projectKey     项目标识
 * @param environmentKey 环境标识
 * @param operation      操作类型
 * @param riskLevel      风险级别
 * @param sqlText        SQL 原文
 * @param sqlHash        SQL hash
 * @param resultSummary  结果摘要，写入器会做长度上限控制
 * @param affectedRows   影响行数
 * @param errorCode      错误码
 * @param errorMessage   错误摘要
 * @param confirmationId 确认挑战标识
 * @author refinex
 */
public record AuditEventWriteRequest(
        String requestId,
        Long userId,
        Long tokenId,
        String tokenPrefix,
        AuditRequestContext context,
        String projectKey,
        String environmentKey,
        String operation,
        String riskLevel,
        String sqlText,
        String sqlHash,
        String resultSummary,
        Long affectedRows,
        String errorCode,
        String errorMessage,
        String confirmationId
) {

    /**
     * 读取非空请求来源上下文。
     *
     * @return 请求来源上下文
     */
    public AuditRequestContext effectiveContext() {
        return context == null ? AuditRequestContext.unknown("unknown") : context;
    }
}
