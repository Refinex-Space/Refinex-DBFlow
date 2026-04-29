package com.refinex.dbflow.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DBFlow YAML 配置绑定与启动期校验测试。
 *
 * @author refinex
 */
class DbflowPropertiesTests {

    /**
     * 轻量配置上下文运行器，仅加载 DBFlow 配置属性绑定。
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    /**
     * 创建合法配置属性数组。
     *
     * @param overrides 覆盖属性
     * @return 合法配置属性数组
     */
    private static String[] validProperties(String... overrides) {
        String[] base = {
                "dbflow.datasource-defaults.driver-class-name=com.mysql.cj.jdbc.Driver",
                "dbflow.datasource-defaults.username=dbflow_demo",
                "dbflow.datasource-defaults.password=${DBFLOW_DEMO_PASSWORD:}",
                "dbflow.datasource-defaults.validate-on-startup=false",
                "dbflow.datasource-defaults.hikari.maximum-pool-size=8",
                "dbflow.datasource-defaults.hikari.minimum-idle=2",
                "dbflow.datasource-defaults.hikari.connection-timeout=3s",
                "dbflow.datasource-defaults.hikari.idle-timeout=10m",
                "dbflow.datasource-defaults.hikari.max-lifetime=30m",
                "dbflow.datasource-defaults.hikari.pool-name-prefix=dbflow-target",
                "dbflow.projects[0].key=demo",
                "dbflow.projects[0].name=Demo Project",
                "dbflow.projects[0].environments[0].key=dev",
                "dbflow.projects[0].environments[0].name=Development",
                "dbflow.projects[0].environments[0].jdbc-url=jdbc:mysql://127.0.0.1:3306/refinex_demo",
                "dbflow.projects[0].environments[0].password=${DBFLOW_DEMO_ENV_PASSWORD:}",
                "dbflow.policies.dangerous-ddl.whitelist[0].project-key=demo",
                "dbflow.policies.dangerous-ddl.whitelist[0].environment-key=dev",
                "dbflow.policies.dangerous-ddl.whitelist[0].schema-name=refinex_demo",
                "dbflow.policies.dangerous-ddl.whitelist[0].table-name=orders",
                "dbflow.policies.dangerous-ddl.whitelist[0].operation=DROP_TABLE",
                "dbflow.policies.dangerous-ddl.whitelist[1].project-key=demo",
                "dbflow.policies.dangerous-ddl.whitelist[1].environment-key=dev",
                "dbflow.policies.dangerous-ddl.whitelist[1].schema-name=refinex_demo",
                "dbflow.policies.dangerous-ddl.whitelist[1].operation=DROP_DATABASE",
                "dbflow.policies.dangerous-ddl.whitelist[1].allow-prod-dangerous-ddl=true"
        };
        for (String override : overrides) {
            String overrideKey = override.substring(0, override.indexOf('='));
            for (int index = 0; index < base.length; index++) {
                if (base[index].startsWith(overrideKey + "=")) {
                    base[index] = override;
                    break;
                }
            }
        }
        return base;
    }

    /**
     * 断言启动失败信息包含指定片段。
     *
     * @param failure         启动失败异常
     * @param expectedMessage 预期错误信息片段
     */
    private static void assertStartupFailureContains(Throwable failure, String expectedMessage) {
        assertThat(failure).isNotNull();
        assertThat(failure).hasMessageContaining(expectedMessage);
    }

