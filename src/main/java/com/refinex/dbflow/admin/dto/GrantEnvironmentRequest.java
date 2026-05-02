package com.refinex.dbflow.admin.dto;

/**
 * React 管理端创建环境授权请求。
 *
 * @param userId         用户主键
 * @param projectKey     项目标识
 * @param environmentKey 环境标识
 * @param grantType      授权类型
 * @author refinex
 */
public record GrantEnvironmentRequest(Long userId, String projectKey, String environmentKey, String grantType) {
}
