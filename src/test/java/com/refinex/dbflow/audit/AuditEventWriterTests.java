package com.refinex.dbflow.audit;

import com.refinex.dbflow.access.entity.DbfApiToken;
import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.service.AccessService;
import com.refinex.dbflow.audit.dto.AuditEventWriteRequest;
import com.refinex.dbflow.audit.dto.AuditRequestContext;
import com.refinex.dbflow.audit.entity.DbfAuditEvent;
import com.refinex.dbflow.audit.service.AuditEventWriter;
import com.refinex.dbflow.audit.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统一审计事件写入器测试。
 *
 * @author refinex
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:audit_event_writer_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration"
})
@Import({AccessService.class, AuditService.class, AuditEventWriter.class})
class AuditEventWriterTests {

    /**
     * 访问服务。
     */
    @Autowired
    private AccessService accessService;

    /**
     * 审计服务。
     */
    @Autowired
    private AuditService auditService;

    /**
     * 统一审计事件写入器。
     */
    @Autowired
    private AuditEventWriter auditEventWriter;

    /**
     * 测试用户。
     */
    private DbfUser user;

    /**
     * 测试 Token。
     */
    private DbfApiToken token;

    /**
     * 创建用户和 Token 夹具。
     */
    @BeforeEach
    void setUp() {
        user = accessService.createUser("audit-writer-" + UUID.randomUUID(), "Audit Writer", "hash");
        token = accessService.issueActiveToken(
                user.getId(),
                "hash-" + UUID.randomUUID(),
                "dbf_audit",
                Instant.now().plus(1, ChronoUnit.DAYS)
        );
    }

    /**
     * 验证成功审计事件写入核心字段并裁剪结果摘要。
     */
    @Test
    void shouldWriteCoreFieldsAndBoundedSummaryWithoutTokenPlaintext() {
        String plaintextToken = "dbf_plaintext_token_should_not_be_saved";
        String longSummary = IntStream.range(0, 200)
                .mapToObj(index -> "row-" + index + "={id=" + index + ",name='value'}")
                .collect(Collectors.joining(","));

        DbfAuditEvent saved = auditEventWriter.executed(request(longSummary));

        assertThat(saved.getRequestId()).isEqualTo("req-audit");
        assertThat(saved.getUserId()).isEqualTo(user.getId());
        assertThat(saved.getTokenId()).isEqualTo(token.getId());
        assertThat(saved.getTokenPrefix()).isEqualTo(token.getTokenPrefix());
        assertThat(saved.getClientName()).isEqualTo("codex");
        assertThat(saved.getClientVersion()).isEqualTo("1.2.3");
        assertThat(saved.getUserAgent()).isEqualTo("Codex/Test");
        assertThat(saved.getSourceIp()).isEqualTo("10.0.0.8");
        assertThat(saved.getProjectKey()).isEqualTo("demo");
        assertThat(saved.getEnvironmentKey()).isEqualTo("dev");
        assertThat(saved.getTool()).isEqualTo("dbflow_execute_sql");
        assertThat(saved.getOperationType()).isEqualTo("SELECT");
        assertThat(saved.getRiskLevel()).isEqualTo("LOW");
        assertThat(saved.getDecision()).isEqualTo("EXECUTED");
        assertThat(saved.getStatus()).isEqualTo("ALLOWED_EXECUTED");
        assertThat(saved.getSqlHash()).isEqualTo("hash-1");
        assertThat(saved.getSqlText()).isEqualTo("SELECT * FROM orders");
        assertThat(saved.getResultSummary()).hasSizeLessThanOrEqualTo(512);
        assertThat(saved.getResultSummary()).doesNotContain("row-199", plaintextToken);
    }

    /**
     * 验证统一写入器覆盖关键审计决策。
     */
    @Test
    void shouldWriteNamedAuditDecisions() {
        auditEventWriter.requestReceived(request("received"));
        auditEventWriter.policyDenied(request("denied"));
        auditEventWriter.requiresConfirmation(request("requires confirmation"));
        auditEventWriter.failed(request("failed"));
        auditEventWriter.confirmationExpired(request("expired"));

        List<DbfAuditEvent> events = auditService.findRecentByUser(user.getId());

        assertThat(events)
                .extracting(DbfAuditEvent::getDecision)
                .contains(
                        "REQUEST_RECEIVED",
                        "POLICY_DENIED",
                        "REQUIRES_CONFIRMATION",
                        "FAILED",
                        "CONFIRMATION_EXPIRED"
                );
        assertThat(events)
                .allSatisfy(event -> {
                    assertThat(event.getRequestId()).isNotBlank();
                    assertThat(event.getUserId()).isNotNull();
                    assertThat(event.getTokenId()).isNotNull();
                    assertThat(event.getClientName()).isNotBlank();
                    assertThat(event.getClientVersion()).isNotBlank();
                    assertThat(event.getUserAgent()).isNotBlank();
                    assertThat(event.getSourceIp()).isNotBlank();
                    assertThat(event.getProjectKey()).isNotBlank();
                    assertThat(event.getEnvironmentKey()).isNotBlank();
                    assertThat(event.getTool()).isNotBlank();
                    assertThat(event.getOperationType()).isNotBlank();
                    assertThat(event.getRiskLevel()).isNotBlank();
                });
    }

    /**
     * 创建审计写入请求。
     *
     * @param summary 结果摘要
     * @return 审计写入请求
     */
    private AuditEventWriteRequest request(String summary) {
        return new AuditEventWriteRequest(
                "req-audit",
                user.getId(),
                token.getId(),
                token.getTokenPrefix(),
                new AuditRequestContext("codex", "1.2.3", "Codex/Test", "10.0.0.8", "dbflow_execute_sql"),
                "demo",
                "dev",
                "SELECT",
                "LOW",
                "SELECT * FROM orders",
                "hash-1",
                summary,
                12L,
                "ERR",
                "error message",
                "cnf_1"
        );
    }
}
