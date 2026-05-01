package com.refinex.dbflow.admin.view;

/**
 * 授权表格行。
 *
 * @param id             授权主键
 * @param userId         用户主键
 * @param username       用户名
 * @param projectKey     项目标识
 * @param environmentKey 环境标识
 * @param grantType      授权类型
 * @param status         授权状态
 * @author refinex
 */
public record GrantRow(Long id, Long userId, String username, String projectKey, String environmentKey,
                       String grantType, String status) {
}
