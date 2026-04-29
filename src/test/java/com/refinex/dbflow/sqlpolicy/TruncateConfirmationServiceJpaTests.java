package com.refinex.dbflow.sqlpolicy;

import com.refinex.dbflow.access.entity.DbfApiToken;
import com.refinex.dbflow.access.entity.DbfEnvironment;
import com.refinex.dbflow.access.entity.DbfProject;
import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.service.AccessService;
import com.refinex.dbflow.audit.entity.DbfAuditEvent;
import com.refinex.dbflow.audit.entity.DbfConfirmationChallenge;
import com.refinex.dbflow.audit.repository.DbfConfirmationChallengeRepository;
import com.refinex.dbflow.audit.service.AuditEventWriter;
import com.refinex.dbflow.audit.service.AuditService;
import com.refinex.dbflow.audit.service.ConfirmationService;
import com.refinex.dbflow.common.DbflowException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TRUNCATE 二次确认挑战生命周期 JPA 测试。
 *
 * @author refinex
 */
@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:truncate_confirmation_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AccessService.class, AuditService.class, AuditEventWriter.class, ConfirmationService.class,
        SqlClassifier.class, TruncateConfirmationService.class})
class TruncateConfirmationServiceJpaTests {

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
     * TRUNCATE 确认服务。
     */
    @Autowired
    private TruncateConfirmationService truncateConfirmationService;

    /**
     * 确认挑战 repository。
     */
    @Autowired
    private DbfConfirmationChallengeRepository confirmationChallengeRepository;

    /**
     * 验证 TRUNCATE 会创建服务端确认挑战并记录审计。
     */
    @Test
    void shouldCreatePendingChallengeForTruncate() {
        Fixture fixture = fixture("create");

        TruncateConfirmationDecision decision = truncateConfirmationService.createChallenge(request(
                "req-create",
                fixture,
                "TRUNCATE TABLE sales.orders",
                Instant.now()
        ));

        DbfConfirmationChallenge challenge = confirmationChallengeRepository
                .findByConfirmationId(decision.confirmationId())
                .orElseThrow();
        assertThat(decision.confirmationRequired()).isTrue();
        assertThat(decision.confirmed()).isFalse();
        assertThat(decision.sqlHash()).isNotBlank();
        assertThat(decision.riskLevel()).isEqualTo(SqlRiskLevel.CRITICAL);
        assertThat(challenge.getUserId()).isEqualTo(fixture.user().getId());
        assertThat(challenge.getTokenId()).isEqualTo(fixture.token().getId());
        assertThat(challenge.getProjectKey()).isEqualTo("project-create");
        assertThat(challenge.getEnvironmentKey()).isEqualTo("prod");
        assertThat(challenge.getSqlHash()).isEqualTo(decision.sqlHash());
        assertThat(challenge.getRiskLevel()).isEqualTo("CRITICAL");
        assertThat(challenge.getExpiresAt()).isAfter(Instant.now());
        assertAuditStatuses(fixture.user().getId(), "REQUIRES_CONFIRMATION");
    }

    /**
     * 验证同一 user、token、project/env 和 SQL hash 可以确认成功，并立即消费挑战。
     */
    @Test
    void shouldConfirmSameChallengeSuccessfullyAndConsumeIt() {
        Fixture fixture = fixture("success");
        String sql = "TRUNCATE TABLE sales.orders";
        Instant now = Instant.now();
        TruncateConfirmationDecision created = truncateConfirmationService.createChallenge(request("req-success-create", fixture, sql, now));

        TruncateConfirmationDecision confirmed = truncateConfirmationService.confirm(confirmRequest(
                "req-success-confirm",
                fixture,
                created.confirmationId(),
                sql,
                now.plus(1, ChronoUnit.MINUTES)
        ));

        DbfConfirmationChallenge challenge = confirmationChallengeRepository
                .findByConfirmationId(created.confirmationId())
                .orElseThrow();
        assertThat(confirmed.confirmed()).isTrue();
        assertThat(confirmed.status()).isEqualTo("CONFIRMED");
        assertThat(challenge.getStatus()).isEqualTo("CONFIRMED");
        assertThat(challenge.getConfirmedAt()).isNotNull();
        assertAuditStatuses(fixture.user().getId(), "CONFIRMATION_CONFIRMED", "REQUIRES_CONFIRMATION");
    }

    /**
     * 验证不同 token 不能确认挑战。
     */
    @Test
    void shouldRejectDifferentToken() {
        Fixture owner = fixture("token-owner");
        Fixture attacker = fixture("token-attacker");
        TruncateConfirmationDecision created = truncateConfirmationService.createChallenge(request(
                "req-token-create",
                owner,
                "TRUNCATE TABLE sales.orders",
                Instant.now()
        ));

        assertThatThrownBy(() -> truncateConfirmationService.confirm(confirmRequest(
                "req-token-confirm",
                new Fixture(owner.projectKey(), owner.user(), attacker.token(), owner.environment()),
                created.confirmationId(),
                "TRUNCATE TABLE sales.orders",
                Instant.now().plus(1, ChronoUnit.MINUTES)
        ))).isInstanceOf(DbflowException.class)
                .hasMessageContaining("确认挑战不匹配");
        assertAuditStatuses(owner.user().getId(), "DENIED", "REQUIRES_CONFIRMATION");
    }

