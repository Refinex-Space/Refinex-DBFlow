package com.refinex.dbflow.audit.service;

import java.time.Instant;

/**
 * 管理端审计事件详情。
 *
 * @param id             审计事件主键
 * @param requestId      请求标识
 * @param userId         用户主键
 * @param projectKey     项目标识
 * @param environmentKey 环境标识
 * @param clientName     客户端名称
 * @param clientVersion  客户端版本
 * @param userAgent      HTTP User-Agent
 * @param sourceIp       来源 IP
 * @param tool           工具名称
 * @param operationType  操作类型
 * @param riskLevel      风险级别
 * @param status         审计状态
 * @param decision       审计决策
 * @param sqlHash        SQL hash
 * @param sqlText        脱敏后的 SQL 原文
 * @param resultSummary  脱敏后的有界结果摘要
 * @param affectedRows   影响行数
 * @param errorCode      错误码
 * @param errorMessage   脱敏后的错误摘要
 * @param confirmationId 确认挑战标识
 * @param createdAt      创建时间
 * @author refinex
 */
public record AuditEventDetail(
        Long id,
        String requestId,
        Long userId,
        String projectKey,
        String environmentKey,
        String clientName,
        String clientVersion,
        String userAgent,
        String sourceIp,
        String tool,
        String operationType,
        String riskLevel,
        String status,
        String decision,
        String sqlHash,
        String sqlText,
        String resultSummary,
        Long affectedRows,
        String errorCode,
        String errorMessage,
        String confirmationId,
        Instant createdAt
) {
}
