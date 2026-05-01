package com.refinex.dbflow.audit;

import com.refinex.dbflow.access.entity.DbfApiToken;
import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.service.AccessService;
import com.refinex.dbflow.audit.dto.*;
import com.refinex.dbflow.audit.entity.DbfAuditEvent;
import com.refinex.dbflow.audit.service.AuditEventWriter;
import com.refinex.dbflow.audit.service.AuditQueryService;
import com.refinex.dbflow.audit.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 管理端审计查询服务测试。
 *
 * @author refinex
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:audit_query_service_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration"
})
@Import({AccessService.class, AuditService.class, AuditEventWriter.class, AuditQueryService.class})
class AuditQueryServiceTests {

    /**
     * 访问服务。
     */
    @Autowired
    private AccessService accessService;

    /**
     * 审计事件写入器。
     */
    @Autowired
    private AuditEventWriter auditEventWriter;

    /**
     * 审计查询服务。
     */
    @Autowired
    private AuditQueryService auditQueryService;

    /**
     * JDBC 测试工具，用于固定审计创建时间。
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * JPA 测试实体管理器。
     */
    @Autowired
    private TestEntityManager entityManager;

    /**
     * 第一位测试用户。
     */
    private DbfUser firstUser;

    /**
     * 第一位测试用户 Token。
     */
    private DbfApiToken firstToken;

    /**
     * 第二位测试用户。
     */
    private DbfUser secondUser;

    /**
     * 第二位测试用户 Token。
     */
    private DbfApiToken secondToken;

    /**
     * 基准时间。
     */
    private Instant baseTime;

    /**
     * 创建审计查询测试夹具。
     */
    @BeforeEach
    void setUp() {
        baseTime = Instant.parse("2026-04-29T08:00:00Z");
        firstUser = accessService.createUser("audit-query-a-" + UUID.randomUUID(), "Audit Query A", "hash");
        firstToken = accessService.issueActiveToken(
                firstUser.getId(),
                "hash-a-" + UUID.randomUUID(),
                "aq_a",
                Instant.now().plus(1, ChronoUnit.DAYS)
        );
        secondUser = accessService.createUser("audit-query-b-" + UUID.randomUUID(), "Audit Query B", "hash");
        secondToken = accessService.issueActiveToken(
                secondUser.getId(),
                "hash-b-" + UUID.randomUUID(),
                "aq_b",
                Instant.now().plus(1, ChronoUnit.DAYS)
        );
        saveEvent(firstUser, firstToken, "demo", "dev", "LOW", "EXECUTED", "hash-select",
                "dbflow_execute_sql", "SELECT * FROM orders", "query ok", baseTime.plusSeconds(60));
        saveEvent(secondUser, secondToken, "ops", "prod", "CRITICAL", "POLICY_DENIED", "hash-drop",
                "dbflow_execute_sql", "DROP TABLE orders", "policy denied", baseTime.plusSeconds(120));
        saveEvent(firstUser, firstToken, "demo", "prod", "HIGH", "FAILED", "hash-update",
                "dbflow_explain_sql", "UPDATE orders SET status='DONE'", "failed", baseTime.plusSeconds(180));
        entityManager.clear();
    }

