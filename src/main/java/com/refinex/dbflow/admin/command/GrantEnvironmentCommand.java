package com.refinex.dbflow.admin.command;

/**
 * 环境授权命令。
 *
 * @param userId         用户主键
 * @param projectKey     项目标识
 * @param environmentKey 环境标识
 * @param grantType      授权类型
 * @author refinex
 */
public record GrantEnvironmentCommand(Long userId, String projectKey, String environmentKey, String grantType) {
}
