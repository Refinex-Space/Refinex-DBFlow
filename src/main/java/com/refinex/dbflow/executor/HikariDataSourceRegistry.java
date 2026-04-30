package com.refinex.dbflow.executor;

import com.refinex.dbflow.common.DbflowException;
import com.refinex.dbflow.common.ErrorCode;
import com.refinex.dbflow.config.DbflowProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于 DBFlow 项目环境配置创建并管理 Hikari 目标库数据源。
 *
 * @author refinex
 */
@Service
public class HikariDataSourceRegistry implements ProjectEnvironmentDataSourceRegistry, DisposableBean {

    /**
     * 运维日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HikariDataSourceRegistry.class);

    /**
     * 已注册的目标库数据源，key 为 projectKey/environmentKey。
     */
    private final AtomicReference<Map<DataSourceKey, HikariDataSource>> dataSources;

    /**
     * 创建 Hikari 目标库数据源注册表。
     *
     * @param properties DBFlow 配置属性
     */
    public HikariDataSourceRegistry(DbflowProperties properties) {
        this.dataSources = new AtomicReference<>(
                Collections.unmodifiableMap(createDataSources(Objects.requireNonNull(properties), false)));
        LOGGER.info("datasource.registry.initialized targetCount={}", dataSources.get().size());
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
        HikariDataSource dataSource = dataSources.get().get(key);
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
        closeAll(dataSources.getAndSet(Collections.emptyMap()));
    }

    /**
     * 使用已校验候选配置替换当前数据源快照。
     *
     * @param candidateProperties 候选 DBFlow 配置属性
     * @return 数据源重载结果
     */
    DataSourceReloadResult replaceWithCandidate(DbflowProperties candidateProperties) {
        LOGGER.info("datasource.registry.replace.started");
        Map<DataSourceKey, HikariDataSource> candidateDataSources = Collections.unmodifiableMap(
                createDataSources(Objects.requireNonNull(candidateProperties), true));
        Map<DataSourceKey, HikariDataSource> previousDataSources = dataSources.getAndSet(candidateDataSources);
        closeAll(previousDataSources);
        LOGGER.info("datasource.registry.replace.completed previousCount={} targetCount={}",
                previousDataSources.size(), candidateDataSources.size());
        return DataSourceReloadResult.success(candidateDataSources.size(), "目标数据源配置已生效");
    }

    /**
     * 根据配置创建全部目标库数据源。
     *
     * @param properties                DBFlow 配置属性
     * @param forceConnectionValidation 是否强制预热连接
     * @return 目标库数据源映射
     */
    private Map<DataSourceKey, HikariDataSource> createDataSources(
            DbflowProperties properties,
            boolean forceConnectionValidation) {
        Map<DataSourceKey, HikariDataSource> targetDataSources = new LinkedHashMap<>();
        DbflowProperties.DatasourceDefaults defaults = properties.getDatasourceDefaults();
        try {
            for (DbflowProperties.Project project : properties.getProjects()) {
                for (DbflowProperties.Environment environment : project.getEnvironments()) {
                    DataSourceKey key = new DataSourceKey(project.getKey(), environment.getKey());
                    HikariDataSource dataSource = createDataSource(defaults, project, environment,
                            forceConnectionValidation);
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
     * @param defaults                  数据源默认配置
     * @param project                   项目配置
     * @param environment               环境配置
     * @param forceConnectionValidation 是否强制预热连接
     * @return Hikari 数据源
     */
    private HikariDataSource createDataSource(
            DbflowProperties.DatasourceDefaults defaults,
            DbflowProperties.Project project,
            DbflowProperties.Environment environment,
            boolean forceConnectionValidation) {
        boolean validateConnection = defaults.isValidateOnStartup() || forceConnectionValidation;
        HikariConfig config = buildConfig(defaults, project, environment, validateConnection);
        HikariDataSource dataSource = null;
        try {
            dataSource = new HikariDataSource(config);
            if (validateConnection) {
                validateConnection(dataSource);
            }
            LOGGER.info("datasource.target.created project={} env={} validateConnection={}",
                    project.getKey(), environment.getKey(), validateConnection);
            return dataSource;
        } catch (RuntimeException | SQLException exception) {
            closeQuietly(dataSource);
            LOGGER.warn("datasource.target.create.failed project={} env={} validateConnection={} errorType={}",
                    project.getKey(), environment.getKey(), validateConnection, exception.getClass().getSimpleName());
            throw new DbflowException(ErrorCode.INTERNAL_ERROR,
                    sanitizedFailureMessage(validateConnection, project, environment));
        }
    }

    /**
     * 构建单个 Hikari 数据源配置。
     *
     * @param defaults           数据源默认配置
     * @param project            项目配置
     * @param environment        环境配置
     * @param validateConnection 是否需要启动期连接校验
     * @return Hikari 配置
     */
    private HikariConfig buildConfig(
            DbflowProperties.DatasourceDefaults defaults,
            DbflowProperties.Project project,
            DbflowProperties.Environment environment,
            boolean validateConnection) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(environment.getJdbcUrl());
        config.setDriverClassName(resolveText(environment.getDriverClassName(), defaults.getDriverClassName()));
        setIfHasText(config::setUsername, resolveText(environment.getUsername(), defaults.getUsername()));
        setIfPresent(config::setPassword, resolveSecret(environment.getPassword(), defaults.getPassword()));
        config.setPoolName(poolName(defaults.getHikari(), project.getKey(), environment.getKey()));
        applySharedHikari(config, defaults.getHikari());
        config.setInitializationFailTimeout(validateConnection ? 1L : -1L);
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
     * @param validateConnection 是否需要启动期连接校验
     * @param project            项目配置
     * @param environment        环境配置
     * @return 脱敏失败消息
     */
    private String sanitizedFailureMessage(
            boolean validateConnection,
            DbflowProperties.Project project,
            DbflowProperties.Environment environment) {
        DataSourceKey key = new DataSourceKey(project.getKey(), environment.getKey());
        if (validateConnection) {
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
     * 安静关闭一组 Hikari 数据源。
     *
     * @param targetDataSources 目标库数据源映射
     */
    private void closeAll(Map<DataSourceKey, HikariDataSource> targetDataSources) {
        for (HikariDataSource dataSource : targetDataSources.values()) {
            closeQuietly(dataSource);
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
