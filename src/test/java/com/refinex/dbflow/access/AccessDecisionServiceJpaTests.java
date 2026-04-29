package com.refinex.dbflow.access;

import com.refinex.dbflow.access.entity.DbfApiToken;
import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.entity.DbfUserEnvGrant;
import com.refinex.dbflow.access.service.*;
import com.refinex.dbflow.config.DbflowProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 项目环境访问判断服务 JPA 测试。
 *
 * @author refinex
 */
@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:access_decision_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.datasource-defaults.driver-class-name=com.mysql.cj.jdbc.Driver",
        "dbflow.datasource-defaults.username=dbflow_default",
        "dbflow.datasource-defaults.password=${DBFLOW_DEFAULT_PASSWORD:}",
        "dbflow.projects[0].key=demo",
        "dbflow.projects[0].name=Demo Project",
        "dbflow.projects[0].environments[0].key=dev",
        "dbflow.projects[0].environments[0].name=Development",
        "dbflow.projects[0].environments[0].jdbc-url=jdbc:mysql://127.0.0.1:3306/demo_dev",
        "dbflow.projects[0].environments[0].username=demo_user",
        "dbflow.projects[0].environments[0].password=${DBFLOW_DEMO_DEV_PASSWORD:}"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableConfigurationProperties(DbflowProperties.class)
@Import({AccessService.class, AccessDecisionService.class, ProjectEnvironmentCatalogService.class})
class AccessDecisionServiceJpaTests {

    /**
     * 访问控制服务。
     */
    @Autowired
    private AccessService accessService;

    /**
     * 访问判断服务。
     */
    @Autowired
    private AccessDecisionService accessDecisionService;

    /**
     * 项目环境配置目录服务。
     */
    @Autowired
    private ProjectEnvironmentCatalogService catalogService;

    /**
     * 每个用例前同步配置项目环境展示模型。
     */
    @BeforeEach
    void setUpCatalog() {
        catalogService.syncConfiguredProjectEnvironments();
    }

    /**
     * 验证配置项目环境可同步到元数据库，并且展示视图不暴露密码。
     */
    @Test
    void shouldSyncConfiguredEnvironmentWithoutPasswordExposure() {
        List<ConfiguredEnvironmentView> environments = catalogService.listConfiguredEnvironments();

        assertThat(environments).hasSize(1);
        ConfiguredEnvironmentView environment = environments.getFirst();
        assertThat(environment.projectKey()).isEqualTo("demo");
        assertThat(environment.environmentKey()).isEqualTo("dev");
        assertThat(environment.metadataPresent()).isTrue();
        assertThat(environment.driverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
        assertThat(environment.username()).isEqualTo("demo_user");
        assertThat(environment.toString()).doesNotContain("DBFLOW_DEMO_DEV_PASSWORD");
    }

    /**
     * 验证可按项目和环境标识创建、查询和删除授权。
     */
    @Test
    void shouldCreateQueryAndDeleteGrantByProjectEnvironmentKey() {
        DbfUser user = createUser("grant-alice");

        DbfUserEnvGrant grant = accessService.grantEnvironment(user.getId(), "demo", "dev", "WRITE");

        assertThat(grant.getUserId()).isEqualTo(user.getId());
        assertThat(accessService.findGrant(user.getId(), "demo", "dev")).map(DbfUserEnvGrant::getId)
                .contains(grant.getId());
        assertThat(accessService.hasActiveGrant(user.getId(), "demo", "dev")).isTrue();
        assertThat(accessService.deleteGrant(user.getId(), "demo", "dev")).isTrue();
        assertThat(accessService.findGrant(user.getId(), "demo", "dev")).isEmpty();
    }

    /**
     * 验证已授权用户与 active Token 可以访问项目环境。
     */
    @Test
    void shouldAllowAuthorizedUserTokenAndEnvironment() {
        DbfUser user = createUser("decision-bob");
        DbfApiToken token = issueToken(user);
        accessService.grantEnvironment(user.getId(), "demo", "dev", "WRITE");

        AccessDecision decision = accessDecisionService.decide(request(user, token, "demo", "dev"));

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.reason()).isEqualTo(AccessDecisionReason.ALLOWED);
    }

