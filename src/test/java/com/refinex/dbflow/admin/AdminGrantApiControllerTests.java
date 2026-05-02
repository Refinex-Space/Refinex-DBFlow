package com.refinex.dbflow.admin;

import com.refinex.dbflow.admin.command.CreateUserCommand;
import com.refinex.dbflow.admin.command.GrantEnvironmentCommand;
import com.refinex.dbflow.admin.service.AdminAccessManagementService;
import com.refinex.dbflow.admin.view.GrantRow;
import com.refinex.dbflow.admin.view.UserRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * React 管理端项目环境授权 JSON API 测试。
 *
 * @author refinex
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_grant_api_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.admin.initial-user.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "dbflow.datasource-defaults.driver-class-name=org.h2.Driver",
        "dbflow.projects[0].key=billing-core",
        "dbflow.projects[0].name=Billing Core",
        "dbflow.projects[0].environments[0].key=staging",
        "dbflow.projects[0].environments[0].name=Staging",
        "dbflow.projects[0].environments[0].jdbc-url=jdbc:h2:mem:target_admin_grant_api_staging;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "dbflow.projects[0].environments[1].key=prod",
        "dbflow.projects[0].environments[1].name=Production",
        "dbflow.projects[0].environments[1].jdbc-url=jdbc:h2:mem:target_admin_grant_api_prod;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class AdminGrantApiControllerTests {

    /**
     * MockMvc 测试客户端。
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 管理端访问管理服务。
     */
    @Autowired
    private AdminAccessManagementService accessManagementService;

    /**
     * 验证授权选项返回 active 用户和环境选项，且不暴露连接信息。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldReturnOptionsWithoutConnectionInformation() throws Exception {
        String activeUsername = uniqueUsername("grant-options-active");
        String disabledUsername = uniqueUsername("grant-options-disabled");
        createFixtureUser(activeUsername);
        UserRow disabledUser = createFixtureUser(disabledUsername);
        accessManagementService.disableUser(disabledUser.id());

        mockMvc.perform(get("/admin/api/grants/options")
                        .with(user("admin").roles("ADMIN"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.users[?(@.username=='%s')].username".formatted(activeUsername))
                        .value(activeUsername))
                .andExpect(jsonPath("$.data.environments[?(@.environmentKey=='staging')].projectKey")
                        .value("billing-core"))
                .andExpect(jsonPath("$.data.environments[?(@.environmentKey=='prod')].environmentName")
                        .value("Production"))
                .andExpect(content().string(not(containsString(disabledUsername))))
                .andExpect(content().string(not(containsString("jdbc:h2"))))
                .andExpect(content().string(not(containsString("target_admin_grant_api"))));
    }

    /**
     * 验证管理员可以筛选授权分组列表，且列表不暴露数据库连接信息。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldListAndFilterGrantGroupsWithoutConnectionInformation() throws Exception {
        String username = uniqueUsername("grant-list");
        UserRow user = createFixtureUser(username);
        accessManagementService.grantEnvironment(new GrantEnvironmentCommand(
                user.id(),
                "billing-core",
                "staging",
                "WRITE"));

        mockMvc.perform(get("/admin/api/grants")
                        .with(user("admin").roles("ADMIN"))
                        .param("username", username)
                        .param("projectKey", "billing-core")
                        .param("environmentKey", "staging")
                        .param("status", "ACTIVE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].username").value(username))
                .andExpect(jsonPath("$.data[0].projectKey").value("billing-core"))
                .andExpect(jsonPath("$.data[0].environments[0].environmentKey").value("staging"))
                .andExpect(jsonPath("$.data[0].environments[0].grantType").value("WRITE"))
                .andExpect(content().string(not(containsString("jdbc:h2"))))
                .andExpect(content().string(not(containsString("target_admin_grant_api"))));
    }

    /**
     * 验证管理员可以通过 JSON 创建环境授权。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldCreateGrantWithJsonAndCsrf() throws Exception {
        UserRow user = createFixtureUser(uniqueUsername("grant-create"));

        mockMvc.perform(post("/admin/api/grants")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "projectKey": "billing-core",
                                  "environmentKey": "staging",
                                  "grantType": "READ"
                                }
                                """.formatted(user.id())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(user.id()))
                .andExpect(jsonPath("$.data.projectKey").value("billing-core"))
                .andExpect(jsonPath("$.data.environmentKey").value("staging"))
                .andExpect(jsonPath("$.data.grantType").value("READ"))
                .andExpect(content().string(not(containsString("jdbc:h2"))));
    }

    /**
     * 验证项目级批量更新支持空环境列表并撤销全部授权。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldUpdateProjectGrantsAndRevokeAllWhenEnvironmentKeysEmpty() throws Exception {
        String username = uniqueUsername("grant-update");
        UserRow user = createFixtureUser(username);

        mockMvc.perform(post("/admin/api/grants/update-project")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "projectKey": "billing-core",
                                  "environmentKeys": ["staging", "prod"],
                                  "grantType": "WRITE"
                                }
                                """.formatted(user.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/admin/api/grants")
                        .with(user("admin").roles("ADMIN"))
                        .param("username", username)
                        .param("projectKey", "billing-core")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].environments.length()").value(2));

        mockMvc.perform(post("/admin/api/grants/update-project")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "projectKey": "billing-core",
                                  "environmentKeys": [],
                                  "grantType": "WRITE"
                                }
                                """.formatted(user.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/admin/api/grants")
                        .with(user("admin").roles("ADMIN"))
                        .param("username", username)
                        .param("projectKey", "billing-core")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    /**
     * 验证管理员可以撤销单条授权。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRevokeGrantWithJsonAndCsrf() throws Exception {
        String username = uniqueUsername("grant-revoke");
        UserRow user = createFixtureUser(username);
        GrantRow grant = accessManagementService.grantEnvironment(new GrantEnvironmentCommand(
                user.id(),
                "billing-core",
                "staging",
                "WRITE"));

        mockMvc.perform(post("/admin/api/grants/{grantId}/revoke", grant.id())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.revoked").value(true));

        mockMvc.perform(get("/admin/api/grants")
                        .with(user("admin").roles("ADMIN"))
                        .param("username", username)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    /**
     * 验证授权 API mutation 缺少 CSRF 时被安全链拒绝。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRejectMutationWithoutCsrf() throws Exception {
        mockMvc.perform(post("/admin/api/grants")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "projectKey": "billing-core",
                                  "environmentKey": "staging",
                                  "grantType": "READ"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    /**
     * 创建测试用户。
     *
     * @param username 用户名
     * @return 用户行视图
     */
    private UserRow createFixtureUser(String username) {
        return accessManagementService.createUser(
                new CreateUserCommand(username, username, "Admin123456!"));
    }

    /**
     * 生成唯一用户名。
     *
     * @param prefix 用户名前缀
     * @return 唯一用户名
     */
    private String uniqueUsername(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
