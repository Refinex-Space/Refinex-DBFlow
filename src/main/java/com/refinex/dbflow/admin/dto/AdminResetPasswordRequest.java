package com.refinex.dbflow.admin.dto;

/**
 * React 管理端重置用户密码请求。
 *
 * @param newPassword 新明文密码
 * @author refinex
 */
public record AdminResetPasswordRequest(String newPassword) {
}
