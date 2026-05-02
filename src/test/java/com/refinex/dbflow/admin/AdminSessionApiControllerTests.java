package com.refinex.dbflow.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * React 管理端当前 session API 测试。
 *
 * @author refinex
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_session_api_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.admin.initial-user.enabled=false"
})
@AutoConfigureMockMvc
class AdminSessionApiControllerTests {

    /**
     * MockMvc 测试客户端。
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证已登录管理员可以读取当前 session 与 shell 元数据。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldReturnCurrentAdminSession() throws Exception {
        mockMvc.perform(get("/admin/api/session")
                        .with(user("admin").roles("ADMIN"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.displayName").value("admin"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.shell.adminName").value("admin"))
                .andExpect(jsonPath("$.shell.mcpStatus").value("HEALTHY"))
                .andExpect(jsonPath("$.shell.mcpTone").value("ok"))
                .andExpect(jsonPath("$.shell.configSourceLabel").value("Local application config"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.shell.datasourcePassword").doesNotExist());
    }

    /**
     * 验证匿名 React 客户端可以通过 JSON 401 识别未登录状态。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldReturnJsonUnauthorizedForAnonymousSessionRequest() throws Exception {
        mockMvc.perform(get("/admin/api/session").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.authenticated").value(false));
    }
}
