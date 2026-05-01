package com.refinex.dbflow.executor;


import com.refinex.dbflow.access.entity.DbfApiToken;
import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.service.AccessService;
import com.refinex.dbflow.access.service.ProjectEnvironmentCatalogService;
import com.refinex.dbflow.executor.dto.*;
import com.refinex.dbflow.executor.service.SqlExecutionService;
import com.refinex.dbflow.executor.service.SqlExplainService;
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
 * MySQL 8 和 MySQL 5.7 SQL EXPLAIN 集成测试。
 *
 * @author refinex
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sql_explain_mysql_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.datasource-defaults.driver-class-name=com.mysql.cj.jdbc.Driver",
        "dbflow.datasource-defaults.validate-on-startup=true",
        "dbflow.datasource-defaults.hikari.maximum-pool-size=3",
        "dbflow.datasource-defaults.hikari.minimum-idle=1",
        "dbflow.projects[0].key=mysql8",
        "dbflow.projects[0].name=MySQL 8",
        "dbflow.projects[0].environments[0].key=dev",
        "dbflow.projects[0].environments[0].name=Development",
        "dbflow.projects[1].key=mysql57",
        "dbflow.projects[1].name=MySQL 5.7",
        "dbflow.projects[1].environments[0].key=dev",
        "dbflow.projects[1].environments[0].name=Development"
})
class SqlExplainServiceMysqlTests {

    /**
     * MySQL 8 测试容器。
     */
    @Container
    private static final MySQLContainer<?> MYSQL_8 = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.36"))
            .withDatabaseName("dbflow_explain")
            .withUsername("dbflow_user")
            .withPassword("dbflow_pass");

    /**
     * MySQL 5.7 测试容器。
     */
    @Container
    private static final MySQLContainer<?> MYSQL_57 = new MySQLContainer<>(DockerImageName.parse("mysql:5.7.44"))
            .withDatabaseName("dbflow_explain")
            .withUsername("dbflow_user")
            .withPassword("dbflow_pass");

    /**
     * SQL EXPLAIN 服务。
     */
    @Autowired
    private SqlExplainService sqlExplainService;

    /**
     * SQL 执行服务，用于准备测试数据。
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
     * 已授权用户。
     */
    private DbfUser user;

    /**
     * 已授权 Token。
     */
    private DbfApiToken token;

