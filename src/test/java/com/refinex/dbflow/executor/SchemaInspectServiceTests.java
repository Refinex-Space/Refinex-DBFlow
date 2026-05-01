package com.refinex.dbflow.executor;


import com.refinex.dbflow.access.entity.DbfApiToken;
import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.service.AccessDecisionService;
import com.refinex.dbflow.access.service.AccessService;
import com.refinex.dbflow.access.service.ProjectEnvironmentCatalogService;
import com.refinex.dbflow.config.properties.DbflowProperties;
import com.refinex.dbflow.executor.datasource.ProjectEnvironmentDataSourceRegistry;
import com.refinex.dbflow.executor.dto.SchemaInspectRequest;
import com.refinex.dbflow.executor.dto.SchemaInspectResult;
import com.refinex.dbflow.executor.service.SchemaInspectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * schema inspect 服务非容器边界测试。
 *
 * @author refinex
 */
@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:schema_inspect_service_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.datasource-defaults.driver-class-name=com.mysql.cj.jdbc.Driver",
        "dbflow.projects[0].key=demo",
        "dbflow.projects[0].name=Demo Project",
        "dbflow.projects[0].environments[0].key=dev",
        "dbflow.projects[0].environments[0].name=Development",
        "dbflow.projects[0].environments[0].jdbc-url=jdbc:mysql://127.0.0.1:3306/schema_dev"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableConfigurationProperties(DbflowProperties.class)
@Import({
        AccessService.class,
        AccessDecisionService.class,
        ProjectEnvironmentCatalogService.class,
        SchemaInspectService.class,
        SchemaInspectServiceTests.TestBeans.class
})
class SchemaInspectServiceTests {

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
     * 每个测试前创建元数据夹具。
     */
    @BeforeEach
    void setUp() {
        catalogService.syncConfiguredProjectEnvironments();
        user = accessService.createUser("schema-" + UUID.randomUUID(), "Schema User", "hash");
        token = accessService.issueActiveToken(
                user.getId(),
                "hash-" + UUID.randomUUID(),
                "dbf_schema",
                Instant.now().plus(1, ChronoUnit.DAYS)
        );
    }

    /**
     * 验证未授权环境会被拒绝，且不会访问目标数据源或返回连接配置。
     */
    @Test
    void shouldDenyUnauthorizedEnvironmentBeforeTargetAccessAndHideSecrets() {
        SchemaInspectResult result = schemaInspectService.inspect(request(null, null, 100));

        assertThat(result.status()).isEqualTo("DENIED");
        assertThat(result.allowed()).isFalse();
        assertThat(result.errorCode()).isEqualTo("GRANT_NOT_FOUND");
        assertThat(result.schemas()).isEmpty();
        assertThat(result.tables()).isEmpty();
        assertThat(result.columns()).isEmpty();
        assertThat(result.indexes()).isEmpty();
        assertThat(result.toString()).doesNotContain("jdbc:mysql", "schema_dev", "password");
    }

    /**
     * 创建 schema inspect 请求。
     *
     * @param schema   schema 过滤
     * @param table    表过滤
     * @param maxItems 最大返回条目数
     * @return schema inspect 请求
     */
    private SchemaInspectRequest request(String schema, String table, int maxItems) {
        return new SchemaInspectRequest(
                "req-" + UUID.randomUUID(),
                user.getId(),
                token.getId(),
                token.getTokenPrefix(),
                "demo",
                "dev",
                schema,
                table,
                maxItems
        );
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
            return new ProjectEnvironmentDataSourceRegistry() {
                @Override
                public DataSource getDataSource(String projectKey, String environmentKey) {
                    throw new AssertionError("拒绝路径不应访问目标数据源");
                }
            };
        }
    }
}
