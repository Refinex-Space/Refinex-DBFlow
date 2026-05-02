package com.refinex.dbflow.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * React 管理端 CSRF cookie 安全测试。
 *
 * @author refinex
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_csrf_spa_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.admin.initial-user.enabled=false"
})
@AutoConfigureMockMvc
class AdminCsrfSpaTests {

    /**
     * CSRF cookie 名称。
     */
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";

    /**
     * SPA 提交 CSRF token 的请求头名称。
     */
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    /**
     * Thymeleaf 渲染的 CSRF 隐藏字段匹配规则。
     */
    private static final Pattern CSRF_INPUT_PATTERN = Pattern.compile(
            "name=\"_csrf\"[^>]*value=\"([^\"]+)\"|value=\"([^\"]+)\"[^>]*name=\"_csrf\"");

    /**
     * MockMvc 测试客户端。
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证安全 GET 会下发浏览器可读的 CSRF cookie。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldExposeReadableXsrfCookieOnLoginPage() throws Exception {
        MvcResult result = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(CSRF_COOKIE_NAME))
                .andExpect(cookie().httpOnly(CSRF_COOKIE_NAME, false))
                .andReturn();

        Cookie csrfCookie = result.getResponse().getCookie(CSRF_COOKIE_NAME);
        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.getValue()).isNotBlank();
    }

    /**
     * 验证未携带 CSRF token 的管理端 API mutation 会被拒绝。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRejectAdminApiMutationWithoutCsrf() throws Exception {
        mockMvc.perform(post("/admin/api/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    /**
     * 验证 SPA 可以通过 X-XSRF-TOKEN 请求头提交 cookie 中的原始 token。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldAcceptXsrfHeaderFromSpaCookie() throws Exception {
        Cookie csrfCookie = fetchCsrfCookie();

        MvcResult result = mockMvc.perform(post("/admin/api/users")
                        .with(user("admin").roles("ADMIN"))
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue()))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isNotEqualTo(HttpStatus.FORBIDDEN.value());
    }

    /**
     * 验证 Thymeleaf 隐藏字段形式的 CSRF token 仍可用于表单提交。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldKeepThymeleafHiddenCsrfFormSubmission() throws Exception {
        MvcResult loginResult = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie csrfCookie = loginResult.getResponse().getCookie(CSRF_COOKIE_NAME);
        String csrfParameter = extractCsrfParameter(loginResult.getResponse().getContentAsString());

        mockMvc.perform(post("/admin/users")
                        .with(user("admin").roles("ADMIN"))
                        .cookie(csrfCookie)
                        .param("_csrf", csrfParameter)
                        .param("username", "csrf.form.user")
                        .param("displayName", "CSRF Form User")
                        .param("password", "Admin123456!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
    }

    /**
     * 获取 CSRF cookie。
     *
     * @return CSRF cookie
     * @throws Exception MockMvc 执行异常
     */
    private Cookie fetchCsrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie csrfCookie = result.getResponse().getCookie(CSRF_COOKIE_NAME);
        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.getValue()).isNotBlank();
        return csrfCookie;
    }

    /**
     * 提取 Thymeleaf 渲染出的 CSRF 隐藏字段值。
     *
     * @param html 登录页 HTML
     * @return CSRF 隐藏字段值
     */
    private String extractCsrfParameter(String html) {
        Matcher matcher = CSRF_INPUT_PATTERN.matcher(html);
        assertThat(matcher.find()).isTrue();
        String token = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        assertThat(token).isNotBlank();
        return token;
    }
}
