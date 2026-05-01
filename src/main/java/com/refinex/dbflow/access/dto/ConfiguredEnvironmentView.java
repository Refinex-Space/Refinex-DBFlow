package com.refinex.dbflow.access.dto;

/**
 * 管理端项目环境展示视图，不包含数据库密码。
 *
 * @param projectId       元数据库项目主键
 * @param environmentId   元数据库环境主键
 * @param projectKey      项目标识
 * @param projectName     项目名称
 * @param environmentKey  环境标识
 * @param environmentName 环境名称
 * @param jdbcUrl         JDBC URL
 * @param driverClassName JDBC 驱动类名
 * @param username        数据库用户名
 * @param metadataPresent 元数据库展示模型是否已存在
 * @author refinex
 */
public record ConfiguredEnvironmentView(
        Long projectId,
        Long environmentId,
        String projectKey,
        String projectName,
        String environmentKey,
        String environmentName,
        String jdbcUrl,
        String driverClassName,
        String username,
        boolean metadataPresent
) {
}
