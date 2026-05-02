package com.refinex.dbflow.mcp;

import com.refinex.dbflow.access.entity.DbfApiToken;
import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.service.AccessService;
import com.refinex.dbflow.access.service.ProjectEnvironmentCatalogService;
import com.refinex.dbflow.mcp.dto.DbflowMcpSkeletonResponse;
import com.refinex.dbflow.mcp.tool.DbflowMcpTools;
import com.refinex.dbflow.security.request.McpRequestMetadata;
import com.refinex.dbflow.security.token.McpAuthenticationToken;
import com.refinex.dbflow.security.token.McpTokenValidationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MCP 目标与策略投影测试。
 *
 * @author refinex
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mcp_target_policy_projection_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "dbflow.admin.initial-user.enabled=false",
        "dbflow.security.mcp-token.pepper=mcp-target-policy-test-pepper",
        "dbflow.datasource-defaults.validate-on-startup=false",
        "dbflow.datasource-defaults.driver-class-name=org.h2.Driver",
        "dbflow.datasource-defaults.username=sa",
        "dbflow.datasource-defaults.password=",
        "dbflow.datasource-defaults.hikari.minimum-idle=0",
        "dbflow.projects[0].key=demo",
        "dbflow.projects[0].name=Demo Project",
        "dbflow.projects[0].environments[0].key=dev",
        "dbflow.projects[0].environments[0].name=Development",
        "dbflow.projects[0].environments[0].jdbc-url=jdbc:mysql://127.0.0.1:3306/demo_dev?useSSL=false",
        "dbflow.projects[0].environments[0].driver-class-name=com.mysql.cj.jdbc.Driver",
        "dbflow.projects[0].environments[0].username=demo_user",
        "dbflow.projects[0].environments[0].password=",
        "dbflow.projects[0].environments[1].key=prod",
        "dbflow.projects[0].environments[1].name=Production",
        "dbflow.projects[0].environments[1].jdbc-url=jdbc:mysql://127.0.0.1:3306/demo_prod?useSSL=false",
        "dbflow.projects[0].environments[1].driver-class-name=com.mysql.cj.jdbc.Driver",
        "dbflow.projects[0].environments[1].username=prod_user",
        "dbflow.projects[0].environments[1].password=",
        "dbflow.policies.dangerous-ddl.whitelist[0].project-key=demo",
        "dbflow.policies.dangerous-ddl.whitelist[0].environment-key=dev",
        "dbflow.policies.dangerous-ddl.whitelist[0].schema-name=app",
        "dbflow.policies.dangerous-ddl.whitelist[0].table-name=orders",
        "dbflow.policies.dangerous-ddl.whitelist[0].operation=DROP_TABLE",
        "dbflow.policies.dangerous-ddl.whitelist[0].allow-prod-dangerous-ddl=true"
})
class DbflowMcpTargetPolicyProjectionTests {

    /**
     * DBFlow MCP tools。
     */
    @Autowired
    private DbflowMcpTools dbflowMcpTools;

    /**
     * 访问控制服务。
     */
    @Autowired
    private AccessService accessService;

    /**
     * 项目环境配置目录服务。
     */
    @Autowired
    private ProjectEnvironmentCatalogService catalogService;

    /**
     * 每个用例前同步配置目录。
     */
    @BeforeEach
    void setUpCatalog() {
        SecurityContextHolder.clearContext();
        catalogService.syncConfiguredProjectEnvironments();
    }

