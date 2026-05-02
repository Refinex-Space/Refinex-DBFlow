package com.refinex.dbflow.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * React 管理端 JSON 登录和退出测试。
 *
 * @author refinex
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_json_login_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.admin.initial-user.enabled=true",
        "dbflow.admin.initial-user.username=admin-json",
        "dbflow.admin.initial-user.display-name=Admin JSON"
})
@AutoConfigureMockMvc
class AdminJsonLoginTests {

    /**
     * CSRF cookie 名称。
     */
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";

    /**
     * SPA 提交 CSRF token 的请求头名称。
     */
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    /**
     * XHR 请求头名称。
     */
    private static final String REQUESTED_WITH_HEADER = "X-Requested-With";

    /**
     * XHR 请求头值。
     */
    private static final String XML_HTTP_REQUEST = "XMLHttpRequest";

    /**
     * 测试用管理员用户名。
     */
    private static final String ADMIN_USERNAME = "admin-json";

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
     * 注入测试用初始化管理员密码。
     *
     * @param registry 动态属性注册器
     */
    @DynamicPropertySource
    static void adminSecurityProperties(DynamicPropertyRegistry registry) {
        registry.add("dbflow.admin.initial-user.password", () -> ADMIN_PASSWORD);
    }

    /**
     * 验证 JSON 登录成功返回当前 session 信息。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldReturnSessionJsonWhenJsonLoginSucceeds() throws Exception {
        Cookie csrfCookie = fetchCsrfCookie();

        mockMvc.perform(post("/login")
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", ADMIN_USERNAME)
                        .param("password", ADMIN_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.authenticated").value(true))
                .andExpect(jsonPath("$.data.username").value(ADMIN_USERNAME))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.data.shell.adminName").value(ADMIN_USERNAME));
    }

    /**
     * 验证 JSON 登录失败返回 401 JSON。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldReturnJsonUnauthorizedWhenJsonLoginFails() throws Exception {
        Cookie csrfCookie = fetchCsrfCookie();

        mockMvc.perform(post("/login")
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .header(REQUESTED_WITH_HEADER, XML_HTTP_REQUEST)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", ADMIN_USERNAME)
                        .param("password", "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    /**
     * 验证 JSON 退出成功返回 JSON 且会失效 session。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldReturnJsonAndInvalidateSessionWhenJsonLogoutSucceeds() throws Exception {
        Cookie csrfCookie = fetchCsrfCookie();
        MvcResult loginResult = loginWithJson(csrfCookie);
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        Cookie logoutCsrfCookie = latestCsrfCookie(loginResult, csrfCookie);

        assertThat(session).isNotNull();

        mockMvc.perform(post("/logout")
                        .session(session)
                        .cookie(logoutCsrfCookie)
                        .header(CSRF_HEADER_NAME, logoutCsrfCookie.getValue())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"));

        assertThat(session.isInvalid()).isTrue();
    }

    /**
     * 验证 JSON 登录仍然要求 CSRF token。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldKeepJsonLoginCsrfProtected() throws Exception {
        mockMvc.perform(post("/login")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", ADMIN_USERNAME)
                        .param("password", ADMIN_PASSWORD))
                .andExpect(status().isForbidden());
    }

    /**
     * 获取 CSRF cookie。
     *
     * @return CSRF cookie
     * @throws Exception MockMvc 执行异常
     */
    private Cookie fetchCsrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/login").header(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(CSRF_COOKIE_NAME))
                .andReturn();
        Cookie csrfCookie = result.getResponse().getCookie(CSRF_COOKIE_NAME);
        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.getValue()).isNotBlank();
        return csrfCookie;
    }

    /**
     * 执行 JSON 登录。
     *
     * @param csrfCookie CSRF cookie
     * @return 登录结果
     * @throws Exception MockMvc 执行异常
     */
    private MvcResult loginWithJson(Cookie csrfCookie) throws Exception {
        return mockMvc.perform(post("/login")
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", ADMIN_USERNAME)
                        .param("password", ADMIN_PASSWORD))
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * 读取响应中最新的非空 CSRF cookie。
     *
     * @param result         MockMvc 执行结果
     * @param fallbackCookie 兜底 CSRF cookie
     * @return 最新 CSRF cookie
     */
    private Cookie latestCsrfCookie(MvcResult result, Cookie fallbackCookie) {
        Cookie[] cookies = result.getResponse().getCookies();
        for (int index = cookies.length - 1; index >= 0; index--) {
            Cookie cookie = cookies[index];
            if (CSRF_COOKIE_NAME.equals(cookie.getName())
                    && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return cookie;
            }
        }
        return fallbackCookie;
    }
}
