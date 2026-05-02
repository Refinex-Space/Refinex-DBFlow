package com.refinex.dbflow.admin;

import com.refinex.dbflow.access.entity.DbfApiToken;
import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.service.AccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
 * React 管理端运维只读 API 测试。
 *
 * @author refinex
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_operations_api_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.admin.initial-user.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "dbflow.datasource-defaults.driver-class-name=org.h2.Driver",
        "dbflow.datasource-defaults.username=sa",
        "dbflow.datasource-defaults.hikari.pool-name-prefix=admin-api-target",
        "dbflow.datasource-defaults.hikari.maximum-pool-size=2",
        "dbflow.projects[0].key=ops-api",
        "dbflow.projects[0].name=Ops API",
        "dbflow.projects[0].environments[0].key=prod",
        "dbflow.projects[0].environments[0].name=Production",
        "dbflow.projects[0].environments[0].jdbc-url=jdbc:h2:mem:admin_operations_api_target;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "dbflow.projects[0].environments[0].driver-class-name=org.h2.Driver",
        "dbflow.projects[0].environments[0].username=sa",
        "dbflow.policies.dangerous-ddl.whitelist[0].project-key=ops-api",
        "dbflow.policies.dangerous-ddl.whitelist[0].environment-key=prod",
        "dbflow.policies.dangerous-ddl.whitelist[0].schema-name=ops",
        "dbflow.policies.dangerous-ddl.whitelist[0].table-name=legacy_jobs",
        "dbflow.policies.dangerous-ddl.whitelist[0].operation=DROP_TABLE",
        "dbflow.policies.dangerous-ddl.whitelist[0].allow-prod-dangerous-ddl=false"
})
@AutoConfigureMockMvc
class AdminOperationsApiControllerTests {

    /**
     * 目标测试 JDBC URL，响应中不能直接出现完整值。
     */
    private static final String TARGET_JDBC_URL = "jdbc:h2:mem:admin_operations_api_target";

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
     * 测试 Token 前缀。
     */
    private String tokenPrefix;

    /**
     * 创建 API 测试夹具。
     */
    @BeforeEach
    void setUp() {
        DbfUser testUser = accessService.createUser("admin-api-" + UUID.randomUUID(), "Admin API", "hash");
        DbfApiToken testToken = accessService.issueActiveToken(
                testUser.getId(),
                "hash-" + UUID.randomUUID(),
                "api_token",
                Instant.now().plus(1, ChronoUnit.DAYS)
        );
        tokenPrefix = testToken.getTokenPrefix();
    }

    /**
     * 验证管理员可以读取总览 JSON。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldReturnOverviewJsonForAdmin() throws Exception {
        mockMvc.perform(get("/admin/api/overview")
                        .with(user("admin").roles("ADMIN"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.metrics").isArray())
                .andExpect(jsonPath("$.data.windowLabel").value("最近 24 小时网关安全、执行和健康摘要。"))
                .andExpect(content().string(not(containsString(tokenPrefix))))
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(content().string(not(containsString("tokenHash"))));
    }

    /**
     * 验证管理员可以读取脱敏配置 JSON。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldReturnConfigJsonWithoutSecretsForAdmin() throws Exception {
        mockMvc.perform(get("/admin/api/config")
                        .with(user("admin").roles("ADMIN"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rows[0].project").value("ops-api"))
                .andExpect(jsonPath("$.data.rows[0].env").value("prod"))
                .andExpect(jsonPath("$.data.rows[0].datasource").value("ops-api/prod"))
                .andExpect(content().string(not(containsString(TARGET_JDBC_URL))))
                .andExpect(content().string(not(containsString("plain-db-password"))))
                .andExpect(content().string(not(containsString(tokenPrefix))))
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(content().string(not(containsString("tokenHash"))));
    }

    /**
     * 验证管理员可以读取危险策略 JSON。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldReturnDangerousPolicyJsonWithoutSecretsForAdmin() throws Exception {
        mockMvc.perform(get("/admin/api/policies/dangerous")
                        .with(user("admin").roles("ADMIN"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.defaults").isArray())
                .andExpect(jsonPath("$.data.whitelist[0].project").value("ops-api"))
                .andExpect(jsonPath("$.data.whitelist[0].table").value("legacy_jobs"))
                .andExpect(content().string(not(containsString(tokenPrefix))))
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(content().string(not(containsString("tokenHash"))));
    }

    /**
     * 验证管理员可以读取系统健康 JSON，且不暴露完整目标连接串。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldReturnHealthJsonWithoutSecretsForAdmin() throws Exception {
        mockMvc.perform(get("/admin/api/health")
                        .with(user("admin").roles("ADMIN"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(content().string(containsString("ops-api / prod")))
                .andExpect(content().string(not(containsString(TARGET_JDBC_URL))))
                .andExpect(content().string(not(containsString("plain-db-password"))))
                .andExpect(content().string(not(containsString(tokenPrefix))))
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(content().string(not(containsString("tokenHash"))));
    }

    /**
     * 验证普通用户不能访问运维只读 API。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRejectNonAdminReadonlyApiRequests() throws Exception {
        mockMvc.perform(get("/admin/api/overview").with(user("operator").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/api/config").with(user("operator").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/api/policies/dangerous").with(user("operator").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/api/health").with(user("operator").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
