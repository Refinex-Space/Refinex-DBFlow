package com.refinex.dbflow.admin.dto;

import com.refinex.dbflow.admin.view.TokenRow;

import java.time.Instant;

/**
 * React 管理端 Token 列表安全响应行。
 *
 * @param id          Token 主键
 * @param userId      用户主键
 * @param username    用户名
 * @param tokenPrefix Token 展示前缀
 * @param status      Token 状态
 * @param expiresAt   过期时间
 * @param lastUsedAt  最后使用时间
 * @author refinex
 */
public record AdminTokenRowResponse(Long id, Long userId, String username, String tokenPrefix, String status,
                                    Instant expiresAt, Instant lastUsedAt) {

    /**
     * 从内部 Token 行转换为安全 JSON 响应行。
     *
     * @param row 内部 Token 行
     * @return 不包含明文和 hash 的 Token 响应行
     */
    public static AdminTokenRowResponse from(TokenRow row) {
        return new AdminTokenRowResponse(
                row.id(),
                row.userId(),
                row.username(),
                row.tokenPrefix(),
                row.status(),
                row.expiresAt(),
                row.lastUsedAt());
    }
}
