package com.refinex.dbflow.admin;

import com.refinex.dbflow.access.entity.DbfApiToken;
import com.refinex.dbflow.access.entity.DbfEnvironment;
import com.refinex.dbflow.access.entity.DbfProject;
import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.service.AccessService;
import com.refinex.dbflow.audit.service.AuditEventWriteRequest;
import com.refinex.dbflow.audit.service.AuditEventWriter;
import com.refinex.dbflow.audit.service.AuditRequestContext;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
 * 管理端总览页真实数据渲染测试。
 *
 * @author refinex
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_overview_real_data_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.admin.initial-user.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "dbflow.datasource-defaults.driver-class-name=org.h2.Driver",
        "dbflow.datasource-defaults.username=sa",
        "dbflow.projects[0].key=overview-real",
        "dbflow.projects[0].name=Overview Real",
        "dbflow.projects[0].environments[0].key=qa",
        "dbflow.projects[0].environments[0].name=QA",
        "dbflow.projects[0].environments[0].jdbc-url=jdbc:h2:mem:overview_real_target;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminOverviewRealDataControllerTests {

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
     * 验证无审计事件时展示真实空状态且不展示原型样例。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    @Order(1)
    void shouldRenderRealEmptyStateWithoutPrototypeRows() throws Exception {
        mockMvc.perform(get("/admin").with(user("actual-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("actual-admin")))
                .andExpect(content().string(containsString("当前暂无审计事件")))
                .andExpect(content().string(containsString("overview-real / qa")))
                .andExpect(content().string(not(containsString("状态样例"))))
                .andExpect(content().string(not(containsString("admin.refinex"))))
                .andExpect(content().string(not(containsString("billing-core"))))
                .andExpect(content().string(not(containsString("risk-lab"))))
                .andExpect(content().string(not(containsString("128"))));
    }

    /**
     * 验证总览页展示真实审计、Token、授权和 shell 数据。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    @Order(2)
    void shouldRenderRealOverviewDataAndShell() throws Exception {
        DbfUser user = accessService.createUser("overview-user-" + UUID.randomUUID(), "Overview User", "hash");
        DbfApiToken token = accessService.issueActiveToken(
                user.getId(),
                "hash-" + UUID.randomUUID(),
                "overview_token",
                Instant.now().plus(3, ChronoUnit.DAYS)
        );
        DbfProject project = accessService.createProject("overview-runtime-" + UUID.randomUUID(), "Overview Runtime", null);
        DbfEnvironment environment = accessService.createEnvironment(project.getId(), "prod", "Production");
        accessService.grantEnvironment(user.getId(), environment.getId(), "WRITE");
        String sqlHash = "hash-overview-" + UUID.randomUUID();
        auditEventWriter.executed(new AuditEventWriteRequest(
                "req-overview-" + UUID.randomUUID(),
                user.getId(),
                token.getId(),
                token.getTokenPrefix(),
                new AuditRequestContext("Codex", "1.0.0", "Codex/Test", "127.0.0.1", "dbflow_execute_sql"),
                project.getProjectKey(),
                "prod",
                "SELECT",
                "LOW",
                "SELECT id FROM overview_orders",
                sqlHash,
                "返回 1 行",
                1L,
                null,
                null,
                null
        ));

        mockMvc.perform(get("/admin").with(user("actual-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("actual-admin")))
                .andExpect(content().string(containsString(project.getProjectKey())))
                .andExpect(content().string(containsString(sqlHash)))
                .andExpect(content().string(containsString("1 个 7 天内过期")))
                .andExpect(content().string(containsString("Local application config")))
                .andExpect(content().string(not(containsString("状态样例"))))
                .andExpect(content().string(not(containsString("admin.refinex"))))
                .andExpect(content().string(not(containsString("billing-core"))))
                .andExpect(content().string(not(containsString("risk-lab"))));
    }

    /**
     * 验证普通用户不能访问管理端总览页。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRejectNonAdminOverviewRequest() throws Exception {
        mockMvc.perform(get("/admin").with(user("operator").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
