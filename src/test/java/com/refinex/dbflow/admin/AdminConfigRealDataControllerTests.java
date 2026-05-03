package com.refinex.dbflow.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理端配置页真实数据和脱敏测试。
 *
 * @author refinex
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_config_real_data_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.admin.initial-user.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "dbflow.datasource-defaults.driver-class-name=org.h2.Driver",
        "dbflow.datasource-defaults.username=sa",
        "dbflow.datasource-defaults.hikari.maximum-pool-size=3",
        "dbflow.datasource-defaults.hikari.minimum-idle=1",
        "dbflow.projects[0].key=config-real",
        "dbflow.projects[0].name=Config Real",
        "dbflow.projects[0].environments[0].key=dev",
        "dbflow.projects[0].environments[0].name=Development",
        "dbflow.projects[0].environments[0].jdbc-url=jdbc:h2:mem:admin_config_target;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "dbflow.projects[0].environments[0].driver-class-name=org.h2.Driver",
        "dbflow.projects[0].environments[0].username=config_user"
})
@AutoConfigureMockMvc
class AdminConfigRealDataControllerTests {

    /**
     * MockMvc 测试客户端。
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证配置页展示真实脱敏配置。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRenderRealRedactedConfigRows() throws Exception {
        mockMvc.perform(get("/admin-legacy/config").with(user("config-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("config-admin")))
                .andExpect(content().string(containsString("config-real")))
                .andExpect(content().string(containsString("Config Real")))
                .andExpect(content().string(containsString("dev")))
                .andExpect(content().string(containsString("h2")))
                .andExpect(content().string(containsString("admin_config_target")))
                .andExpect(content().string(containsString("config_user")))
                .andExpect(content().string(containsString("maxPool=3")))
                .andExpect(content().string(containsString("minIdle=1")))
                .andExpect(content().string(containsString("Local application config")))
                .andExpect(content().string(not(containsString("jdbc:h2:mem:admin_config_target"))))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("DB_CLOSE_DELAY"))))
                .andExpect(content().string(not(containsString("source=nacos:dbflow-targets-prod.yml"))));
    }

    /**
     * 验证普通用户不能访问配置页。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRejectNonAdminConfigRequest() throws Exception {
        mockMvc.perform(get("/admin-legacy/config").with(user("operator").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
