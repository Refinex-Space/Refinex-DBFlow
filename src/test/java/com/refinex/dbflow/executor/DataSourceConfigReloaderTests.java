package com.refinex.dbflow.executor;

import com.refinex.dbflow.config.DbflowProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据源配置重载器测试。
 *
 * @author refinex
 */
class DataSourceConfigReloaderTests {

    /**
     * 测试中的数据源注册表。
     */
    private HikariDataSourceRegistry registry;

    /**
     * 测试后关闭注册表，避免连接池泄漏。
     */
    @AfterEach
    void tearDown() {
        if (registry != null) {
            registry.destroy();
        }
    }

    /**
     * 验证候选配置全部预热成功后，会替换当前注册表并关闭旧连接池。
     *
     * @throws SQLException 获取连接失败时抛出
     */
    @Test
    void shouldAtomicallyReplaceRegistryAfterCandidateWarmup() throws SQLException {
        registry = new HikariDataSourceRegistry(properties("target_reload_old"));
        DataSourceConfigReloader reloader = new DataSourceConfigReloader(registry);
        HikariDataSource oldDataSource = dataSource("demo", "dev");

        DataSourceReloadResult result = reloader.reload(properties("target_reload_new"));

        HikariDataSource newDataSource = dataSource("demo", "dev");
        assertThat(result.success()).isTrue();
        assertThat(newDataSource).isNotSameAs(oldDataSource);
        assertThat(newDataSource.getJdbcUrl()).contains("target_reload_new");
        assertThat(newDataSource.isClosed()).isFalse();
        assertThat(oldDataSource.isClosed()).isTrue();
        assertConnectionAvailable(newDataSource);
    }

    /**
     * 验证候选配置预热失败时保留旧连接池，避免既有环境不可用。
     *
     * @throws SQLException 获取连接失败时抛出
     */
    @Test
    void shouldKeepOldRegistryWhenCandidateWarmupFails() throws SQLException {
        registry = new HikariDataSourceRegistry(properties("target_reload_stable"));
        DataSourceConfigReloader reloader = new DataSourceConfigReloader(registry);
        HikariDataSource oldDataSource = dataSource("demo", "dev");

        DataSourceReloadResult result = reloader.reload(unreachableProperties());

        HikariDataSource currentDataSource = dataSource("demo", "dev");
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("目标数据源启动校验失败");
        assertThat(result.message()).doesNotContain("very-secret-password");
        assertThat(currentDataSource).isSameAs(oldDataSource);
        assertThat(oldDataSource.isClosed()).isFalse();
        assertConnectionAvailable(oldDataSource);
    }

    /**
     * 验证候选配置基础校验失败时不会关闭旧连接池。
     *
     * @throws SQLException 获取连接失败时抛出
     */
    @Test
    void shouldKeepOldRegistryWhenCandidateValidationFails() throws SQLException {
        registry = new HikariDataSourceRegistry(properties("target_reload_validation_old"));
        DataSourceConfigReloader reloader = new DataSourceConfigReloader(registry);
        HikariDataSource oldDataSource = dataSource("demo", "dev");

        DataSourceReloadResult result = reloader.reload(propertiesWithPasswordInJdbcUrl());

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("候选数据源配置校验失败");
        assertThat(dataSource("demo", "dev")).isSameAs(oldDataSource);
        assertThat(oldDataSource.isClosed()).isFalse();
        assertConnectionAvailable(oldDataSource);
    }

    /**
     * 根据项目环境获取 Hikari 数据源。
     *
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @return Hikari 数据源
     */
    private HikariDataSource dataSource(String projectKey, String environmentKey) {
        return (HikariDataSource) registry.getDataSource(projectKey, environmentKey);
    }

    /**
     * 断言数据源能够获取连接。
     *
     * @param dataSource Hikari 数据源
     * @throws SQLException 获取连接失败时抛出
     */
    private void assertConnectionAvailable(HikariDataSource dataSource) throws SQLException {
        try (Connection ignored = dataSource.getConnection()) {
            // 只验证连接可获取。
        }
    }

    /**
     * 创建合法 DBFlow 数据源配置。
     *
     * @param databaseName H2 数据库名称
     * @return DBFlow 配置
     */
    private DbflowProperties properties(String databaseName) {
        DbflowProperties properties = new DbflowProperties();
        DbflowProperties.DatasourceDefaults defaults = new DbflowProperties.DatasourceDefaults();
        defaults.setDriverClassName("org.h2.Driver");
        defaults.setUsername("sa");
        defaults.setValidateOnStartup(false);
        DbflowProperties.Hikari hikari = new DbflowProperties.Hikari();
        hikari.setPoolNamePrefix("dbflow-reload");
        hikari.setMaximumPoolSize(2);
        hikari.setMinimumIdle(0);
        defaults.setHikari(hikari);
        properties.setDatasourceDefaults(defaults);

        DbflowProperties.Project project = new DbflowProperties.Project();
        project.setKey("demo");
        DbflowProperties.Environment environment = new DbflowProperties.Environment();
        environment.setKey("dev");
        environment.setJdbcUrl("jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        project.setEnvironments(List.of(environment));
        properties.setProjects(List.of(project));
        properties.afterPropertiesSet();
        return properties;
    }

    /**
     * 创建无法连接的候选配置。
     *
     * @return 无法连接的候选配置
     */
    private DbflowProperties unreachableProperties() {
        DbflowProperties properties = properties("target_reload_unreachable");
        properties.getDatasourceDefaults().setValidateOnStartup(true);
        properties.getDatasourceDefaults().getHikari().setConnectionTimeout(java.time.Duration.ofMillis(250));
        properties.getProjects().get(0).getEnvironments().get(0)
                .setJdbcUrl("jdbc:h2:tcp://127.0.0.1:65534/mem:missing");
        properties.getProjects().get(0).getEnvironments().get(0).setPassword("very-secret-password");
        properties.afterPropertiesSet();
        return properties;
    }

    /**
     * 创建包含 JDBC URL 密码参数的非法候选配置。
     *
     * @return 非法候选配置
     */
    private DbflowProperties propertiesWithPasswordInJdbcUrl() {
        DbflowProperties properties = properties("target_reload_invalid");
        properties.getProjects().get(0).getEnvironments().get(0)
                .setJdbcUrl("jdbc:h2:mem:target_reload_invalid;password=secret-value");
        return properties;
    }
}
