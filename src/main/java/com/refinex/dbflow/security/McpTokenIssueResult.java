package com.refinex.dbflow.security;

import java.time.Instant;

/**
 * MCP Token 颁发结果，明文 Token 只允许通过该对象一次性返回。
 *
 * @param tokenId        Token 主键
 * @param userId         用户主键
 * @param plaintextToken 明文 Token
 * @param tokenPrefix    Token 前缀
 * @param expiresAt      过期时间
 * @author refinex
 */
public record McpTokenIssueResult(
        Long tokenId,
        Long userId,
        String plaintextToken,
        String tokenPrefix,
        Instant expiresAt
) {
}
