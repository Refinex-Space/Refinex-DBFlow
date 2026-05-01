package com.refinex.dbflow.admin.view;

import java.time.Instant;

/**
 * Token 表格行；`tokenHash` 永远为空，用于测试防泄漏边界。
 *
 * @param id          Token 主键
 * @param userId      用户主键
 * @param username    用户名
 * @param tokenPrefix Token 展示前缀
 * @param status      Token 状态
 * @param expiresAt   过期时间
 * @param lastUsedAt  最后使用时间
 * @param tokenHash   Token hash，管理端列表中必须为空
 * @author refinex
 */
public record TokenRow(Long id, Long userId, String username, String tokenPrefix, String status, Instant expiresAt,
                       Instant lastUsedAt, String tokenHash) {
}
