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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 管理端审计查询 API 测试。
 *
 * @author refinex
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_audit_query_api_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.admin.initial-user.enabled=false"
})
@AutoConfigureMockMvc
class AdminAuditEventControllerTests {

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
     * 测试审计事件。
     */
    private DbfAuditEvent auditEvent;

    /**
     * 测试项目标识。
     */
    private String projectKey;

    /**
     * 测试 Token 前缀。
     */
    private String tokenPrefix;

    /**
     * 创建控制器测试夹具。
     */
    @BeforeEach
    void setUp() {
        DbfUser user = accessService.createUser("audit-api-" + UUID.randomUUID(), "Audit API", "hash");
        DbfApiToken token = accessService.issueActiveToken(
                user.getId(),
                "hash-" + UUID.randomUUID(),
                "api_audit",
                Instant.now().plus(1, ChronoUnit.DAYS)
        );
        projectKey = "demo-api-" + UUID.randomUUID();
        tokenPrefix = token.getTokenPrefix();
        auditEvent = auditEventWriter.executed(new AuditEventWriteRequest(
                "req-api-" + UUID.randomUUID(),
                user.getId(),
                token.getId(),
                token.getTokenPrefix(),
                new AuditRequestContext("codex", "1.0.0", "Codex/Test", "127.0.0.1", "dbflow_execute_sql"),
                projectKey,
                "dev",
                "SELECT",
                "LOW",
                "SELECT * FROM accounts WHERE password='plain-db-password'",
                "hash-api",
                "password=plain-db-password",
                1L,
                null,
                null,
                null
        ));
    }

    /**
     * 验证管理员可以分页过滤查询审计事件。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldAllowAdminToQueryAuditEvents() throws Exception {
        mockMvc.perform(get("/admin/api/audit-events")
                        .with(user("admin").roles("ADMIN"))
                        .param("project", projectKey)
                        .param("decision", "EXECUTED")
                        .param("tool", "dbflow_execute_sql")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "createdAt")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(auditEvent.getId()))
                .andExpect(jsonPath("$.data.content[0].projectKey").value(projectKey))
                .andExpect(jsonPath("$.data.content[0].decision").value("EXECUTED"))
                .andExpect(jsonPath("$.data.content[0].tool").value("dbflow_execute_sql"));
    }

    /**
     * 验证管理员详情响应不暴露敏感 Token 或数据库密码。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldReturnSanitizedAuditDetailForAdmin() throws Exception {
        mockMvc.perform(get("/admin/api/audit-events/{id}", auditEvent.getId())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(auditEvent.getId()))
                .andExpect(jsonPath("$.data.tokenId").doesNotExist())
                .andExpect(jsonPath("$.data.tokenPrefix").doesNotExist())
                .andExpect(content().string(not(containsString(tokenPrefix))))
                .andExpect(content().string(not(containsString("plain-db-password"))))
                .andExpect(content().string(containsString("[REDACTED]")));
    }

    /**
     * 验证普通用户不能查询全量审计列表。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRejectNonAdminAuditListRequest() throws Exception {
        mockMvc.perform(get("/admin/api/audit-events")
                        .with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    /**
     * 验证普通用户不能查看审计详情。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRejectNonAdminAuditDetailRequest() throws Exception {
        mockMvc.perform(get("/admin/api/audit-events/{id}", auditEvent.getId())
                        .with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
