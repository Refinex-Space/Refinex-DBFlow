package com.refinex.dbflow.audit;

import com.refinex.dbflow.access.entity.DbfEnvironment;
import com.refinex.dbflow.access.entity.DbfProject;
import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.service.AccessService;
import com.refinex.dbflow.audit.entity.DbfAuditEvent;
import com.refinex.dbflow.audit.entity.DbfConfirmationChallenge;
import com.refinex.dbflow.audit.service.AuditService;
import com.refinex.dbflow.audit.service.ConfirmationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 审计与确认元数据服务 JPA 测试。
 *
 * @author refinex
 */
@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:audit_service_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AccessService.class, AuditService.class, ConfirmationService.class})
class AuditAndConfirmationServiceJpaTests {

    /**
     * 访问控制服务，用于准备用户和环境元数据。
     */
    @Autowired
    private AccessService accessService;

    /**
     * 审计服务。
     */
    @Autowired
    private AuditService auditService;

    /**
     * 确认服务。
     */
    @Autowired
    private ConfirmationService confirmationService;

    /**
     * 验证确认挑战可以从 PENDING 转为 CONFIRMED。
     */
    @Test
    void shouldConfirmPendingChallenge() {
        DbfUser user = accessService.createUser("dave", "Dave", "hash");
        DbfEnvironment environment = createEnvironment("project-confirm", "prod");
        Instant expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES);

        confirmationService.createPending(
                user.getId(),
                environment.getId(),
                "confirm-1",
                "sql-hash-1",
                "TRUNCATE TABLE orders",
                "HIGH",
                expiresAt
        );
        DbfConfirmationChallenge confirmed = confirmationService.confirm("confirm-1", Instant.now());

        assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");
        assertThat(confirmed.getConfirmedAt()).isNotNull();
        assertThat(confirmationService.findPendingByUser(user.getId())).isEmpty();
    }

    /**
     * 验证审计服务可以插入并按用户查询审计事件。
     */
    @Test
    void shouldInsertAuditEvent() {
        DbfUser user = accessService.createUser("erin", "Erin", "hash");

        DbfAuditEvent event = DbfAuditEvent.executed(
                "req-1",
                user.getId(),
                "project-a",
                "dev",
                "SELECT",
                "LOW",
                "ALLOWED_EXECUTED",
                "sql-hash-2",
                "SELECT 1",
                "1 row",
                0L
        );
        DbfAuditEvent saved = auditService.record(event);
        List<DbfAuditEvent> recent = auditService.findRecentByUser(user.getId());

        assertThat(saved.getId()).isNotNull();
        assertThat(recent).extracting(DbfAuditEvent::getRequestId).contains("req-1");
    }

    /**
     * 创建测试环境。
     *
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @return 项目环境实体
     */
    private DbfEnvironment createEnvironment(String projectKey, String environmentKey) {
        DbfProject project = accessService.createProject(projectKey, projectKey, "测试项目");
        return accessService.createEnvironment(project.getId(), environmentKey, environmentKey);
    }
}
