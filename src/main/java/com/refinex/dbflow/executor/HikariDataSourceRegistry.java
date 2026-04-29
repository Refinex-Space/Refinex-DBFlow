package com.refinex.dbflow.executor;

import com.refinex.dbflow.common.DbflowException;
import com.refinex.dbflow.common.ErrorCode;
import com.refinex.dbflow.config.DbflowProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 基于 DBFlow 项目环境配置创建并管理 Hikari 目标库数据源。
 *
 * @author refinex
 */
@Service
public class HikariDataSourceRegistry implements ProjectEnvironmentDataSourceRegistry, DisposableBean {

    /**
     * 已注册的目标库数据源，key 为 projectKey/environmentKey。
     */
    private final Map<DataSourceKey, HikariDataSource> dataSources;

    /**
     * 创建 Hikari 目标库数据源注册表。
     *
     * @param properties DBFlow 配置属性
     */
    public HikariDataSourceRegistry(DbflowProperties properties) {
        this.dataSources = Collections.unmodifiableMap(createDataSources(Objects.requireNonNull(properties)));
    }

    /**
     * 按项目和环境标识获取目标库数据源。
     *
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @return 目标库数据源
     */
    @Override
    public DataSource getDataSource(String projectKey, String environmentKey) {
        DataSourceKey key = new DataSourceKey(projectKey, environmentKey);
        HikariDataSource dataSource = dataSources.get(key);
        if (dataSource == null) {
            throw new DbflowException(ErrorCode.INVALID_REQUEST,
                    "未配置目标数据源: " + key.toDisplayName());
        }
        return dataSource;
    }

    /**
     * 关闭所有由注册表持有的 Hikari 数据源。
     */
    @Override
    public void destroy() {
        for (HikariDataSource dataSource : dataSources.values()) {
            closeQuietly(dataSource);
        }
    }

    /**
     * 根据配置创建全部目标库数据源。
     *
     * @param properties DBFlow 配置属性
     * @return 目标库数据源映射
     */
    private Map<DataSourceKey, HikariDataSource> createDataSources(DbflowProperties properties) {
        Map<DataSourceKey, HikariDataSource> targetDataSources = new LinkedHashMap<>();
        DbflowProperties.DatasourceDefaults defaults = properties.getDatasourceDefaults();
        try {
            for (DbflowProperties.Project project : properties.getProjects()) {
                for (DbflowProperties.Environment environment : project.getEnvironments()) {
                    DataSourceKey key = new DataSourceKey(project.getKey(), environment.getKey());
                    HikariDataSource dataSource = createDataSource(defaults, project, environment);
                    HikariDataSource previous = targetDataSources.put(key, dataSource);
                    if (previous != null) {
                        closeQuietly(previous);
                        closeQuietly(dataSource);
                        throw new DbflowException(ErrorCode.INTERNAL_ERROR,
                                "目标数据源重复注册: " + key.toDisplayName());
                    }
                }
            }
        } catch (RuntimeException exception) {
            for (HikariDataSource dataSource : targetDataSources.values()) {
                closeQuietly(dataSource);
            }
            throw exception;
        }
        return targetDataSources;
    }

    /**
     * 创建单个项目环境目标库数据源。
     *
     * @param defaults    数据源默认配置
     * @param project     项目配置
     * @param environment 环境配置
     * @return Hikari 数据源
     */
    private HikariDataSource createDataSource(
            DbflowProperties.DatasourceDefaults defaults,
            DbflowProperties.Project project,
            DbflowProperties.Environment environment) {
        HikariConfig config = buildConfig(defaults, project, environment);
        HikariDataSource dataSource = null;
        try {
            dataSource = new HikariDataSource(config);
            if (defaults.isValidateOnStartup()) {
                validateConnection(dataSource);
            }
            return dataSource;
        } catch (RuntimeException | SQLException exception) {
            closeQuietly(dataSource);
            throw new DbflowException(ErrorCode.INTERNAL_ERROR, sanitizedFailureMessage(defaults, project, environment));
        }
    }

