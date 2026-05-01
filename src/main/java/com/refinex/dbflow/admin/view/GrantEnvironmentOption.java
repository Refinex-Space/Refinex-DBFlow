package com.refinex.dbflow.admin.view;

/**
 * 授权表单环境下拉选项。
 *
 * @param projectKey      项目标识
 * @param projectName     项目名称
 * @param environmentKey  环境标识
 * @param environmentName 环境名称
 * @author refinex
 */
public record GrantEnvironmentOption(String projectKey, String projectName, String environmentKey,
                                     String environmentName) {
}
