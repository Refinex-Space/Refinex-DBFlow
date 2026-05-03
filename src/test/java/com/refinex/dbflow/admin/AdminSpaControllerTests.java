package com.refinex.dbflow.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * React 管理端 SPA 路由控制器测试。
 *
 * @author refinex
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_spa_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.admin.initial-user.enabled=false"
})
@AutoConfigureMockMvc
class AdminSpaControllerTests {

    /**
     * MockMvc 测试客户端。
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证 React 管理端根路径转发到 SPA 入口。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldForwardAdminRootToSpaIndex() throws Exception {
        mockMvc.perform(get("/admin").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/admin/index.html"));
    }

    /**
     * 验证 React 管理端子路由转发到 SPA 入口。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldForwardAdminRouteToSpaIndex() throws Exception {
        mockMvc.perform(get("/admin/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/admin/index.html"));
    }

    /**
     * 验证资源路径按静态资源处理，不回退到 SPA 入口。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldServeAdminStaticResourceWithoutSpaFallback() throws Exception {
        mockMvc.perform(get("/admin/assets/admin-spa-test.js"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl(null))
                .andExpect(content().string(containsString("admin-spa-test")));
    }

    /**
     * 验证管理端 API 仍然返回 JSON，不被 SPA fallback 捕获。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldKeepAdminApiAsJsonApi() throws Exception {
        mockMvc.perform(get("/admin/api/session")
                        .with(user("admin").roles("ADMIN"))
                        .accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl(null))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"authenticated\":true")));
    }

    /**
     * 验证 React 登录页入口由 SPA index 渲染。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldServeReactLoginPageFromSpaIndex() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl(null))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("DBFlow Admin")))
                .andExpect(content().string(containsString("/admin/assets/")));
    }

    /**
     * 验证旧后台入口已经不可用。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldNotExposeAdminLegacyRoute() throws Exception {
        mockMvc.perform(get("/admin-legacy").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }
}
