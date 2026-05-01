package com.refinex.dbflow.admin.command;

/**
 * Token 颁发命令。
 *
 * @param userId        用户主键
 * @param expiresInDays 有效天数
 * @author refinex
 */
public record IssueTokenCommand(Long userId, Integer expiresInDays) {
}
