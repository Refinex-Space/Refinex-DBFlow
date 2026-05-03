package com.refinex.dbflow.admin;

import com.refinex.dbflow.access.entity.DbfApiToken;
import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.service.AccessService;
import com.refinex.dbflow.audit.dto.AuditEventWriteRequest;
import com.refinex.dbflow.audit.dto.AuditRequestContext;
import com.refinex.dbflow.audit.entity.DbfAuditEvent;
import com.refinex.dbflow.audit.service.AuditEventWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理端审计、危险策略和系统健康页面测试。
 *
 * @author refinex
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_operations_pages_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.admin.initial-user.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "dbflow.datasource-defaults.driver-class-name=org.h2.Driver",
        "dbflow.datasource-defaults.username=sa",
        "dbflow.datasource-defaults.hikari.pool-name-prefix=admin-ops-target",
        "dbflow.datasource-defaults.hikari.maximum-pool-size=2",
        "dbflow.projects[0].key=ops-admin",
        "dbflow.projects[0].name=Ops Admin",
        "dbflow.projects[0].environments[0].key=prod",
        "dbflow.projects[0].environments[0].name=Production",
        "dbflow.projects[0].environments[0].jdbc-url=jdbc:h2:mem:admin_operations_target;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "dbflow.projects[0].environments[0].driver-class-name=org.h2.Driver",
        "dbflow.projects[0].environments[0].username=sa",
        "dbflow.policies.dangerous-ddl.whitelist[0].project-key=ops-admin",
        "dbflow.policies.dangerous-ddl.whitelist[0].environment-key=prod",
        "dbflow.policies.dangerous-ddl.whitelist[0].schema-name=ops",
        "dbflow.policies.dangerous-ddl.whitelist[0].table-name=legacy_jobs",
        "dbflow.policies.dangerous-ddl.whitelist[0].operation=DROP_TABLE",
        "dbflow.policies.dangerous-ddl.whitelist[0].allow-prod-dangerous-ddl=false"
})
@AutoConfigureMockMvc
class AdminOperationsPageControllerTests {

    /**
     * MockMvc 测试客户端。
     */
    @Autowired
    private MockMvc mockMvc;

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
     * 当前测试项目标识。
     */
    private String projectKey;

    /**
     * 当前测试用户。
     */
    private DbfUser testUser;

    /**
     * 当前测试 Token。
     */
    private DbfApiToken testToken;

    /**
     * 被拒绝审计事件。
     */
    private DbfAuditEvent deniedEvent;

    /**
     * 创建页面测试夹具。
     */
    @BeforeEach
    void setUp() {
        testUser = accessService.createUser("admin-page-" + UUID.randomUUID(), "Admin Page", "hash");
        testToken = accessService.issueActiveToken(
                testUser.getId(),
                "hash-" + UUID.randomUUID(),
                "page_token",
                Instant.now().plus(1, ChronoUnit.DAYS)
        );
        projectKey = "ops-admin-" + UUID.randomUUID();
        auditEventWriter.executed(new AuditEventWriteRequest(
                "req-executed-" + UUID.randomUUID(),
                testUser.getId(),
                testToken.getId(),
                testToken.getTokenPrefix(),
                new AuditRequestContext("Codex", "1.0.0", "Codex/Test", "127.0.0.1", "dbflow_execute_sql"),
                projectKey,
                "prod",
                "SELECT",
                "LOW",
                "SELECT id FROM account",
                "hash-executed-" + projectKey,
                "返回 1 行",
                1L,
                null,
                null,
                null
        ));
        deniedEvent = auditEventWriter.policyDenied(new AuditEventWriteRequest(
                "req-denied-" + UUID.randomUUID(),
                testUser.getId(),
                testToken.getId(),
                testToken.getTokenPrefix(),
                new AuditRequestContext("Codex", "1.0.0", "Codex/Test", "127.0.0.1", "dbflow_execute_sql"),
                projectKey,
                "prod",
                "DROP_TABLE",
                "CRITICAL",
                "DROP TABLE account WHERE password='plain-db-password'",
                "hash-denied-" + projectKey,
                "reason=DROP_TABLE_NOT_WHITELISTED password=plain-db-password",
                0L,
                "DROP_TABLE_NOT_WHITELISTED",
                "password=plain-db-password",
                null
        ));
    }

    /**
     * 验证审计列表支持分页和项目过滤。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRenderAuditListWithPagination() throws Exception {
        mockMvc.perform(get("/admin-legacy/audit")
                        .with(user("admin").roles("ADMIN"))
                        .param("project", projectKey)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("审计列表")))
                .andExpect(content().string(containsString("共 2 条")))
                .andExpect(content().string(containsString("第 1/2 页")))
                .andExpect(content().string(containsString("下一页")));
    }

    /**
     * 验证审计列表支持决策、项目和工具过滤。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldFilterAuditListAndExposeDeniedReasonLink() throws Exception {
        mockMvc.perform(get("/admin-legacy/audit")
                        .with(user("admin").roles("ADMIN"))
                        .param("project", projectKey)
                        .param("decision", "POLICY_DENIED")
                        .param("tool", "dbflow_execute_sql"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("hash-denied-" + projectKey)))
                .andExpect(content().string(containsString("查看拒绝原因")))
                .andExpect(content().string(not(containsString("hash-executed-" + projectKey))));
    }

    /**
     * 验证审计详情页面脱敏敏感信息并展示拒绝原因。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRenderSanitizedAuditDetailWithDeniedReason() throws Exception {
        mockMvc.perform(get("/admin-legacy/audit/{id}", deniedEvent.getId())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("审计详情")))
                .andExpect(content().string(containsString("DROP_TABLE_NOT_WHITELISTED")))
                .andExpect(content().string(containsString("[REDACTED]")))
                .andExpect(content().string(not(containsString("plain-db-password"))))
                .andExpect(content().string(not(containsString(testToken.getTokenPrefix()))));
    }

    /**
     * 验证危险策略页面只读展示默认策略、白名单和生产强化规则。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRenderReadOnlyDangerousPolicyPage() throws Exception {
        mockMvc.perform(get("/admin-legacy/policies/dangerous")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("默认高危策略")))
                .andExpect(content().string(containsString("DROP_TABLE")))
                .andExpect(content().string(containsString("legacy_jobs")))
                .andExpect(content().string(containsString("TRUNCATE confirmation")))
                .andExpect(content().string(containsString("prod 强化")));
    }

    /**
     * 验证系统健康页面展示关键组件且不暴露目标连接串。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRenderSystemHealthPageWithoutSecrets() throws Exception {
        mockMvc.perform(get("/admin-legacy/health")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("元数据库")))
                .andExpect(content().string(containsString("MCP Streamable HTTP")))
                .andExpect(content().string(containsString("Nacos")))
                .andExpect(content().string(containsString("ops-admin / prod")))
                .andExpect(content().string(containsString("admin-ops-target-ops-admin-prod")))
                .andExpect(content().string(not(containsString("jdbc:h2:mem:admin_operations_target"))))
                .andExpect(content().string(not(containsString("password"))));
    }

    /**
     * 验证普通用户不能访问管理端运维页面。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRejectNonAdminOperationsPages() throws Exception {
        mockMvc.perform(get("/admin-legacy/audit").with(user("operator").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin-legacy/policies/dangerous").with(user("operator").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin-legacy/health").with(user("operator").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