    /**
     * 构建单个 Hikari 数据源配置。
     *
     * @param defaults    数据源默认配置
     * @param project     项目配置
     * @param environment 环境配置
     * @return Hikari 配置
     */
    private HikariConfig buildConfig(
            DbflowProperties.DatasourceDefaults defaults,
            DbflowProperties.Project project,
            DbflowProperties.Environment environment) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(environment.getJdbcUrl());
        config.setDriverClassName(resolveText(environment.getDriverClassName(), defaults.getDriverClassName()));
        setIfHasText(config::setUsername, resolveText(environment.getUsername(), defaults.getUsername()));
        setIfPresent(config::setPassword, resolveSecret(environment.getPassword(), defaults.getPassword()));
        config.setPoolName(poolName(defaults.getHikari(), project.getKey(), environment.getKey()));
        applySharedHikari(config, defaults.getHikari());
        config.setInitializationFailTimeout(defaults.isValidateOnStartup() ? 1L : -1L);
        return config;
    }

    /**
     * 应用共享 Hikari 配置。
     *
     * @param config Hikari 配置
     * @param hikari 共享 Hikari 配置
     */
    private void applySharedHikari(HikariConfig config, DbflowProperties.Hikari hikari) {
        setIfPresent(config::setMaximumPoolSize, hikari.getMaximumPoolSize());
        setIfPresent(config::setMinimumIdle, hikari.getMinimumIdle());
        setIfPresent(config::setConnectionTimeout, toMillis(hikari.getConnectionTimeout()));
        setIfPresent(config::setIdleTimeout, toMillis(hikari.getIdleTimeout()));
        setIfPresent(config::setMaxLifetime, toMillis(hikari.getMaxLifetime()));
    }

    /**
     * 主动获取一次连接以验证目标库可访问。
     *
     * @param dataSource Hikari 数据源
     * @throws SQLException 连接获取失败时抛出
     */
    private void validateConnection(HikariDataSource dataSource) throws SQLException {
        try (Connection ignored = dataSource.getConnection()) {
            // 只验证连接可获取，不执行任何业务 SQL。
        }
    }

    /**
     * 构造不包含 JDBC URL 或密码的失败消息。
     *
     * @param defaults    数据源默认配置
     * @param project     项目配置
     * @param environment 环境配置
     * @return 脱敏失败消息
     */
    private String sanitizedFailureMessage(
            DbflowProperties.DatasourceDefaults defaults,
            DbflowProperties.Project project,
            DbflowProperties.Environment environment) {
        DataSourceKey key = new DataSourceKey(project.getKey(), environment.getKey());
        if (defaults.isValidateOnStartup()) {
            return "目标数据源启动校验失败: " + key.toDisplayName();
        }
        return "目标数据源初始化失败: " + key.toDisplayName();
    }

    /**
     * 解析环境值和默认值中的第一个非空白文本。
     *
     * @param environmentValue 环境级配置值
     * @param defaultValue     默认配置值
     * @return 有效配置值
     */
    private String resolveText(String environmentValue, String defaultValue) {
        if (StringUtils.hasText(environmentValue)) {
            return environmentValue;
        }
        return defaultValue;
    }

    /**
     * 解析敏感值；环境级配置存在时覆盖默认配置，允许空字符串作为显式空密码。
     *
     * @param environmentValue 环境级敏感配置
     * @param defaultValue     默认敏感配置
     * @return 有效敏感配置
     */
    private String resolveSecret(String environmentValue, String defaultValue) {
        if (environmentValue != null) {
            return environmentValue;
        }
        return defaultValue;
    }

    /**
     * 创建 Hikari 池名称。
     *
     * @param hikari         Hikari 共享配置
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @return Hikari 池名称
     */
    private String poolName(DbflowProperties.Hikari hikari, String projectKey, String environmentKey) {
        String prefix = StringUtils.hasText(hikari.getPoolNamePrefix()) ? hikari.getPoolNamePrefix() : "dbflow-target";
        return sanitizePoolSegment(prefix) + "-" + sanitizePoolSegment(projectKey) + "-"
                + sanitizePoolSegment(environmentKey);
    }

    /**
     * 将池名称片段转换为 Hikari 友好的可读文本。
     *
     * @param value 原始片段
     * @return 脱敏后的池名称片段
     */
    private String sanitizePoolSegment(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }
        return value.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    /**
     * 将时间配置转换为毫秒。
     *
     * @param duration 时间配置
     * @return 毫秒值
     */
    private Long toMillis(Duration duration) {
        return duration == null ? null : duration.toMillis();
    }

    /**
     * 在配置值非空白时设置文本。
     *
     * @param setter 配置写入器
     * @param value  配置值
     */
    private void setIfHasText(StringSetter setter, String value) {
        if (StringUtils.hasText(value)) {
            setter.accept(value);
        }
    }

    /**
     * 在配置值非空时设置对象。
     *
     * @param setter 配置写入器
     * @param value  配置值
     * @param <T>    配置值类型
     */
    private <T> void setIfPresent(ValueSetter<T> setter, T value) {
        if (value != null) {
            setter.accept(value);
        }
    }

    /**
     * 安静关闭 Hikari 数据源。
     *
     * @param dataSource Hikari 数据源
     */
    private void closeQuietly(HikariDataSource dataSource) {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * 字符串配置写入器。
     *
     * @author refinex
     */
    @FunctionalInterface
    private interface StringSetter {

        /**
         * 写入字符串配置。
         *
         * @param value 字符串配置值
         */
        void accept(String value);
    }

    /**
     * 泛型配置写入器。
     *
     * @param <T> 配置值类型
     * @author refinex
     */
    @FunctionalInterface
    private interface ValueSetter<T> {

        /**
         * 写入配置值。
         *
         * @param value 配置值
         */
        void accept(T value);
    }

    /**
     * 目标库数据源键。
     *
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     */
    private record DataSourceKey(String projectKey, String environmentKey) {

        /**
         * 返回面向诊断的目标标识。
         *
         * @return 目标标识
         */
        private String toDisplayName() {
            return projectKey + "/" + environmentKey;
        }
    }
}
