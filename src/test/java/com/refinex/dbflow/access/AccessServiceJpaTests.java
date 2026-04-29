package com.refinex.dbflow.access;

import com.refinex.dbflow.access.entity.*;
import com.refinex.dbflow.access.service.AccessService;
import com.refinex.dbflow.common.DbflowException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 访问控制元数据服务 JPA 测试。
 *
 * @author refinex
 */
@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:access_service_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AccessService.class)
class AccessServiceJpaTests {

    /**
     * 访问控制服务。
     */
    @Autowired
    private AccessService accessService;

    /**
     * 验证一个用户只能存在一个 active token。
     */
    @Test
    void shouldRejectSecondActiveToken() {
        DbfUser user = accessService.createUser("alice", "Alice", "hash");
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.DAYS);

        DbfApiToken token = accessService.issueActiveToken(user.getId(), "hash-1", "dbf_1", expiresAt);

        assertThat(token.getStatus()).isEqualTo("ACTIVE");
        assertThat(accessService.findActiveToken(user.getId())).isPresent();
        assertThatThrownBy(() -> accessService.issueActiveToken(user.getId(), "hash-2", "dbf_2", expiresAt))
                .isInstanceOf(DbflowException.class);
    }

    /**
     * 验证吊销 token 后允许重新颁发 active token。
     */
    @Test
    void shouldAllowNewActiveTokenAfterRevocation() {
        DbfUser user = accessService.createUser("carol", "Carol", "hash");
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.DAYS);
        DbfApiToken token = accessService.issueActiveToken(user.getId(), "hash-3", "dbf_3", expiresAt);

        accessService.revokeToken(token.getId(), Instant.now());
        DbfApiToken newToken = accessService.issueActiveToken(user.getId(), "hash-4", "dbf_4", expiresAt);

        assertThat(newToken.getStatus()).isEqualTo("ACTIVE");
        assertThat(accessService.findActiveToken(user.getId())).map(DbfApiToken::getId).contains(newToken.getId());
    }

    /**
     * 验证授权关系查询边界。
     */
    @Test
    void shouldQueryActiveEnvironmentGrants() {
        DbfUser user = accessService.createUser("bob", "Bob", "hash");
        DbfProject project = accessService.createProject("project-a", "Project A", "测试项目");
        DbfEnvironment environment = accessService.createEnvironment(project.getId(), "dev", "开发环境");

        DbfUserEnvGrant grant = accessService.grantEnvironment(user.getId(), environment.getId(), "WRITE");
        List<DbfUserEnvGrant> activeGrants = accessService.findActiveGrants(user.getId());

        assertThat(activeGrants).extracting(DbfUserEnvGrant::getId).containsExactly(grant.getId());
        assertThat(accessService.hasActiveGrant(user.getId(), environment.getId())).isTrue();
        assertThatThrownBy(() -> accessService.grantEnvironment(user.getId(), environment.getId(), "WRITE"))
                .isInstanceOf(DbflowException.class);
    }
}
