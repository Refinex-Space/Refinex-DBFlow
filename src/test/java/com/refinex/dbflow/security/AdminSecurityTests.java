package com.refinex.dbflow.security;

import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.repository.DbfUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 管理端 session 安全测试。
 *
 * @author refinex
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_security_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.admin.initial-user.enabled=true",
        "dbflow.admin.initial-user.username=admin-test",
        "dbflow.admin.initial-user.display-name=Admin Test"
})
@AutoConfigureMockMvc
class AdminSecurityTests {

    /**
     * 测试用管理员用户名。
     */
    private static final String ADMIN_USERNAME = "admin-test";

    /**
     * 测试用管理员密码，运行期随机生成并注入测试上下文。
     */
    private static final String ADMIN_PASSWORD = UUID.randomUUID().toString();

    /**
     * MockMvc 测试客户端。
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 用户 repository。
     */
    @Autowired
    private DbfUserRepository userRepository;

    /**
     * 密码编码器。
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 注入测试用初始化管理员密码。
     *
     * @param registry 动态属性注册器
     */
    @DynamicPropertySource
    static void adminSecurityProperties(DynamicPropertyRegistry registry) {
        registry.add("dbflow.admin.initial-user.password", () -> ADMIN_PASSWORD);
    }

    /**
     * 验证未登录访问管理端会跳转到登录页。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRedirectAnonymousAdminRequestToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    /**
     * 验证正确管理员账号可以登录管理端。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldLoginWithInitialAdminUser() throws Exception {
        mockMvc.perform(formLogin().user(ADMIN_USERNAME).password(ADMIN_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(authenticated().withUsername(ADMIN_USERNAME));
    }

    /**
     * 验证错误密码登录失败。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRejectInvalidAdminPassword() throws Exception {
        mockMvc.perform(formLogin().user(ADMIN_USERNAME).password("wrong-test-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    /**
     * 验证管理端退出接口受 CSRF 保护。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldProtectLogoutWithCsrf() throws Exception {
        mockMvc.perform(post("/logout").with(user(ADMIN_USERNAME).roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    /**
     * 验证初始化管理员密码以 BCrypt hash 存储。
     */
    @Test
    void shouldStoreInitialAdminPasswordAsBCryptHash() {
        DbfUser user = userRepository.findByUsername(ADMIN_USERNAME).orElseThrow();

        assertThat(user.getPasswordHash()).isNotEqualTo(ADMIN_PASSWORD);
        assertThat(passwordEncoder.matches(ADMIN_PASSWORD, user.getPasswordHash())).isTrue();
    }
}