    /**
     * 验证未授权用户会被拒绝。
     */
    @Test
    void shouldDenyUserWithoutGrant() {
        DbfUser user = createUser("decision-carol");
        DbfApiToken token = issueToken(user);

        AccessDecision decision = accessDecisionService.decide(request(user, token, "demo", "dev"));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo(AccessDecisionReason.GRANT_NOT_FOUND);
    }

    /**
     * 验证环境不存在时会被拒绝。
     */
    @Test
    void shouldDenyMissingEnvironment() {
        DbfUser user = createUser("decision-dave");
        DbfApiToken token = issueToken(user);

        AccessDecision decision = accessDecisionService.decide(request(user, token, "demo", "missing"));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo(AccessDecisionReason.ENVIRONMENT_NOT_FOUND);
    }

    /**
     * 验证用户禁用后会被拒绝。
     */
    @Test
    void shouldDenyDisabledUser() {
        DbfUser user = createUser("decision-erin");
        DbfApiToken token = issueToken(user);
        accessService.grantEnvironment(user.getId(), "demo", "dev", "WRITE");
        accessService.disableUser(user.getId());

        AccessDecision decision = accessDecisionService.decide(request(user, token, "demo", "dev"));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo(AccessDecisionReason.USER_DISABLED);
    }

    /**
     * 验证 Token 禁用后会被拒绝。
     */
    @Test
    void shouldDenyDisabledToken() {
        DbfUser user = createUser("decision-frank");
        DbfApiToken token = issueToken(user);
        accessService.grantEnvironment(user.getId(), "demo", "dev", "WRITE");
        accessService.revokeToken(token.getId(), Instant.now());

        AccessDecision decision = accessDecisionService.decide(request(user, token, "demo", "dev"));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo(AccessDecisionReason.TOKEN_DISABLED);
    }

    /**
     * 验证 Token 所属用户不匹配时会被拒绝。
     */
    @Test
    void shouldDenyTokenUserMismatch() {
        DbfUser user = createUser("decision-gina");
        DbfUser otherUser = createUser("decision-hank");
        DbfApiToken token = issueToken(otherUser);

        AccessDecision decision = accessDecisionService.decide(request(user, token, "demo", "dev"));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo(AccessDecisionReason.TOKEN_USER_MISMATCH);
    }

    /**
     * 验证 Token 过期时会被拒绝。
     */
    @Test
    void shouldDenyExpiredToken() {
        DbfUser user = createUser("decision-ivy");
        DbfApiToken token = accessService.issueActiveToken(
                user.getId(),
                "hash-expired",
                "dbf_expired",
                Instant.now().minus(1, ChronoUnit.HOURS)
        );

        AccessDecision decision = accessDecisionService.decide(request(user, token, "demo", "dev"));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo(AccessDecisionReason.TOKEN_EXPIRED);
    }

    /**
     * 创建测试用户。
     *
     * @param username 用户名
     * @return 用户实体
     */
    private DbfUser createUser(String username) {
        return accessService.createUser(username, username, "password-hash");
    }

    /**
     * 颁发测试 Token。
     *
     * @param user 用户实体
     * @return Token 元数据
     */
    private DbfApiToken issueToken(DbfUser user) {
        return accessService.issueActiveToken(
                user.getId(),
                "hash-" + user.getUsername(),
                "dbf_" + user.getUsername(),
                Instant.now().plus(1, ChronoUnit.DAYS)
        );
    }

    /**
     * 创建访问判断请求。
     *
     * @param user           用户实体
     * @param token          Token 元数据
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @return 访问判断请求
     */
    private AccessDecisionRequest request(
            DbfUser user,
            DbfApiToken token,
            String projectKey,
            String environmentKey
    ) {
        return new AccessDecisionRequest(user.getId(), token.getId(), projectKey, environmentKey);
    }
}
