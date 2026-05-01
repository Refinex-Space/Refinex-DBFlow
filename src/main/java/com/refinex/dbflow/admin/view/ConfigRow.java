package com.refinex.dbflow.admin.view;

/**
 * 配置页行。
 *
 * @param project     项目标识
 * @param projectName 项目名称
 * @param env         环境标识
 * @param envName     环境名称
 * @param datasource  数据源摘要
 * @param type        数据库类型
 * @param host        主机
 * @param port        端口
 * @param schema      数据库或 schema
 * @param username    用户名
 * @param limits      连接池限制
 * @param syncStatus  元数据库同步状态
 * @author refinex
 */
public record ConfigRow(
        String project,
        String projectName,
        String env,
        String envName,
        String datasource,
        String type,
        String host,
        String port,
        String schema,
        String username,
        String limits,
        String syncStatus
) {
}
