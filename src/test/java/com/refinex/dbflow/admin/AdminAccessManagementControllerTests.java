package com.refinex.dbflow.admin;

import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.repository.DbfUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 管理端用户、Token、授权页面 smoke 测试。
 *
 * @author refinex
 */
@SpringBootTest(properties = {
                "spring.datasource.url=jdbc:h2:mem:admin_access_management_controller_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.locations=classpath:db/migration",
                "dbflow.admin.initial-user.enabled=false",
                "dbflow.security.mcp-token.pepper=admin-controller-test-pepper",
                "dbflow.datasource-defaults.driver-class-name=org.h2.Driver",
                "dbflow.projects[0].key=billing-core",
                "dbflow.projects[0].name=Billing Core",
                "dbflow.projects[0].environments[0].key=staging",
                "dbflow.projects[0].environments[0].name=Staging",
                "dbflow.projects[0].environments[0].jdbc-url=jdbc:h2:mem:target_admin_controller_staging;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class AdminAccessManagementControllerTests {

        /**
         * 完整 Token 明文匹配模式。
         */
        private static final Pattern PLAINTEXT_TOKEN_PATTERN = Pattern.compile("dbf_[A-Za-z0-9_-]{20,}");

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
         * 验证管理端页面可以完成创建用户、授权环境、颁发 Token 和吊销 Token。
         *
         * @throws Exception MockMvc 执行异常
         */
        @Test
        void shouldSmokeManageUserGrantIssueAndRevokeToken() throws Exception {
                mockMvc.perform(post("/admin/users")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf())
                                .param("username", "ops.bob")
                                .param("displayName", "Ops Bob")
                                .param("password", "Admin123456!"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/users"));
                DbfUser createdUser = userRepository.findByUsername("ops.bob").orElseThrow();

                mockMvc.perform(get("/admin/users").with(user("admin").roles("ADMIN")))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("ops.bob")))
                                .andExpect(content().string(not(containsString("password_hash"))));

                mockMvc.perform(post("/admin/grants")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf())
                                .param("userId", createdUser.getId().toString())
                                .param("projectKey", "billing-core")
                                .param("environmentKey", "staging")
                                .param("grantType", "WRITE"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/grants"));

                mockMvc.perform(get("/admin/grants").with(user("admin").roles("ADMIN")))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("ops.bob")))
                                .andExpect(content().string(containsString("billing-core")))
                                .andExpect(content().string(not(containsString("password="))));

                MvcResult issueResult = mockMvc.perform(post("/admin/tokens")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf())
                                .param("userId", createdUser.getId().toString())
                                .param("expiresInDays", "7"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/tokens"))
                                .andReturn();

                MvcResult tokenPage = mockMvc.perform(get("/admin/tokens")
                                .with(user("admin").roles("ADMIN"))
                                .flashAttrs(issueResult.getFlashMap()))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("tokenRevealModal")))
                                .andExpect(content().string(not(containsString("token_hash"))))
                                .andReturn();
                String plaintextToken = findPlaintextToken(tokenPage.getResponse().getContentAsString());

                mockMvc.perform(get("/admin/tokens").with(user("admin").roles("ADMIN")))
                                .andExpect(status().isOk())
                                .andExpect(content().string(not(containsString(plaintextToken))));

                mockMvc.perform(post("/admin/tokens/" + firstTokenId(tokenPage.getResponse().getContentAsString())
                                + "/revoke")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/tokens"));
                mockMvc.perform(get("/admin/tokens").with(user("admin").roles("ADMIN")).param("status", "REVOKED"))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("REVOKED")));
        }

        /**
         * 验证重新颁发 Token 会展示新的单次明文。
         *
         * @throws Exception MockMvc 执行异常
         */
        @Test
        void shouldReissueTokenWithNewOneTimePlaintext() throws Exception {
                DbfUser user = userRepository.save(DbfUser.create("ops.carol", "Ops Carol", "password-hash"));
                mockMvc.perform(post("/admin/users/" + user.getId() + "/tokens/reissue")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf())
                                .param("expiresInDays", "14"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/tokens"))
                                .andExpect(flash().attributeExists("issuedToken"));
        }

        /**
         * 验证非管理员不能写管理端资源。
         *
         * @throws Exception MockMvc 执行异常
         */
        @Test
        void shouldRejectNonAdminWrite() throws Exception {
                mockMvc.perform(post("/admin/users")
                                .with(user("operator").roles("USER"))
                                .with(csrf())
                                .param("username", "ops.denied")
                                .param("displayName", "Denied"))
                                .andExpect(status().isForbidden());
        }

        /**
         * 验证缺少 CSRF 的 POST 被拒绝。
         *
         * @throws Exception MockMvc 执行异常
         */
        @Test
        void shouldRejectPostWithoutCsrf() throws Exception {
                mockMvc.perform(post("/admin/users")
                                .with(user("admin").roles("ADMIN"))
                                .param("username", "ops.csrf")
                                .param("displayName", "Csrf"))
                                .andExpect(status().isForbidden());
        }

        /**
         * 从 HTML 中解析一次性明文 Token。
         *
         * @param html HTML 内容
         * @return 明文 Token
         */
        private String findPlaintextToken(String html) {
                Matcher matcher = PLAINTEXT_TOKEN_PATTERN.matcher(html);
                assertThat(matcher.find()).isTrue();
                return matcher.group();
        }

        /**
         * 从 HTML 中解析第一个 Token 主键。
         *
         * @param html HTML 内容
         * @return Token 主键
         */
        private String firstTokenId(String html) {
                Matcher matcher = Pattern.compile("/admin/tokens/(\\d+)/revoke").matcher(html);
                assertThat(matcher.find()).isTrue();
                return matcher.group(1);
        }
}
