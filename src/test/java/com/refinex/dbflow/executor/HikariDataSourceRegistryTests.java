package com.refinex.dbflow.executor;

import com.refinex.dbflow.common.DbflowException;
import com.refinex.dbflow.config.properties.DbflowProperties;
import com.refinex.dbflow.executor.datasource.HikariDataSourceRegistry;
import com.refinex.dbflow.executor.datasource.ProjectEnvironmentDataSourceRegistry;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

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
            assertThat(demoProd.getMinimumIdle()).isZero();
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
     * 验证关闭启动期连接校验时，目标库连接池不会为了补足 idle 连接而后台触碰业务库。
     *
     * @throws InterruptedException 等待后台线程观察窗口被中断时抛出
     */
    @Test
    void shouldNotOpenBackgroundConnectionsWhenStartupValidationDisabled() throws InterruptedException {
        CountingFailingDriver.reset();

        contextRunner.withPropertyValues(targetProperties(
                "dbflow.datasource-defaults.driver-class-name="
                        + CountingFailingDriver.class.getName(),
                "dbflow.projects[0].environments[0].jdbc-url=" + CountingFailingDriver.URL,
                "dbflow.projects[0].environments[1].jdbc-url=" + CountingFailingDriver.URL,
                "dbflow.projects[1].environments[0].jdbc-url=" + CountingFailingDriver.URL,
                "dbflow.datasource-defaults.hikari.minimum-idle=1",
                "dbflow.datasource-defaults.hikari.connection-timeout=250ms"
        )).run(context -> {
            assertThat(context).hasNotFailed();
            assertNoConnectionAttemptsFor(Duration.ofMillis(800));
        });
    }

    /**
     * 验证开启启动期连接校验时仍保留配置的最小空闲连接数。
     */
    @Test
    void shouldPreserveMinimumIdleWhenStartupValidationEnabled() {
        contextRunner.withPropertyValues(targetProperties(
                "dbflow.datasource-defaults.validate-on-startup=true",
                "dbflow.datasource-defaults.hikari.minimum-idle=1"
        )).run(context -> {
            ProjectEnvironmentDataSourceRegistry registry =
                    context.getBean(ProjectEnvironmentDataSourceRegistry.class);

            HikariDataSource demoDev = (HikariDataSource) registry.getDataSource("demo", "dev");

            assertThat(demoDev.getMinimumIdle()).isEqualTo(1);
        });
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

    /**
     * 统计连接尝试并始终失败的测试 JDBC Driver。
     *
     * @author refinex
     */
    public static final class CountingFailingDriver implements Driver {

        /**
         * 测试 JDBC URL。
         */
        private static final String URL = "jdbc:dbflow-test-unreachable://target";

        /**
         * 连接尝试次数。
         */
        private static final AtomicInteger CONNECTION_ATTEMPTS = new AtomicInteger();

        static {
            try {
                DriverManager.registerDriver(new CountingFailingDriver());
            } catch (SQLException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }

        /**
         * 重置连接尝试计数。
         */
        private static void reset() {
            CONNECTION_ATTEMPTS.set(0);
        }

        /**
         * 返回连接尝试次数。
         *
         * @return 连接尝试次数
         */
        private static int connectionAttempts() {
            return CONNECTION_ATTEMPTS.get();
        }

        /**
         * 始终拒绝测试连接并记录尝试次数。
         *
         * @param url  JDBC URL
         * @param info 连接属性
         * @return 不返回连接
         * @throws SQLException 固定抛出测试连接失败
         */
        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            if (!acceptsURL(url)) {
                return null;
            }
            CONNECTION_ATTEMPTS.incrementAndGet();
            throw new SQLException("counting driver refuses target connection");
        }

        /**
         * 判断是否支持测试 URL。
         *
         * @param url JDBC URL
         * @return 支持时返回 true
         */
        @Override
        public boolean acceptsURL(String url) {
            return URL.equals(url);
        }

        /**
         * 返回测试驱动属性信息。
         *
         * @param url  JDBC URL
         * @param info 连接属性
         * @return 空属性数组
         */
        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        /**
         * 返回主版本号。
         *
         * @return 主版本号
         */
        @Override
        public int getMajorVersion() {
            return 1;
        }

        /**
         * 返回次版本号。
         *
         * @return 次版本号
         */
        @Override
        public int getMinorVersion() {
            return 0;
        }

        /**
         * 返回是否兼容 JDBC。
         *
         * @return 测试驱动不声明兼容
         */
        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        /**
         * 返回父级日志记录器。
         *
         * @return 父级日志记录器
         * @throws SQLFeatureNotSupportedException 固定不支持
         */
        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException("not supported");
        }
    }

    /**
     * 在指定观察窗口内断言没有目标库连接尝试。
     *
     * @param duration 观察时长
     * @throws InterruptedException 等待被中断时抛出
     */
    private static void assertNoConnectionAttemptsFor(Duration duration) throws InterruptedException {
        long deadline = System.nanoTime() + duration.toNanos();
        while (System.nanoTime() < deadline) {
            assertThat(CountingFailingDriver.connectionAttempts()).isZero();
            Thread.sleep(25L);
        }
        assertThat(CountingFailingDriver.connectionAttempts()).isZero();
    }
}