    /**
     * 注册动态 MySQL 容器连接属性。
     *
     * @param registry 动态属性注册表
     */
    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("dbflow.projects[0].environments[0].jdbc-url", MYSQL_8::getJdbcUrl);
        registry.add("dbflow.projects[0].environments[0].username", MYSQL_8::getUsername);
        registry.add("dbflow.projects[0].environments[0].password", MYSQL_8::getPassword);
        registry.add("dbflow.projects[1].environments[0].jdbc-url", MYSQL_57::getJdbcUrl);
        registry.add("dbflow.projects[1].environments[0].username", MYSQL_57::getUsername);
        registry.add("dbflow.projects[1].environments[0].password", MYSQL_57::getPassword);
    }

    /**
     * 每个测试前创建授权夹具和目标表。
     */
    @BeforeEach
    void setUp() throws SQLException {
        catalogService.syncConfiguredProjectEnvironments();
        user = accessService.createUser("explain-mysql-" + UUID.randomUUID(), "Explain MySQL", "hash");
        token = accessService.issueActiveToken(
                user.getId(),
                "hash-" + UUID.randomUUID(),
                "dbf_exp",
                Instant.now().plus(1, ChronoUnit.DAYS)
        );
        accessService.grantEnvironment(user.getId(), "mysql8", "dev", "WRITE");
        accessService.grantEnvironment(user.getId(), "mysql57", "dev", "WRITE");
        recreateTable(MYSQL_8);
        recreateTable(MYSQL_57);
        seed("mysql8");
        seed("mysql57");
    }

    /**
     * 验证 MySQL 8 优先返回 JSON 计划摘要，并识别索引命中。
     */
    @Test
    void shouldExplainIndexedSelectWithMysql8JsonSummary() {
        SqlExplainResult result = sqlExplainService.explain(request(
                "mysql8",
                "SELECT id, customer_id FROM p07_orders WHERE customer_id = 10"
        ));

        assertThat(result.allowed()).isTrue();
        assertThat(result.status()).isEqualTo("EXPLAINED");
        assertThat(result.jsonPlanSummary()).isNotBlank();
        assertThat(result.planRows()).isNotEmpty();
        assertThat(result.planRows().getFirst().table()).isEqualTo("p07_orders");
        assertThat(result.planRows().getFirst().key()).isEqualTo("idx_customer_id");
        assertThat(result.advice()).anyMatch(advice -> advice.code().equals("INDEX_USED"));
    }

    /**
     * 验证无索引查询会返回基础索引建议。
     */
    @Test
    void shouldAdviseForUnindexedWhereClause() {
        SqlExplainResult result = sqlExplainService.explain(request(
                "mysql8",
                "SELECT id FROM p07_orders WHERE status = 'NEW'"
        ));

        assertThat(result.allowed()).isTrue();
        assertThat(result.planRows()).isNotEmpty();
        assertThat(result.advice()).anyMatch(advice -> advice.code().equals("FULL_SCAN"));
    }

    /**
     * 验证 EXPLAIN UPDATE 不会实际修改目标数据。
     */
    @Test
    void shouldExplainDmlWithoutExecutingIt() {
        SqlExplainResult result = sqlExplainService.explain(request(
                "mysql8",
                "UPDATE p07_orders SET status = 'DONE' WHERE customer_id = 10"
        ));
        SqlExecutionResult count = sqlExecutionService.execute(executeRequest(
                "mysql8",
                "SELECT COUNT(*) AS done_count FROM p07_orders WHERE status = 'DONE'"
        ));

        assertThat(result.allowed()).isTrue();
        assertThat(result.explainSql()).startsWith("EXPLAIN");
        assertThat(count.rows().getFirst()).containsEntry("done_count", 0L);
    }

    /**
     * 验证 MySQL 5.7 兼容传统 EXPLAIN 输出。
     */
    @Test
    void shouldExplainSelectWithMysql57TraditionalRows() {
        SqlExplainResult result = sqlExplainService.explain(request(
                "mysql57",
                "SELECT id FROM p07_orders WHERE customer_id = 10"
        ));

        assertThat(result.allowed()).isTrue();
        assertThat(result.format()).isIn("TRADITIONAL", "JSON");
        assertThat(result.planRows()).isNotEmpty();
        assertThat(result.planRows().getFirst().table()).isEqualTo("p07_orders");
        assertThat(result.planRows().getFirst().rows()).isGreaterThanOrEqualTo(1L);
    }

    /**
     * 创建 EXPLAIN 请求。
     *
     * @param project 项目标识
     * @param sql     SQL 原文
     * @return EXPLAIN 请求
     */
    private SqlExplainRequest request(String project, String sql) {
        return new SqlExplainRequest(
                "req-" + UUID.randomUUID(),
                user.getId(),
                token.getId(),
                token.getTokenPrefix(),
                project,
                "dev",
                sql,
                null
        );
    }

    /**
     * 创建 SQL 执行请求。
     *
     * @param project 项目标识
     * @param sql     SQL 原文
     * @return SQL 执行请求
     */
    private SqlExecutionRequest executeRequest(String project, String sql) {
        return new SqlExecutionRequest(
                "req-" + UUID.randomUUID(),
                user.getId(),
                token.getId(),
                token.getTokenPrefix(),
                project,
                "dev",
                sql,
                null,
                false,
                "EXPLAIN 集成测试",
                SqlExecutionOptions.defaults()
        );
    }

    /**
     * 填充目标表数据。
     *
     * @param project 项目标识
     */
    private void seed(String project) {
        sqlExecutionService.execute(executeRequest(
                project,
                "INSERT INTO p07_orders (id, customer_id, status) VALUES (1, 10, 'NEW'), (2, 20, 'PAID')"
        ));
    }

    /**
     * 重建目标表。
     *
     * @param container MySQL 容器
     * @throws SQLException 重建失败时抛出
     */
    private void recreateTable(MySQLContainer<?> container) throws SQLException {
        try (java.sql.Connection connection = DriverManager.getConnection(
                container.getJdbcUrl(),
                container.getUsername(),
                container.getPassword()
        );
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS p07_orders");
            statement.execute("""
                    CREATE TABLE p07_orders (
                        id INT PRIMARY KEY,
                        customer_id INT NOT NULL,
                        status VARCHAR(32) NOT NULL,
                        INDEX idx_customer_id (customer_id)
                    )
                    """);
        }
    }
}
