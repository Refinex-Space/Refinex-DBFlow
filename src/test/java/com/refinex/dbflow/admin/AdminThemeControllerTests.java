package com.refinex.dbflow.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理端主题与共享 shell 合同测试。
 *
 * @author refinex
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_theme_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.admin.initial-user.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false"
})
@AutoConfigureMockMvc
class AdminThemeControllerTests {

    /**
     * MockMvc 测试客户端。
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证登录页包含管理端样式入口且无需管理员会话。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRenderLoginWithAdminStylesheet() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Refinex-DBFlow Admin")))
                .andExpect(content().string(containsString("/admin-assets/css/admin.css")))
                .andExpect(content().string(containsString("/admin-assets/img/dbflow-logo.svg")))
                .andExpect(content().string(containsString("data-theme-toggle")))
                .andExpect(content().string(containsString("theme-toggle-moon")))
                .andExpect(content().string(containsString("theme-toggle-sun")))
                .andExpect(content().string(containsString("登录后台")));
    }

    /**
     * 验证管理端共享 shell 暴露主题切换合同和运行时状态。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRenderThemeControlAndSharedShellForAdmin() throws Exception {
        mockMvc.perform(get("/admin").with(user("theme-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-theme-choice=\"system\"")))
                .andExpect(content().string(containsString("data-theme-choice=\"light\"")))
                .andExpect(content().string(containsString("data-theme-choice=\"dark\"")))
                .andExpect(content().string(containsString("aria-label=\"切换主题\"")))
                .andExpect(content().string(containsString("/admin-assets/img/dbflow-logo.svg")))
                .andExpect(content().string(containsString("Local application config")))
                .andExpect(content().string(containsString("theme-admin")))
                .andExpect(content().string(containsString("退出登录")));
    }

    /**
     * 验证普通用户不能访问管理端主题 shell。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRejectNonAdminThemeShellRequest() throws Exception {
        mockMvc.perform(get("/admin").with(user("operator").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
