package com.refinex.dbflow.observability;

import com.refinex.dbflow.access.entity.DbfApiToken;
import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.service.AccessService;
import com.refinex.dbflow.admin.service.AdminOperationsViewService;
import com.refinex.dbflow.admin.view.HealthItem;
import com.refinex.dbflow.admin.view.HealthPageView;
import com.refinex.dbflow.audit.dto.AuditEventWriteRequest;
import com.refinex.dbflow.audit.dto.AuditRequestContext;
import com.refinex.dbflow.audit.service.AuditEventWriter;
import com.refinex.dbflow.observability.dto.HealthSnapshot;
import com.refinex.dbflow.observability.service.DbflowHealthService;
import com.refinex.dbflow.observability.service.DbflowMetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DBFlow 运维健康检查和指标测试。
 *
 * @author refinex
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:operational_health_metrics_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.admin.initial-user.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "management.endpoint.health.show-details=never",
        "dbflow.datasource-defaults.driver-class-name=org.h2.Driver",
        "dbflow.datasource-defaults.username=sa",
        "dbflow.datasource-defaults.hikari.pool-name-prefix=ops-metrics-target",
        "dbflow.datasource-defaults.hikari.maximum-pool-size=2",
        "dbflow.projects[0].key=ops-metrics",
        "dbflow.projects[0].name=Ops Metrics",
        "dbflow.projects[0].environments[0].key=dev",
        "dbflow.projects[0].environments[0].name=Development",
        "dbflow.projects[0].environments[0].jdbc-url=jdbc:h2:mem:ops_metrics_target;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "dbflow.projects[0].environments[0].driver-class-name=org.h2.Driver",
        "dbflow.projects[0].environments[0].username=sa"
})
@AutoConfigureMockMvc
class OperationalHealthAndMetricsTests {

    /**
     * MockMvc 测试客户端。
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 健康指示器集合。
     */
    @Autowired
    private Map<String, HealthIndicator> healthIndicators;

    /**
     * DBFlow 运维健康服务。
     */
    @Autowired
    private DbflowHealthService healthService;

    /**
     * 管理端运维页面服务。
     */
    @Autowired
    private AdminOperationsViewService operationsViewService;

    /**
     * DBFlow 指标服务。
     */
    @Autowired
    private DbflowMetricsService metricsService;

    /**
     * Micrometer 指标注册表。
     */
    @Autowired
    private MeterRegistry meterRegistry;

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
     * 测试用户。
     */
    private DbfUser user;

    /**
     * 测试 Token。
     */
    private DbfApiToken token;

    /**
     * 创建审计夹具。
     */
    @BeforeEach
    void setUp() {
        user = accessService.createUser("ops-metrics-" + UUID.randomUUID(), "Ops Metrics", "hash");
        token = accessService.issueActiveToken(
                user.getId(),
                "hash-" + UUID.randomUUID(),
                "ops_metrics",
                Instant.now().plus(1, ChronoUnit.DAYS)
        );
    }

    /**
     * 验证自定义 health indicator 可用且返回非敏感详情。
     */
    @Test
    void shouldExposeCustomHealthIndicatorsWithoutSecrets() {
        assertThat(healthIndicators.get("dbflowMetadataDatabaseHealthIndicator").health().getStatus())
                .isEqualTo(Status.UP);
        assertThat(healthIndicators.get("dbflowMcpEndpointReadinessHealthIndicator").health().getStatus())
                .isEqualTo(Status.UP);
        assertThat(healthIndicators.get("dbflowNacosHealthIndicator").health().getStatus())
                .isEqualTo(Status.UP);
        assertThat(healthIndicators.get("dbflowTargetDatasourceRegistryHealthIndicator").health().getStatus())
                .isEqualTo(Status.UP);
        assertThat(healthIndicators.get("dbflowMetadataDatabaseHealthIndicator").health().getDetails().toString())
                .doesNotContain("jdbc:h2:mem", "password");
    }

    /**
     * 验证 Actuator 只开放健康和指标端点，健康详情默认隐藏。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldExposeMinimalActuatorEndpointsWithoutHealthDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(content().string(not(containsString("jdbc:h2:mem"))))
                .andExpect(content().string(not(containsString("password"))));

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("dbflow.confirmation.challenges")));

        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isForbidden());
    }

    /**
     * 验证管理端健康页复用共享健康服务。
     */
    @Test
    void shouldReuseSharedHealthServiceForAdminHealthPage() {
        HealthSnapshot snapshot = healthService.snapshot();
        HealthPageView page = operationsViewService.healthPage();

        assertThat(page.overall()).isEqualTo(snapshot.overall());
        assertThat(page.totalCount()).isEqualTo(snapshot.totalCount());
        assertThat(page.unhealthyCount()).isEqualTo(snapshot.unhealthyCount());
        assertThat(page.items())
                .extracting(HealthItem::name)
                .contains("元数据库", "MCP Streamable HTTP", "Nacos", "ops-metrics / dev");
    }

    /**
     * 验证 DBFlow 指标名称和核心标签可写入。
     */
    @Test
    void shouldRecordDbflowMetrics() {
        metricsService.recordMcpCall("dbflow_execute_sql");
        metricsService.recordSqlExecutionDuration("SELECT", "LOW", "ALLOWED_EXECUTED", System.nanoTime());
        auditEventWriter.policyDenied(new AuditEventWriteRequest(
                "req-metrics-" + UUID.randomUUID(),
                user.getId(),
                token.getId(),
                token.getTokenPrefix(),
                new AuditRequestContext("Codex", "1.0.0", "Codex/Test", "127.0.0.1", "dbflow_execute_sql"),
                "ops-metrics",
                "dev",
                "DROP_TABLE",
                "CRITICAL",
                "DROP TABLE account",
                "hash-metrics-" + UUID.randomUUID(),
                "SQL 策略拒绝",
                0L,
                "DENIED",
                "SQL 策略拒绝",
                null
        ));

        assertThat(meterRegistry.find("dbflow.mcp.calls")
                .tag("tool", "dbflow_execute_sql")
                .counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("dbflow.sql.risk")
                .tag("operation", "drop_table")
                .tag("risk", "critical")
                .tag("decision", "policy_denied")
                .counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("dbflow.sql.rejections")
                .tag("operation", "drop_table")
                .tag("decision", "policy_denied")
                .counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("dbflow.sql.execution.duration")
                .tag("operation", "select")
                .tag("risk", "low")
                .tag("status", "allowed_executed")
                .timer().count()).isEqualTo(1L);
        assertThat(meterRegistry.find("dbflow.confirmation.challenges")
                .tag("status", "PENDING")
                .gauge().value()).isZero();
    }

    /**
     * 验证管理端健康页面仍要求管理员权限。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldKeepAdminHealthPageAdminOnly() throws Exception {
        mockMvc.perform(get("/admin/health").with(user("operator").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/health").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("系统健康")))
                .andExpect(content().string(not(containsString("jdbc:h2:mem"))));
    }
}
