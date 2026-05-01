package com.refinex.dbflow.admin.view;

/**
 * 授权筛选条件。
 *
 * @param username       用户名过滤
 * @param projectKey     项目标识过滤
 * @param environmentKey 环境标识过滤
 * @param status         状态过滤
 * @author refinex
 */
public record GrantFilter(String username, String projectKey, String environmentKey, String status) {
}
