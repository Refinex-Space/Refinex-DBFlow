package com.refinex.dbflow.admin;

import com.refinex.dbflow.admin.command.CreateUserCommand;
import com.refinex.dbflow.admin.service.AdminAccessManagementService;
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
 * React 管理端用户管理 JSON API 测试。
 *
 * @author refinex
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_user_api_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.admin.initial-user.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false"
})
@AutoConfigureMockMvc
class AdminUserApiControllerTests {

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
     * 验证管理员可以按用户名和状态筛选用户列表，且列表不暴露密码哈希。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldListAndFilterUsersWithoutPasswordHash() throws Exception {
        String activeUsername = uniqueUsername("api-active");
        String disabledUsername = uniqueUsername("api-disabled");
        createFixtureUser(activeUsername, "API Active");
        UserRow disabledUser = createFixtureUser(disabledUsername, "API Disabled");
        accessManagementService.disableUser(disabledUser.id());

        mockMvc.perform(get("/admin/api/users")
                        .with(user("admin").roles("ADMIN"))
                        .param("username", "api-disabled")
                        .param("status", "DISABLED")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].username").value(disabledUsername))
                .andExpect(jsonPath("$.data[0].status").value("DISABLED"))
                .andExpect(content().string(not(containsString(activeUsername))))
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(content().string(not(containsString("password-hash"))));
    }

    /**
     * 验证管理员可以通过 JSON 创建用户，且响应不返回初始密码。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldCreateUserWithJsonAndCsrfWithoutReturningPassword() throws Exception {
        String username = uniqueUsername("api-create");
        String initialPassword = "Admin123456!";

        mockMvc.perform(post("/admin/api/users")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "displayName": "API Created",
                                  "password": "%s"
                                }
                                """.formatted(username, initialPassword)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(content().string(not(containsString(initialPassword))))
                .andExpect(content().string(not(containsString("passwordHash"))));
    }

    /**
     * 验证创建用户参数错误时返回 JSON 4xx。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldReturnJsonBadRequestForInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/admin/api/users")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "displayName": "Invalid",
                                  "password": "Admin123456!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    /**
     * 验证管理员可以通过 JSON 禁用和启用用户。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldDisableAndEnableUserWithJsonAndCsrf() throws Exception {
        String username = uniqueUsername("api-toggle");
        UserRow createdUser = createFixtureUser(username, "API Toggle");

        mockMvc.perform(post("/admin/api/users/{userId}/disable", createdUser.id())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(get("/admin/api/users")
                        .with(user("admin").roles("ADMIN"))
                        .param("username", username)
                        .param("status", "DISABLED")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("DISABLED"));

        mockMvc.perform(post("/admin/api/users/{userId}/enable", createdUser.id())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(get("/admin/api/users")
                        .with(user("admin").roles("ADMIN"))
                        .param("username", username)
                        .param("status", "ACTIVE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }

    /**
     * 验证重置密码接口不会返回新密码或哈希字段。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldResetPasswordWithoutReturningPassword() throws Exception {
        UserRow createdUser = createFixtureUser(uniqueUsername("api-reset"), "API Reset");
        String newPassword = "NewAdmin123456!";

        mockMvc.perform(post("/admin/api/users/{userId}/reset-password", createdUser.id())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newPassword": "%s"
                                }
                                """.formatted(newPassword)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(content().string(not(containsString(newPassword))))
                .andExpect(content().string(not(containsString("passwordHash"))));
    }

    /**
     * 验证管理端用户 API 拒绝匿名和非管理员调用。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRejectAnonymousAndNonAdminRequests() throws Exception {
        mockMvc.perform(get("/admin/api/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mockMvc.perform(get("/admin/api/users")
                        .with(user("operator").roles("USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    /**
     * 验证用户管理 API mutation 缺少 CSRF 时被安全链拒绝。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRejectMutationWithoutCsrf() throws Exception {
        mockMvc.perform(post("/admin/api/users")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "displayName": "Missing CSRF"
                                }
                                """.formatted(uniqueUsername("api-csrf"))))
                .andExpect(status().isForbidden());
    }

    /**
     * 创建测试用户。
     *
     * @param username    用户名
     * @param displayName 显示名
     * @return 用户行视图
     */
    private UserRow createFixtureUser(String username, String displayName) {
        return accessManagementService.createUser(
                new CreateUserCommand(username, displayName, "password-hash"));
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
