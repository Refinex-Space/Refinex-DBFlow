package com.refinex.dbflow.executor.datasource;

import javax.sql.DataSource;

/**
 * 项目环境级目标库数据源注册表接口。
 *
 * @author refinex
 */
public interface ProjectEnvironmentDataSourceRegistry {

    /**
     * 按项目和环境标识获取目标库数据源。
     *
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @return 目标库数据源
     */
    DataSource getDataSource(String projectKey, String environmentKey);
}