    /**
     * 验证完整 dbflow 配置能够绑定为强类型配置模型。
     */
    @Test
    void shouldBindDatasourceProjectsAndDangerousDdlPolicy() {
        contextRunner.withPropertyValues(validProperties()).run(context -> {
            assertThat(context).hasSingleBean(DbflowProperties.class);

            DbflowProperties properties = context.getBean(DbflowProperties.class);
            assertThat(properties.getDatasourceDefaults().getDriverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
            assertThat(properties.getDatasourceDefaults().isValidateOnStartup()).isFalse();
            assertThat(properties.getDatasourceDefaults().getHikari().getMaximumPoolSize()).isEqualTo(8);
            assertThat(properties.getDatasourceDefaults().getHikari().getMinimumIdle()).isEqualTo(2);
            assertThat(properties.getDatasourceDefaults().getHikari().getConnectionTimeout())
                    .isEqualTo(Duration.ofSeconds(3));
            assertThat(properties.getProjects()).hasSize(1);
            assertThat(properties.getProjects().get(0).getEnvironments()).hasSize(1);
            assertThat(properties.getProjects().get(0).getEnvironments().get(0).getJdbcUrl())
                    .isEqualTo("jdbc:mysql://127.0.0.1:3306/refinex_demo");
            assertThat(properties.getPolicies().getDangerousDdl().getWhitelist()).hasSize(2);
            assertThat(properties.getPolicies().getDangerousDdl().getWhitelist().get(1).isAllowProdDangerousDdl())
                    .isTrue();
        });
    }

    /**
     * 验证非法 Hikari 连接池配置会导致启动失败。
     */
    @Test
    void shouldRejectIllegalHikariPoolSettings() {
        contextRunner.withPropertyValues(
                validProperties(
                        "dbflow.datasource-defaults.hikari.maximum-pool-size=1",
                        "dbflow.datasource-defaults.hikari.minimum-idle=2"
                )
        ).run(context -> assertStartupFailureContains(context.getStartupFailure(), "minimum-idle 不能大于"));
    }

    /**
     * 验证高危 DDL 默认策略满足安全基线。
     */
    @Test
    void shouldProvideDangerousDdlDefaults() {
        contextRunner.run(context -> {
            DbflowProperties properties = context.getBean(DbflowProperties.class);

            assertThat(properties.getPolicies().getDangerousDdl().defaultDecision(DangerousDdlOperation.DROP_TABLE))
                    .isEqualTo(DangerousDdlDecision.DENY);
            assertThat(properties.getPolicies().getDangerousDdl().defaultDecision(DangerousDdlOperation.DROP_DATABASE))
                    .isEqualTo(DangerousDdlDecision.DENY);
            assertThat(properties.getPolicies().getDangerousDdl().defaultDecision(DangerousDdlOperation.TRUNCATE))
                    .isEqualTo(DangerousDdlDecision.REQUIRE_CONFIRMATION);
        });
    }

    /**
     * 验证重复 project key 会导致启动失败。
     */
    @Test
    void shouldRejectDuplicateProjectKey() {
        contextRunner.withPropertyValues(
                "dbflow.datasource-defaults.driver-class-name=com.mysql.cj.jdbc.Driver",
                "dbflow.projects[0].key=demo",
                "dbflow.projects[0].environments[0].key=dev",
                "dbflow.projects[0].environments[0].jdbc-url=jdbc:mysql://127.0.0.1:3306/demo",
                "dbflow.projects[1].key=demo",
                "dbflow.projects[1].environments[0].key=test",
                "dbflow.projects[1].environments[0].jdbc-url=jdbc:mysql://127.0.0.1:3306/demo_test"
        ).run(context -> assertStartupFailureContains(context.getStartupFailure(), "重复 project key"));
    }

    /**
     * 验证同一项目下重复 environment key 会导致启动失败。
     */
    @Test
    void shouldRejectDuplicateEnvironmentKey() {
        contextRunner.withPropertyValues(
                "dbflow.datasource-defaults.driver-class-name=com.mysql.cj.jdbc.Driver",
                "dbflow.projects[0].key=demo",
                "dbflow.projects[0].environments[0].key=dev",
                "dbflow.projects[0].environments[0].jdbc-url=jdbc:mysql://127.0.0.1:3306/demo",
                "dbflow.projects[0].environments[1].key=dev",
                "dbflow.projects[0].environments[1].jdbc-url=jdbc:mysql://127.0.0.1:3306/demo_shadow"
        ).run(context -> assertStartupFailureContains(context.getStartupFailure(), "重复 environment key"));
    }

    /**
     * 验证缺失目标库 JDBC URL 会导致启动失败。
     */
    @Test
    void shouldRejectMissingJdbcUrl() {
        contextRunner.withPropertyValues(
                "dbflow.datasource-defaults.driver-class-name=com.mysql.cj.jdbc.Driver",
                "dbflow.projects[0].key=demo",
                "dbflow.projects[0].environments[0].key=dev"
        ).run(context -> assertStartupFailureContains(context.getStartupFailure(), "jdbc-url 不能为空"));
    }

    /**
     * 验证目标库 JDBC URL 不能携带密码参数，避免连接池日志泄露密码。
     */
    @Test
    void shouldRejectPasswordInJdbcUrl() {
        contextRunner.withPropertyValues(
                validProperties(
                        "dbflow.projects[0].environments[0].jdbc-url=jdbc:mysql://127.0.0.1:3306/demo?password=secret-value"
                )
        ).run(context -> {
            assertStartupFailureContains(context.getStartupFailure(), "jdbc-url 不能包含密码参数");
            assertThat(context.getStartupFailure()).hasMessageNotContaining("secret-value");
        });
    }

    /**
     * 验证环境和默认配置都缺失驱动时会导致启动失败。
     */
    @Test
    void shouldRejectMissingDriver() {
        contextRunner.withPropertyValues(
                "dbflow.projects[0].key=demo",
                "dbflow.projects[0].environments[0].key=dev",
                "dbflow.projects[0].environments[0].jdbc-url=jdbc:mysql://127.0.0.1:3306/demo"
        ).run(context -> assertStartupFailureContains(context.getStartupFailure(), "driver 不能为空"));
    }

    /**
     * 验证非法高危 DDL 白名单粒度会导致启动失败。
     */
    @Test
    void shouldRejectIllegalDangerousDdlWhitelist() {
        contextRunner.withPropertyValues(
                validProperties(
                        "dbflow.policies.dangerous-ddl.whitelist[0].operation=DROP_DATABASE",
                        "dbflow.policies.dangerous-ddl.whitelist[0].table-name=orders"
                )
        ).run(context -> assertStartupFailureContains(context.getStartupFailure(), "DROP_DATABASE 白名单不能配置 table-name"));
    }

    /**
     * 测试专用配置属性启用类。
     *
     * @author refinex
     */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DbflowProperties.class)
    static class TestConfiguration {
    }
}
