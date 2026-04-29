package com.refinex.dbflow.executor;

import com.refinex.dbflow.access.entity.DbfApiToken;
import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.service.AccessService;
import com.refinex.dbflow.access.service.ProjectEnvironmentCatalogService;
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
 * MySQL 8 和 MySQL 5.7 schema inspect 集成测试。
 *
 * @author refinex
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:schema_inspect_mysql_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class SchemaInspectServiceMysqlTests {

    /**
     * MySQL 8 测试容器。
     */
    @Container
    private static final MySQLContainer<?> MYSQL_8 = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.36"))
            .withDatabaseName("dbflow_schema")
            .withUsername("dbflow_user")
            .withPassword("dbflow_pass");

    /**
     * MySQL 5.7 测试容器。
     */
    @Container
    private static final MySQLContainer<?> MYSQL_57 = new MySQLContainer<>(DockerImageName.parse("mysql:5.7.44"))
            .withDatabaseName("dbflow_schema")
            .withUsername("dbflow_user")
            .withPassword("dbflow_pass");

    /**
     * schema inspect 服务。
     */
    @Autowired
    private SchemaInspectService schemaInspectService;

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
     * 每个测试前创建授权夹具和目标库对象。
     */
    @BeforeEach
    void setUp() throws SQLException {
        catalogService.syncConfiguredProjectEnvironments();
        user = accessService.createUser("schema-mysql-" + UUID.randomUUID(), "Schema MySQL", "hash");
        token = accessService.issueActiveToken(
                user.getId(),
                "hash-" + UUID.randomUUID(),
                "dbf_sch",
                Instant.now().plus(1, ChronoUnit.DAYS)
        );
        accessService.grantEnvironment(user.getId(), "mysql8", "dev", "WRITE");
        accessService.grantEnvironment(user.getId(), "mysql57", "dev", "WRITE");
        recreateObjects(MYSQL_8);
        recreateObjects(MYSQL_57);
    }

    /**
     * 验证 MySQL 8 schema inspect 返回表、字段、索引、视图和 routine 元数据。
     */
    @Test
    void shouldInspectMysql8SchemaObjects() {
        SchemaInspectResult result = schemaInspectService.inspect(request("mysql8", "dbflow_schema", null, 200));

        assertThat(result.allowed()).isTrue();
        assertThat(result.status()).isEqualTo("INSPECTED");
        assertThat(result.schemas()).anyMatch(schema -> schema.name().equals("dbflow_schema"));
        assertThat(result.tables()).anyMatch(table -> table.name().equals("p08_orders")
                && table.type().equals("BASE TABLE")
                && table.comment().contains("orders table"));
        assertThat(result.columns()).anyMatch(column -> column.tableName().equals("p08_orders")
                && column.name().equals("status")
                && column.nullable()
                && ("NEW".equals(column.defaultValue()) || "'NEW'".equals(column.defaultValue()))
                && column.comment().contains("订单状态"));
        assertThat(result.indexes()).anyMatch(index -> index.tableName().equals("p08_orders")
                && index.name().equals("idx_status")
                && index.columnName().equals("status"));
        assertThat(result.views()).anyMatch(view -> view.name().equals("v_p08_orders"));
        assertThat(result.routines()).anyMatch(routine -> routine.name().equals("p08_order_count")
                && routine.type().equals("PROCEDURE"));
        assertThat(result.routines()).anyMatch(routine -> routine.name().equals("f_p08_status_label")
                && routine.type().equals("FUNCTION"));
        assertThat(result.toString()).doesNotContain(MYSQL_8.getJdbcUrl(), MYSQL_8.getPassword());
    }

    /**
     * 验证 table 过滤只返回指定表的表、字段和索引元数据。
     */
    @Test
    void shouldFilterBySchemaAndTable() {
        SchemaInspectResult result = schemaInspectService.inspect(request(
                "mysql8",
                "dbflow_schema",
                "p08_orders",
                200
        ));

        assertThat(result.allowed()).isTrue();
        assertThat(result.tables()).extracting(SchemaTableMetadata::name).containsExactly("p08_orders");
        assertThat(result.columns()).allMatch(column -> column.tableName().equals("p08_orders"));
        assertThat(result.indexes()).allMatch(index -> index.tableName().equals("p08_orders"));
        assertThat(result.views()).isEmpty();
    }

    /**
     * 验证结果上限会截断大 schema 响应。
     */
    @Test
    void shouldMarkTruncatedWhenMaxItemsExceeded() {
        SchemaInspectResult result = schemaInspectService.inspect(request("mysql8", "dbflow_schema", null, 1));

        assertThat(result.allowed()).isTrue();
        assertThat(result.truncated()).isTrue();
        assertThat(result.columns()).hasSizeLessThanOrEqualTo(1);
        assertThat(result.maxItems()).isEqualTo(1);
    }

    /**
     * 验证 MySQL 5.7 兼容基础 schema 元数据查询。
     */
    @Test
    void shouldInspectMysql57SchemaObjects() {
        SchemaInspectResult result = schemaInspectService.inspect(request("mysql57", "dbflow_schema", null, 200));

        assertThat(result.allowed()).isTrue();
        assertThat(result.tables()).anyMatch(table -> table.name().equals("p08_orders"));
        assertThat(result.columns()).anyMatch(column -> column.name().equals("status"));
        assertThat(result.indexes()).anyMatch(index -> index.name().equals("idx_status"));
        assertThat(result.routines()).anyMatch(routine -> routine.name().equals("p08_order_count")
                && routine.type().equals("PROCEDURE"));
        assertThat(result.routines()).anyMatch(routine -> routine.name().equals("f_p08_status_label")
                && routine.type().equals("FUNCTION"));
    }

    /**
     * 创建 schema inspect 请求。
     *
     * @param project  项目标识
     * @param schema   schema 过滤
     * @param table    表过滤
     * @param maxItems 最大返回条目数
     * @return schema inspect 请求
     */
    private SchemaInspectRequest request(String project, String schema, String table, int maxItems) {
        return new SchemaInspectRequest(
                "req-" + UUID.randomUUID(),
                user.getId(),
                token.getId(),
                token.getTokenPrefix(),
                project,
                "dev",
                schema,
                table,
                maxItems
        );
    }

    /**
     * 重建目标库对象。
     *
     * @param container MySQL 容器
     * @throws SQLException 重建失败时抛出
     */
    private void recreateObjects(MySQLContainer<?> container) throws SQLException {
        try (java.sql.Connection connection = DriverManager.getConnection(
                container.getJdbcUrl(),
                container.getUsername(),
                container.getPassword()
        );
             Statement statement = connection.createStatement()) {
            statement.execute("DROP VIEW IF EXISTS v_p08_orders");
            statement.execute("DROP FUNCTION IF EXISTS f_p08_status_label");
            statement.execute("DROP PROCEDURE IF EXISTS p08_order_count");
            statement.execute("DROP TABLE IF EXISTS p08_orders");
            statement.execute("""
                    CREATE TABLE p08_orders (
                        id INT PRIMARY KEY COMMENT '订单标识',
                        customer_id INT NOT NULL COMMENT '客户标识',
                        status VARCHAR(32) DEFAULT 'NEW' COMMENT '订单状态',
                        note VARCHAR(64) NULL COMMENT '备注',
                        INDEX idx_status (status),
                        INDEX idx_customer_status (customer_id, status)
                    ) COMMENT='orders table'
                    """);
            statement.execute("CREATE VIEW v_p08_orders AS SELECT id, status FROM p08_orders");
            statement.execute("""
                    CREATE PROCEDURE p08_order_count()
                    READS SQL DATA
                    BEGIN
                        SELECT COUNT(*) FROM p08_orders;
                    END
                    """);
            statement.execute("""
                    CREATE FUNCTION f_p08_status_label(input_status VARCHAR(32))
                    RETURNS VARCHAR(32)
                    DETERMINISTIC
                    NO SQL
                    RETURN input_status
                    """);
        }
    }
}
