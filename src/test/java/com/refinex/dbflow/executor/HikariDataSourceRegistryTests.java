package com.refinex.dbflow.executor;

import com.refinex.dbflow.common.DbflowException;
import com.refinex.dbflow.config.DbflowProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 项目环境级 Hikari 数据源注册表测试。
 *
 * @author refinex
 */
class HikariDataSourceRegistryTests {

    /**
     * 测试专用配置上下文运行器。
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    /**
     * 创建多项目多环境合法配置。
     *
     * @param overrides 覆盖属性
     * @return 配置属性数组
     */
    private static String[] targetProperties(String... overrides) {
        String[] base = {
                "dbflow.datasource-defaults.driver-class-name=org.h2.Driver",
                "dbflow.datasource-defaults.username=sa",
                "dbflow.datasource-defaults.password=${DBFLOW_TARGET_PASSWORD:}",
                "dbflow.datasource-defaults.validate-on-startup=false",
                "dbflow.datasource-defaults.hikari.maximum-pool-size=4",
                "dbflow.datasource-defaults.hikari.minimum-idle=1",
                "dbflow.datasource-defaults.hikari.connection-timeout=1s",
                "dbflow.datasource-defaults.hikari.idle-timeout=10m",
                "dbflow.datasource-defaults.hikari.max-lifetime=30m",
                "dbflow.datasource-defaults.hikari.pool-name-prefix=dbflow-target",
                "dbflow.projects[0].key=demo",
                "dbflow.projects[0].environments[0].key=dev",
                "dbflow.projects[0].environments[0].jdbc-url=jdbc:h2:mem:target_demo_dev;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "dbflow.projects[0].environments[1].key=prod",
                "dbflow.projects[0].environments[1].jdbc-url=jdbc:h2:mem:target_demo_prod;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "dbflow.projects[1].key=ops",
                "dbflow.projects[1].environments[0].key=dev",
                "dbflow.projects[1].environments[0].jdbc-url=jdbc:h2:mem:target_ops_dev;MODE=MySQL;DB_CLOSE_DELAY=-1"
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
     * 验证多项目多环境会创建相互隔离的数据源并继承共享 Hikari 配置。
     */
    @Test
    void shouldCreateIsolatedDataSourcesForEveryConfiguredEnvironment() {
        contextRunner.withPropertyValues(targetProperties()).run(context -> {
            ProjectEnvironmentDataSourceRegistry registry =
                    context.getBean(ProjectEnvironmentDataSourceRegistry.class);

            HikariDataSource demoDev = (HikariDataSource) registry.getDataSource("demo", "dev");
            HikariDataSource demoProd = (HikariDataSource) registry.getDataSource("demo", "prod");
            HikariDataSource opsDev = (HikariDataSource) registry.getDataSource("ops", "dev");

            assertThat(demoDev).isNotSameAs(demoProd).isNotSameAs(opsDev);
            assertThat(demoDev.getJdbcUrl()).contains("target_demo_dev");
            assertThat(demoProd.getJdbcUrl()).contains("target_demo_prod");
            assertThat(opsDev.getJdbcUrl()).contains("target_ops_dev");
            assertThat(demoDev.getMaximumPoolSize()).isEqualTo(4);
            assertThat(demoProd.getMinimumIdle()).isEqualTo(1);
            assertThat(opsDev.getConnectionTimeout()).isEqualTo(1000L);
            assertThat(demoDev.getPoolName()).isEqualTo("dbflow-target-demo-dev");
        });
    }

    /**
     * 验证缺失环境查找会失败且不会回退到默认元数据库。
     */
    @Test
    void shouldRejectMissingEnvironmentWithoutFallback() {
        contextRunner.withPropertyValues(targetProperties()).run(context -> {
            ProjectEnvironmentDataSourceRegistry registry =
                    context.getBean(ProjectEnvironmentDataSourceRegistry.class);

            assertThatThrownBy(() -> registry.getDataSource("demo", "missing"))
                    .isInstanceOf(DbflowException.class)
                    .hasMessageContaining("未配置目标数据源")
                    .hasMessageContaining("demo/missing")
                    .hasMessageNotContaining("DBFLOW_TARGET_PASSWORD");
        });
    }

    /**
     * 验证关闭 Spring 上下文时会关闭所有注册的数据源。
     */
    @Test
    void shouldCloseAllPoolsWhenContextStops() {
        AtomicReference<HikariDataSource> dataSourceReference = new AtomicReference<>();

        contextRunner.withPropertyValues(targetProperties()).run(context -> {
            ProjectEnvironmentDataSourceRegistry registry =
                    context.getBean(ProjectEnvironmentDataSourceRegistry.class);
            dataSourceReference.set((HikariDataSource) registry.getDataSource("demo", "dev"));
            assertThat(dataSourceReference.get().isClosed()).isFalse();
        });

        assertThat(dataSourceReference.get().isClosed()).isTrue();
    }

    /**
     * 验证关闭启动期连接校验时，不可达目标库不会阻断应用上下文创建。
     */
    @Test
    void shouldAllowStartupWhenConnectionValidationDisabled() {
        contextRunner.withPropertyValues(targetProperties(
                "dbflow.projects[0].environments[0].jdbc-url=jdbc:h2:tcp://127.0.0.1:65534/mem:missing",
                "dbflow.projects[0].environments[0].password=very-secret-password",
                "dbflow.datasource-defaults.hikari.connection-timeout=250ms"
        )).run(context -> assertThat(context).hasNotFailed());
    }

    /**
     * 验证开启启动期连接校验时，不可达目标库会以脱敏错误阻断启动。
     */
    @Test
    void shouldRejectUnreachableTargetWhenConnectionValidationEnabled() {
        contextRunner.withPropertyValues(targetProperties(
                "dbflow.datasource-defaults.validate-on-startup=true",
                "dbflow.projects[0].environments[0].jdbc-url=jdbc:h2:tcp://127.0.0.1:65534/mem:missing",
                "dbflow.projects[0].environments[0].password=very-secret-password",
                "dbflow.datasource-defaults.hikari.connection-timeout=250ms"
        )).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasMessageContaining("目标数据源启动校验失败")
                    .hasMessageNotContaining("very-secret-password");
        });
    }

    /**
     * 测试专用配置类。
     *
     * @author refinex
     */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DbflowProperties.class)
    static class TestConfiguration {

        /**
         * 创建测试用 Hikari 数据源注册表。
         *
         * @param properties DBFlow 配置属性
         * @return Hikari 数据源注册表
         */
        @Bean
        HikariDataSourceRegistry hikariDataSourceRegistry(DbflowProperties properties) {
            return new HikariDataSourceRegistry(properties);
        }

    }
}
