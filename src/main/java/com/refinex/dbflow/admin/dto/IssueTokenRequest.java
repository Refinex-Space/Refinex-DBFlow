package com.refinex.dbflow.admin.dto;

/**
 * React 管理端颁发 Token 请求。
 *
 * @param userId        用户主键
 * @param expiresInDays 有效天数
 * @author refinex
 */
public record IssueTokenRequest(Long userId, Integer expiresInDays) {
}
