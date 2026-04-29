package com.refinex.dbflow.executor;

import com.refinex.dbflow.access.entity.DbfApiToken;
import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.service.AccessDecisionService;
import com.refinex.dbflow.access.service.AccessService;
import com.refinex.dbflow.access.service.ProjectEnvironmentCatalogService;
import com.refinex.dbflow.audit.entity.DbfAuditEvent;
import com.refinex.dbflow.audit.service.AuditEventWriter;
import com.refinex.dbflow.audit.service.AuditService;
import com.refinex.dbflow.config.DbflowProperties;
import com.refinex.dbflow.sqlpolicy.DangerousDdlPolicyEngine;
import com.refinex.dbflow.sqlpolicy.SqlClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.AbstractDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SQL EXPLAIN 服务非容器边界测试。
 *
 * @author refinex
 */
@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:sql_explain_service_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.datasource-defaults.driver-class-name=com.mysql.cj.jdbc.Driver",
        "dbflow.projects[0].key=demo",
        "dbflow.projects[0].name=Demo Project",
        "dbflow.projects[0].environments[0].key=dev",
        "dbflow.projects[0].environments[0].name=Development",
        "dbflow.projects[0].environments[0].jdbc-url=jdbc:mysql://127.0.0.1:3306/explain_dev"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableConfigurationProperties(DbflowProperties.class)
@Import({
        AccessService.class,
        AccessDecisionService.class,
        AuditService.class,
        AuditEventWriter.class,
        ProjectEnvironmentCatalogService.class,
        SqlClassifier.class,
        DangerousDdlPolicyEngine.class,
        SqlExplainService.class,
        SqlExplainServiceTests.TestBeans.class
})
class SqlExplainServiceTests {

    /**
     * SQL EXPLAIN 服务。
     */
    @Autowired
    private SqlExplainService sqlExplainService;

    /**
     * 访问控制服务。
     */
    @Autowired
    private AccessService accessService;

    /**
     * 审计服务。
     */
    @Autowired
    private AuditService auditService;

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
     * 每个测试前创建元数据夹具。
     */
    @BeforeEach
    void setUp() {
        catalogService.syncConfiguredProjectEnvironments();
        user = accessService.createUser("explain-" + UUID.randomUUID(), "Explain User", "hash");
        token = accessService.issueActiveToken(
                user.getId(),
                "hash-" + UUID.randomUUID(),
                "dbf_explain",
                Instant.now().plus(1, ChronoUnit.DAYS)
        );
    }

    /**
     * 验证未授权环境会被拒绝，且不会访问目标数据源。
     */
    @Test
    void shouldDenyUnauthorizedEnvironmentBeforeTargetAccess() {
        SqlExplainResult result = sqlExplainService.explain(request("SELECT * FROM p07_orders"));

        assertThat(result.status()).isEqualTo("DENIED");
        assertThat(result.allowed()).isFalse();
        assertThat(result.errorCode()).isEqualTo("GRANT_NOT_FOUND");
        assertThat(result.planRows()).isEmpty();
        assertThat(auditEvents()).extracting(DbfAuditEvent::getDecision)
                .contains("REQUEST_RECEIVED", "POLICY_DENIED");
    }

    /**
     * 验证语法错误会在分类阶段被拒绝，且不会访问目标数据源。
     */
    @Test
    void shouldRejectSyntaxErrorBeforeTargetAccess() {
        accessService.grantEnvironment(user.getId(), "demo", "dev", "WRITE");

        SqlExplainResult result = sqlExplainService.explain(request("UPDATE p07_orders SET status = WHERE id = 1"));

        assertThat(result.status()).isEqualTo("DENIED");
        assertThat(result.allowed()).isFalse();
        assertThat(result.errorCode()).isEqualTo("CLASSIFICATION_REJECTED");
        assertThat(result.planRows()).isEmpty();
        assertThat(auditEvents()).allSatisfy(event -> {
            assertThat(event.getTokenId()).isEqualTo(token.getId());
            assertThat(event.getClientName()).isNotBlank();
            assertThat(event.getClientVersion()).isNotBlank();
            assertThat(event.getUserAgent()).isNotBlank();
            assertThat(event.getSourceIp()).isNotBlank();
            assertThat(event.getTool()).isEqualTo("dbflow_explain_sql");
        });
        assertThat(auditEvents()).extracting(DbfAuditEvent::getDecision)
                .contains("REQUEST_RECEIVED", "POLICY_DENIED");
    }

    /**
     * 验证目标库异常路径会写入失败审计。
     */
    @Test
    void shouldAuditFailureWhenTargetConnectionFails() {
        accessService.grantEnvironment(user.getId(), "demo", "dev", "WRITE");

        SqlExplainResult result = sqlExplainService.explain(request("SELECT * FROM p07_orders"));

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.allowed()).isFalse();
        assertThat(auditEvents()).extracting(DbfAuditEvent::getDecision)
                .contains("REQUEST_RECEIVED", "FAILED");
    }

    /**
     * 创建 EXPLAIN 请求。
     *
     * @param sql SQL 原文
     * @return EXPLAIN 请求
     */
    private SqlExplainRequest request(String sql) {
        return new SqlExplainRequest(
                "req-" + UUID.randomUUID(),
                user.getId(),
                token.getId(),
                token.getTokenPrefix(),
                "demo",
                "dev",
                sql,
                null
        );
    }

    /**
     * 查询当前测试用户审计事件。
     *
     * @return 审计事件列表
     */
    private List<DbfAuditEvent> auditEvents() {
        return auditService.findRecentByUser(user.getId());
    }

    /**
     * 测试专用 Bean。
     *
     * @author refinex
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {

        /**
         * 创建禁止访问的目标库注册表，验证拒绝路径不会触达目标库。
         *
         * @return 目标库注册表
         */
        @Bean
        ProjectEnvironmentDataSourceRegistry projectEnvironmentDataSourceRegistry() {
            return (projectKey, environmentKey) -> new AbstractDataSource() {
                @Override
                public Connection getConnection() throws SQLException {
                    throw new SQLException("目标库连接失败", "08001");
                }

                @Override
                public Connection getConnection(String username, String password) throws SQLException {
                    throw new SQLException("目标库连接失败", "08001");
                }
            };
        }
    }
}
