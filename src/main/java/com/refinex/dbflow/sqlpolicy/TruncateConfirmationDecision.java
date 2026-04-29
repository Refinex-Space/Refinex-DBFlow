package com.refinex.dbflow.sqlpolicy;

import java.time.Instant;

/**
 * TRUNCATE 确认挑战决策结果。
 *
 * @param confirmationRequired 是否需要确认
 * @param confirmed            是否已确认
 * @param status               挑战状态
 * @param confirmationId       确认挑战标识
 * @param sqlHash              SQL hash
 * @param riskLevel            风险级别
 * @param expiresAt            过期时间
 * @author refinex
 */
public record TruncateConfirmationDecision(
        boolean confirmationRequired,
        boolean confirmed,
        String status,
        String confirmationId,
        String sqlHash,
        SqlRiskLevel riskLevel,
        Instant expiresAt
) {
}
