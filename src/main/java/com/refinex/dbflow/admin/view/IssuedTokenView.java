package com.refinex.dbflow.admin.view;

import java.time.Instant;

/**
 * Token 一次性展示视图。
 *
 * @param tokenId        Token 主键
 * @param userId         用户主键
 * @param username       用户名
 * @param plaintextToken 仅一次展示的 Token 明文
 * @param tokenPrefix    Token 展示前缀
 * @param expiresAt      过期时间
 * @author refinex
 */
public record IssuedTokenView(Long tokenId, Long userId, String username, String plaintextToken,
                              String tokenPrefix, Instant expiresAt) {
}