    /**
     * 验证审计查询支持组合过滤。
     */
    @Test
    void shouldFilterAuditEventsBySupportedFields() {
        AuditQueryCriteria criteria = AuditQueryCriteria.builder()
                .from(baseTime.plusSeconds(150))
                .to(baseTime.plusSeconds(240))
                .userId(firstUser.getId())
                .projectKey("demo")
                .environmentKey("prod")
                .riskLevel("HIGH")
                .decision("FAILED")
                .sqlHash("hash-update")
                .tool("dbflow_explain_sql")
                .page(0)
                .size(10)
                .sort("createdAt")
                .direction("desc")
                .build();

        AuditEventPageResponse<AuditEventSummary> page = auditQueryService.query(criteria);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.userId()).isEqualTo(firstUser.getId());
                    assertThat(summary.projectKey()).isEqualTo("demo");
                    assertThat(summary.environmentKey()).isEqualTo("prod");
                    assertThat(summary.riskLevel()).isEqualTo("HIGH");
                    assertThat(summary.decision()).isEqualTo("FAILED");
                    assertThat(summary.sqlHash()).isEqualTo("hash-update");
                    assertThat(summary.tool()).isEqualTo("dbflow_explain_sql");
                });
    }

    /**
     * 验证分页和排序边界稳定。
     */
    @Test
    void shouldPageAndSortAuditEvents() {
        AuditQueryCriteria firstPageCriteria = AuditQueryCriteria.builder()
                .page(0)
                .size(2)
                .sort("createdAt")
                .direction("desc")
                .build();
        AuditQueryCriteria secondPageCriteria = AuditQueryCriteria.builder()
                .page(1)
                .size(2)
                .sort("createdAt")
                .direction("desc")
                .build();

        AuditEventPageResponse<AuditEventSummary> firstPage = auditQueryService.query(firstPageCriteria);
        AuditEventPageResponse<AuditEventSummary> secondPage = auditQueryService.query(secondPageCriteria);

        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.content()).extracting(AuditEventSummary::sqlHash)
                .containsExactly("hash-update", "hash-drop");
        assertThat(secondPage.content()).extracting(AuditEventSummary::sqlHash)
                .containsExactly("hash-select");
    }

    /**
     * 验证详情查询不暴露敏感 Token 字段并脱敏密码文本。
     */
    @Test
    void shouldReturnSanitizedAuditDetail() {
        DbfAuditEvent sensitiveEvent = saveEvent(
                firstUser,
                firstToken,
                "demo",
                "dev",
                "CRITICAL",
                "EXECUTED",
                "hash-password",
                "dbflow_execute_sql",
                "ALTER USER 'root' IDENTIFIED BY 'plain-db-password'",
                "jdbc:mysql://localhost:3306/app?password=plain-db-password",
                baseTime.plusSeconds(300)
        );

        AuditEventDetail detail = auditQueryService.getDetail(sensitiveEvent.getId());

        assertThat(detail.id()).isEqualTo(sensitiveEvent.getId());
        assertThat(detail.sqlText()).doesNotContain("plain-db-password", firstToken.getTokenPrefix());
        assertThat(detail.resultSummary()).doesNotContain("plain-db-password", firstToken.getTokenPrefix());
        assertThat(detail.sqlText()).contains("[REDACTED]");
        assertThat(detail.resultSummary()).contains("[REDACTED]");
    }

    /**
     * 保存审计事件并固定创建时间。
     *
     * @param user           用户
     * @param token          Token
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @param riskLevel      风险级别
     * @param decision       审计决策
     * @param sqlHash        SQL hash
     * @param tool           工具名称
     * @param sqlText        SQL 原文
     * @param resultSummary  结果摘要
     * @param createdAt      创建时间
     * @return 已保存审计事件
     */
    private DbfAuditEvent saveEvent(
            DbfUser user,
            DbfApiToken token,
            String projectKey,
            String environmentKey,
            String riskLevel,
            String decision,
            String sqlHash,
            String tool,
            String sqlText,
            String resultSummary,
            Instant createdAt
    ) {
        DbfAuditEvent event = auditEventWriter.executed(new AuditEventWriteRequest(
                "req-" + UUID.randomUUID(),
                user.getId(),
                token.getId(),
                token.getTokenPrefix(),
                new AuditRequestContext("codex", "1.0.0", "Codex/Test", "127.0.0.1", tool),
                projectKey,
                environmentKey,
                "SELECT",
                riskLevel,
                sqlText,
                sqlHash,
                resultSummary,
                1L,
                null,
                null,
                null
        ));
        if (!"EXECUTED".equals(decision)) {
            jdbcTemplate.update("update dbf_audit_events set decision = ?, status = ? where id = ?",
                    decision, statusForDecision(decision), event.getId());
        }
        jdbcTemplate.update("update dbf_audit_events set created_at = ? where id = ?",
                Timestamp.from(createdAt), event.getId());
        return event;
    }

    /**
     * 将审计决策映射为兼容的状态值。
     *
     * @param decision 审计决策
     * @return 审计状态
     */
    private String statusForDecision(String decision) {
        return switch (decision) {
            case "POLICY_DENIED" -> "DENIED";
            case "FAILED" -> "FAILED";
            default -> "ALLOWED_EXECUTED";
        };
    }
}