    /**
     * 每个用例后清理认证上下文。
     */
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 验证 list targets 返回当前 MCP 主体已授权的真实目标。
     */
    @Test
    void shouldListOnlyAuthorizedConfiguredTargets() {
        DbfUser user = createAuthenticatedUserWithGrant("demo", "dev");

        DbflowMcpSkeletonResponse response = dbflowMcpTools.listTargets();

        assertThat(response.authorization().allowed()).isTrue();
        List<Map<String, Object>> targets = mapList(response.data().get("targets"));
        assertThat(targets).hasSize(1);
        assertThat(targets.getFirst())
                .containsEntry("project", "demo")
                .containsEntry("projectName", "Demo Project")
                .containsEntry("env", "dev")
                .containsEntry("environmentName", "Development")
                .containsEntry("databaseName", "demo_dev")
                .containsEntry("username", "demo_user")
                .containsEntry("metadataPresent", true);
        assertThat(targets).extracting(target -> target.get("env")).doesNotContain("prod");
        assertThat(user.getId()).isNotNull();
    }

    /**
     * 验证 policy tool 返回真实配置中的默认策略和匹配白名单。
     */
    @Test
    void shouldReturnConfiguredDangerousDdlPolicyForAuthorizedTarget() {
        createAuthenticatedUserWithGrant("demo", "dev");

        DbflowMcpSkeletonResponse response = dbflowMcpTools.getEffectivePolicy(
                "demo",
                "dev",
                "app",
                "orders",
                "DROP_TABLE"
        );

        assertThat(response.authorization().allowed()).isTrue();
        assertThat(response.data())
                .containsEntry("status", "AUTHORIZED")
                .containsEntry("dangerousDdlOperation", "DROP_TABLE")
                .containsEntry("effectiveDefaultDecision", "DENY");
        assertThat(map(response.data().get("defaults")))
                .containsEntry("DROP_TABLE", "DENY")
                .containsEntry("DROP_DATABASE", "DENY")
                .containsEntry("TRUNCATE", "REQUIRE_CONFIRMATION");

        List<Map<String, Object>> whitelist = mapList(response.data().get("whitelist"));
        assertThat(whitelist).hasSize(1);
        assertThat(whitelist.getFirst())
                .containsEntry("operation", "DROP_TABLE")
                .containsEntry("project", "demo")
                .containsEntry("env", "dev")
                .containsEntry("schema", "app")
                .containsEntry("table", "orders")
                .containsEntry("allowProdDangerousDdl", true);
    }

    /**
     * 验证未授权目标不会泄露策略配置。
     */
    @Test
    void shouldHidePolicyDetailsForUnauthorizedTarget() {
        createAuthenticatedUserWithGrant("demo", "dev");

        DbflowMcpSkeletonResponse response = dbflowMcpTools.getEffectivePolicy(
                "demo",
                "prod",
                "app",
                "orders",
                "DROP_TABLE"
        );

        assertThat(response.authorization().allowed()).isFalse();
        assertThat(response.data())
                .containsEntry("status", "DENIED")
                .containsEntry("defaults", Map.of())
                .containsEntry("whitelist", List.of());
    }

    /**
     * 创建已认证且拥有指定环境授权的测试用户。
     *
     * @param project 项目标识
     * @param env     环境标识
     * @return 测试用户
     */
    private DbfUser createAuthenticatedUserWithGrant(String project, String env) {
        String suffix = UUID.randomUUID().toString();
        DbfUser user = accessService.createUser("mcp-projection-" + suffix, "MCP Projection", "password-hash");
        DbfApiToken token = accessService.issueActiveToken(
                user.getId(),
                "hash-" + suffix,
                "dbf_" + suffix.substring(0, 8),
                Instant.now().plus(1, ChronoUnit.DAYS)
        );
        accessService.grantEnvironment(user.getId(), project, env, "WRITE");
        SecurityContextHolder.getContext().setAuthentication(McpAuthenticationToken.authenticated(
                new McpTokenValidationResult(token.getId(), user.getId(), token.getTokenPrefix(), null),
                new McpRequestMetadata("mcp-projection-test", "JUnit", "127.0.0.1", "request-" + suffix)
        ));
        return user;
    }

    /**
     * 转换对象为 Map。
     *
     * @param value 原始对象
     * @return Map 对象
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    /**
     * 转换对象为 Map 列表。
     *
     * @param value 原始对象
     * @return Map 列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mapList(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
