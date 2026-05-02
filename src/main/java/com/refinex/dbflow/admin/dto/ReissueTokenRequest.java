package com.refinex.dbflow.admin.dto;

/**
 * React 管理端重新颁发 Token 请求。
 *
 * @param expiresInDays 有效天数
 * @author refinex
 */
public record ReissueTokenRequest(Integer expiresInDays) {
}
