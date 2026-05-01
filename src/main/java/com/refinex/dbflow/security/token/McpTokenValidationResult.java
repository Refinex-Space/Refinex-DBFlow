package com.refinex.dbflow.security.token;

import java.time.Instant;

/**
 * MCP Token 校验成功结果，不包含明文 Token。
 *
 * @param tokenId     Token 主键
 * @param userId      用户主键
 * @param tokenPrefix Token 前缀
 * @param lastUsedAt  最近使用时间
 * @author refinex
 */
public record McpTokenValidationResult(
        Long tokenId,
        Long userId,
        String tokenPrefix,
        Instant lastUsedAt
) {
}
