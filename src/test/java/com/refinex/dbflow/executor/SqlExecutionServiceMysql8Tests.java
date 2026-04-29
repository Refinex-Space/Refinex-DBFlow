package com.refinex.dbflow.executor;

import com.refinex.dbflow.access.entity.DbfApiToken;
import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.service.AccessService;
import com.refinex.dbflow.access.service.ProjectEnvironmentCatalogService;
import com.refinex.dbflow.audit.entity.DbfAuditEvent;
import com.refinex.dbflow.audit.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MySQL 8 受控 SQL 执行集成测试。
 *
 * @author refinex
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sql_execution_mysql8_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.datasource-defaults.driver-class-name=com.mysql.cj.jdbc.Driver",
        "dbflow.datasource-defaults.validate-on-startup=true",
        "dbflow.datasource-defaults.hikari.maximum-pool-size=3",
        "dbflow.datasource-defaults.hikari.minimum-idle=1",
        "dbflow.projects[0].key=demo",
        "dbflow.projects[0].name=Demo Project",
        "dbflow.projects[0].environments[0].key=dev",
        "dbflow.projects[0].environments[0].name=Development"
})
class SqlExecutionServiceMysql8Tests {

    /**
     * MySQL 8 测试容器。
     */
    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.36"))
            .withDatabaseName("dbflow_target")
            .withUsername("dbflow_user")
            .withPassword("dbflow_pass");

    /**
     * SQL 执行服务。
     */
    @Autowired
    private SqlExecutionService sqlExecutionService;

    /**
     * 访问控制服务。
     */
    @Autowired
    private AccessService accessService;

    /**
     * 项目环境目录服务。
     */
    @Autowired
    private ProjectEnvironmentCatalogService catalogService;

    /**
     * 审计服务。
     */
    @Autowired
    private AuditService auditService;

    /**
     * 已授权测试夹具。
     */
    private Fixture fixture;

    /**
     * 注册动态 MySQL 容器连接属性。
     *
     * @param registry 动态属性注册表
     */
    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("dbflow.projects[0].environments[0].jdbc-url", MYSQL::getJdbcUrl);
        registry.add("dbflow.projects[0].environments[0].username", MYSQL::getUsername);
        registry.add("dbflow.projects[0].environments[0].password", MYSQL::getPassword);
    }

    /**
     * 每个测试前创建授权元数据并清理目标库对象。
     */
    @BeforeEach
    void setUp() throws SQLException {
        catalogService.syncConfiguredProjectEnvironments();
        DbfUser user = accessService.createUser("sql-exec-" + UUID.randomUUID(), "SQL Executor", "hash");
        DbfApiToken token = accessService.issueActiveToken(
                user.getId(),
                "hash-" + UUID.randomUUID(),
                "dbf_sql",
                Instant.now().plus(1, ChronoUnit.DAYS)
        );
        accessService.grantEnvironment(user.getId(), "demo", "dev", "WRITE");
        fixture = new Fixture(user, token);
        dropTargetTable();
    }

    /**
     * 验证查询类 SQL 返回限流结果和截断标记。
     */
    @Test
    void shouldReturnLimitedRowsForSelectShowDescribeAndExplain() {
        sqlExecutionService.execute(request("CREATE TABLE p07_orders (id INT PRIMARY KEY, status VARCHAR(32))"));
        sqlExecutionService.execute(request("INSERT INTO p07_orders (id, status) VALUES (1, 'NEW'), (2, 'PAID'), (3, 'DONE')"));

        SqlExecutionResult select = sqlExecutionService.execute(request(
                "SELECT id, status FROM p07_orders ORDER BY id",
                new SqlExecutionOptions(2, 2, 5)
        ));
        SqlExecutionResult show = sqlExecutionService.execute(request("SHOW TABLES LIKE 'p07_orders'"));
        SqlExecutionResult describe = sqlExecutionService.execute(request("DESCRIBE p07_orders"));
        SqlExecutionResult explain = sqlExecutionService.execute(request("EXPLAIN SELECT * FROM p07_orders WHERE id = 1"));

        assertThat(select.query()).isTrue();
        assertThat(select.columns()).containsExactly("id", "status");
        assertThat(select.rows()).hasSize(2);
        assertThat(select.truncated()).isTrue();
        assertThat(select.rows().getFirst()).containsEntry("id", 1).containsEntry("status", "NEW");
        assertThat(show.rows()).isNotEmpty();
        assertThat(describe.rows()).extracting(row -> row.get("Field")).contains("id", "status");
        assertThat(explain.rows()).isNotEmpty();
    }

    /**
     * 验证 MySQL 8 DML 和 DDL 返回影响行数、warning、耗时与语句摘要。
     */
    @Test
    void shouldExecuteDmlAndDdlWithSummaryWarningsAndAudit() {
        SqlExecutionResult create = sqlExecutionService.execute(request(
                "CREATE TABLE p07_orders (id INT PRIMARY KEY, status VARCHAR(32))"
        ));
        SqlExecutionResult insert = sqlExecutionService.execute(request(
                "INSERT INTO p07_orders (id, status) VALUES (1, 'NEW'), (2, 'PAID')"
        ));
        SqlExecutionResult update = sqlExecutionService.execute(request(
                "UPDATE p07_orders SET status = 'DONE' WHERE id = 1"
        ));
        SqlExecutionResult delete = sqlExecutionService.execute(request(
                "DELETE FROM p07_orders WHERE id = 2"
        ));
        SqlExecutionResult alter = sqlExecutionService.execute(request(
                "ALTER TABLE p07_orders ADD COLUMN note VARCHAR(64)"
        ));

        assertThat(create.query()).isFalse();
        assertThat(create.statementSummary()).contains("CREATE").contains("p07_orders");
        assertThat(insert.affectedRows()).isEqualTo(2L);
        assertThat(update.affectedRows()).isEqualTo(1L);
        assertThat(delete.affectedRows()).isEqualTo(1L);
        assertThat(alter.statementSummary()).contains("ALTER").contains("p07_orders");
        assertThat(alter.durationMillis()).isGreaterThanOrEqualTo(0L);
        assertThat(alter.warnings()).isNotNull();
        assertThat(auditService.findRecentByUser(fixture.user().getId()))
                .extracting(DbfAuditEvent::getStatus)
                .contains("ALLOWED_EXECUTED");
    }

    /**
     * 创建默认执行请求。
     *
     * @param sql SQL 原文
     * @return SQL 执行请求
     */
    private SqlExecutionRequest request(String sql) {
        return request(sql, SqlExecutionOptions.defaults());
    }

    /**
     * 创建带执行选项的请求。
     *
     * @param sql     SQL 原文
     * @param options 执行选项
     * @return SQL 执行请求
     */
    private SqlExecutionRequest request(String sql, SqlExecutionOptions options) {
        return new SqlExecutionRequest(
                "req-" + UUID.randomUUID(),
                fixture.user().getId(),
                fixture.token().getId(),
                fixture.token().getTokenPrefix(),
                "demo",
                "dev",
                sql,
                null,
                false,
                "集成测试",
                options
        );
    }

    /**
     * 直接清理测试表，避免使用 P07 服务绕过 DROP 白名单约束。
     *
     * @throws SQLException 清理失败时抛出
     */
    private void dropTargetTable() throws SQLException {
        try (java.sql.Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        );
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS p07_orders");
        }
    }

    /**
     * 已授权用户和 Token 夹具。
     *
     * @param user  用户
     * @param token Token
     */
    private record Fixture(DbfUser user, DbfApiToken token) {
    }
}
