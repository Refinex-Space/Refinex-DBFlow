package com.refinex.dbflow.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 管理端 Thymeleaf 页面控制器测试。
 *
 * @author refinex
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_ui_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.admin.initial-user.enabled=false"
})
@AutoConfigureMockMvc
class AdminUiControllerTests {

    /**
     * MockMvc 测试客户端。
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证未登录访问后台页面会跳转到登录页。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRedirectAnonymousAdminPageToLogin() throws Exception {
        mockMvc.perform(get("/admin-legacy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    /**
     * 验证自定义登录页可访问并包含 Spring Security 登录表单。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRenderCustomLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Refinex-DBFlow Admin")))
                .andExpect(content().string(containsString("/admin-assets/img/dbflow-logo.svg")))
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(content().string(containsString("登录后台")));
    }

    /**
     * 验证后台静态资源无需登录即可访问。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldAllowAnonymousAdminStaticAssets() throws Exception {
        mockMvc.perform(get("/admin-assets/css/admin.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(".app")));
        mockMvc.perform(get("/admin-assets/img/dbflow-logo.svg"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<svg")));
    }

    /**
     * 验证管理员可以访问后台总览页。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRenderAdminOverviewForAdmin() throws Exception {
        mockMvc.perform(get("/admin-legacy").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SQL 请求")))
                .andExpect(content().string(containsString("最近审计事件")));
    }

    /**
     * 验证基础管理页面均可被管理员访问。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRenderBaseAdminPagesForAdmin() throws Exception {
        assertAdminPage("/admin-legacy/users", "用户管理");
        assertAdminPage("/admin-legacy/grants", "项目授权");
        assertAdminPage("/admin-legacy/tokens", "Token 管理");
        assertAdminPage("/admin-legacy/config", "配置查看");
        assertAdminPage("/admin-legacy/policies/dangerous", "危险策略");
        assertAdminPage("/admin-legacy/audit", "审计列表");
        assertAdminPage("/admin-legacy/health", "系统健康");
    }

    /**
     * 验证普通用户不能访问管理端页面。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRejectNonAdminPageRequest() throws Exception {
        mockMvc.perform(get("/admin-legacy").with(user("operator").roles("USER")))
                .andExpect(status().isForbidden());
    }

    /**
     * 断言管理员访问页面成功。
     *
     * @param path         页面路径
     * @param expectedText 预期页面文案
     * @throws Exception MockMvc 执行异常
     */
    private void assertAdminPage(String path, String expectedText) throws Exception {
        mockMvc.perform(get(path).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(expectedText)))
                .andExpect(content().string(containsString("DBFlow Admin")));
    }
}