    /**
     * 验证 SQL hash 不同不能确认挑战。
     */
    @Test
    void shouldRejectDifferentSqlHash() {
        Fixture fixture = fixture("sql");
        TruncateConfirmationDecision created = truncateConfirmationService.createChallenge(request(
                "req-sql-create",
                fixture,
                "TRUNCATE TABLE sales.orders",
                Instant.now()
        ));

        assertThatThrownBy(() -> truncateConfirmationService.confirm(confirmRequest(
                "req-sql-confirm",
                fixture,
                created.confirmationId(),
                "TRUNCATE TABLE sales.order_items",
                Instant.now().plus(1, ChronoUnit.MINUTES)
        ))).isInstanceOf(DbflowException.class)
                .hasMessageContaining("确认挑战不匹配");
        assertAuditStatuses(fixture.user().getId(), "DENIED", "REQUIRES_CONFIRMATION");
    }

    /**
     * 验证过期挑战不可用并会标记为 EXPIRED。
     */
    @Test
    void shouldRejectExpiredChallenge() {
        Fixture fixture = fixture("expired");
        Instant now = Instant.now();
        TruncateConfirmationDecision created = truncateConfirmationService.createChallenge(request(
                "req-expired-create",
                fixture,
                "TRUNCATE TABLE sales.orders",
                now
        ));

        assertThatThrownBy(() -> truncateConfirmationService.confirm(confirmRequest(
                "req-expired-confirm",
                fixture,
                created.confirmationId(),
                "TRUNCATE TABLE sales.orders",
                now.plus(10, ChronoUnit.MINUTES)
        ))).isInstanceOf(DbflowException.class)
                .hasMessageContaining("确认挑战已过期");
        DbfConfirmationChallenge challenge = confirmationChallengeRepository
                .findByConfirmationId(created.confirmationId())
                .orElseThrow();
        assertThat(challenge.getStatus()).isEqualTo("EXPIRED");
        assertAuditStatuses(fixture.user().getId(), "CONFIRMATION_EXPIRED", "REQUIRES_CONFIRMATION");
    }

    /**
     * 验证确认挑战不能重复使用。
     */
    @Test
    void shouldRejectChallengeReuse() {
        Fixture fixture = fixture("reuse");
        String sql = "TRUNCATE TABLE sales.orders";
        Instant now = Instant.now();
        TruncateConfirmationDecision created = truncateConfirmationService.createChallenge(request("req-reuse-create", fixture, sql, now));
        truncateConfirmationService.confirm(confirmRequest(
                "req-reuse-confirm-1",
                fixture,
                created.confirmationId(),
                sql,
                now.plus(1, ChronoUnit.MINUTES)
        ));

        assertThatThrownBy(() -> truncateConfirmationService.confirm(confirmRequest(
                "req-reuse-confirm-2",
                fixture,
                created.confirmationId(),
                sql,
                now.plus(2, ChronoUnit.MINUTES)
        ))).isInstanceOf(DbflowException.class)
                .hasMessageContaining("确认挑战不是待处理状态");
        assertAuditStatuses(fixture.user().getId(), "DENIED", "CONFIRMATION_CONFIRMED", "REQUIRES_CONFIRMATION");
    }

    /**
     * 创建测试请求。
     *
     * @param requestId 请求标识
     * @param fixture   测试夹具
     * @param sql       SQL 原文
     * @param now       当前时间
     * @return TRUNCATE 确认创建请求
     */
    private TruncateConfirmationRequest request(String requestId, Fixture fixture, String sql, Instant now) {
        return new TruncateConfirmationRequest(
                requestId,
                fixture.user().getId(),
                fixture.token().getId(),
                fixture.token().getTokenPrefix(),
                fixture.projectKey(),
                fixture.environment().getEnvironmentKey(),
                sql,
                now
        );
    }

    /**
     * 创建测试确认请求。
     *
     * @param requestId      请求标识
     * @param fixture        测试夹具
     * @param confirmationId 确认挑战标识
     * @param sql            SQL 原文
     * @param now            当前时间
     * @return TRUNCATE 确认请求
     */
    private TruncateConfirmationConfirmRequest confirmRequest(
            String requestId,
            Fixture fixture,
            String confirmationId,
            String sql,
            Instant now) {
        return new TruncateConfirmationConfirmRequest(
                requestId,
                fixture.user().getId(),
                fixture.token().getId(),
                fixture.token().getTokenPrefix(),
                fixture.projectKey(),
                fixture.environment().getEnvironmentKey(),
                confirmationId,
                sql,
                now
        );
    }

    /**
     * 创建用户、Token 和项目环境夹具。
     *
     * @param suffix 名称后缀
     * @return 测试夹具
     */
    private Fixture fixture(String suffix) {
        DbfUser user = accessService.createUser("truncate-" + suffix + "-" + UUID.randomUUID(), "Truncate " + suffix, "hash");
        DbfApiToken token = accessService.issueActiveToken(
                user.getId(),
                "hash-" + suffix + "-" + UUID.randomUUID(),
                "dbf_" + suffix,
                Instant.now().plus(1, ChronoUnit.DAYS)
        );
        String projectKey = "project-" + suffix;
        DbfProject project = accessService.createProject(projectKey, projectKey, "测试项目");
        DbfEnvironment environment = accessService.createEnvironment(project.getId(), "prod", "prod");
        return new Fixture(projectKey, user, token, environment);
    }

    /**
     * 断言用户审计事件包含指定状态。
     *
     * @param userId   用户主键
     * @param statuses 预期状态
     */
    private void assertAuditStatuses(Long userId, String... statuses) {
        List<DbfAuditEvent> events = auditService.findRecentByUser(userId);
        assertThat(events).extracting(DbfAuditEvent::getStatus).contains(statuses);
    }

    /**
     * 测试夹具。
     *
     * @param user        用户
     * @param token       Token
     * @param environment 环境
     */
    private record Fixture(String projectKey, DbfUser user, DbfApiToken token, DbfEnvironment environment) {
    }
}
