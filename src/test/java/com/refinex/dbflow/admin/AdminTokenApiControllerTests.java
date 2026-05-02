package com.refinex.dbflow.admin;

import com.refinex.dbflow.admin.command.CreateUserCommand;
import com.refinex.dbflow.admin.service.AdminAccessManagementService;
import com.refinex.dbflow.admin.view.UserRow;
import com.refinex.dbflow.audit.entity.DbfAuditEvent;
import com.refinex.dbflow.audit.repository.DbfAuditEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
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
 * React 管理端 Token 管理 JSON API 测试。
 *
 * @author refinex
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_token_api_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.admin.initial-user.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "dbflow.security.mcp-token.pepper=admin-token-api-test-pepper"
})
@AutoConfigureMockMvc
class AdminTokenApiControllerTests {

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
     * 管理端访问管理服务。
     */
    @Autowired
    private AdminAccessManagementService accessManagementService;

    /**
     * 审计事件 repository。
     */
    @Autowired
    private DbfAuditEventRepository auditEventRepository;

    /**
     * 验证管理员可以颁发 Token，且明文只出现在当次响应，不出现在列表和审计中。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldIssueTokenAndKeepPlaintextOutOfListAndAudit() throws Exception {
        UserRow user = createFixtureUser(uniqueUsername("token-issue"));

        MvcResult issueResult = mockMvc.perform(post("/admin/api/tokens")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "expiresInDays": 7
                                }
                                """.formatted(user.id())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(user.id()))
                .andExpect(jsonPath("$.data.plaintextToken").isString())
                .andExpect(jsonPath("$.data.tokenPrefix").isString())
                .andReturn();
        String plaintextToken = findPlaintextToken(issueResult.getResponse().getContentAsString());

        mockMvc.perform(get("/admin/api/tokens")
                        .with(user("admin").roles("ADMIN"))
                        .param("username", user.username())
                        .param("status", "ACTIVE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].username").value(user.username()))
                .andExpect(jsonPath("$.data[0].tokenPrefix").isString())
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].expiresAt").exists())
                .andExpect(jsonPath("$.data[0].lastUsedAt").doesNotExist())
                .andExpect(jsonPath("$.data[0].plaintextToken").doesNotExist())
                .andExpect(jsonPath("$.data[0].tokenHash").doesNotExist())
                .andExpect(content().string(not(containsString(plaintextToken))));
        assertAuditDoesNotContain(plaintextToken);
    }

    /**
     * 验证重新颁发 Token 只在当次响应返回新明文，列表和审计不出现新旧明文。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldReissueTokenWithOneTimePlaintextOnly() throws Exception {
        UserRow user = createFixtureUser(uniqueUsername("token-reissue"));
        String firstPlaintext = issueTokenFor(user.id(), 7);

        MvcResult reissueResult = mockMvc.perform(post("/admin/api/users/{userId}/tokens/reissue", user.id())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expiresInDays": 14
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(user.id()))
                .andExpect(jsonPath("$.data.plaintextToken").isString())
                .andReturn();
        String secondPlaintext = findPlaintextToken(reissueResult.getResponse().getContentAsString());
        assertThat(secondPlaintext).isNotEqualTo(firstPlaintext);

        mockMvc.perform(get("/admin/api/tokens")
                        .with(user("admin").roles("ADMIN"))
                        .param("username", user.username())
                        .param("status", "ACTIVE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].plaintextToken").doesNotExist())
                .andExpect(jsonPath("$.data[0].tokenHash").doesNotExist())
                .andExpect(content().string(not(containsString(firstPlaintext))))
                .andExpect(content().string(not(containsString(secondPlaintext))));
        assertAuditDoesNotContain(firstPlaintext);
        assertAuditDoesNotContain(secondPlaintext);
    }

    /**
     * 验证管理员可以吊销 Token，且吊销响应不返回明文。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRevokeTokenWithoutReturningPlaintext() throws Exception {
        UserRow user = createFixtureUser(uniqueUsername("token-revoke"));
        MvcResult issueResult = issueTokenResultFor(user.id(), 7);
        String plaintextToken = findPlaintextToken(issueResult.getResponse().getContentAsString());
        Number tokenId = com.jayway.jsonpath.JsonPath.read(
                issueResult.getResponse().getContentAsString(),
                "$.data.tokenId");

        mockMvc.perform(post("/admin/api/tokens/{tokenId}/revoke", tokenId.longValue())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.revoked").value(true))
                .andExpect(content().string(not(containsString(plaintextToken))));

        mockMvc.perform(get("/admin/api/tokens")
                        .with(user("admin").roles("ADMIN"))
                        .param("username", user.username())
                        .param("status", "REVOKED")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("REVOKED"))
                .andExpect(jsonPath("$.data[0].plaintextToken").doesNotExist())
                .andExpect(jsonPath("$.data[0].tokenHash").doesNotExist())
                .andExpect(content().string(not(containsString(plaintextToken))));
        assertAuditDoesNotContain(plaintextToken);
    }

    /**
     * 验证 Token API mutation 缺少 CSRF 时被安全链拒绝。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldRejectMutationWithoutCsrf() throws Exception {
        mockMvc.perform(post("/admin/api/tokens")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "expiresInDays": 7
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    /**
     * 通过 API 颁发 Token 并返回明文。
     *
     * @param userId        用户主键
     * @param expiresInDays 有效天数
     * @return Token 明文
     * @throws Exception MockMvc 执行异常
     */
    private String issueTokenFor(Long userId, Integer expiresInDays) throws Exception {
        return findPlaintextToken(issueTokenResultFor(userId, expiresInDays).getResponse().getContentAsString());
    }

    /**
     * 通过 API 颁发 Token 并返回响应结果。
     *
     * @param userId        用户主键
     * @param expiresInDays 有效天数
     * @return MockMvc 响应结果
     * @throws Exception MockMvc 执行异常
     */
    private MvcResult issueTokenResultFor(Long userId, Integer expiresInDays) throws Exception {
        return mockMvc.perform(post("/admin/api/tokens")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "expiresInDays": %d
                                }
                                """.formatted(userId, expiresInDays)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
    }

    /**
     * 断言审计事件中不存在 Token 明文。
     *
     * @param plaintextToken Token 明文
     */
    private void assertAuditDoesNotContain(String plaintextToken) {
        String auditText = auditEventRepository.findAll().stream()
                .map(this::auditEventText)
                .reduce("", String::concat);
        assertThat(auditText).doesNotContain(plaintextToken);
    }

    /**
     * 拼接审计事件中可能出现外部输入的文本字段。
     *
     * @param event 审计事件
     * @return 审计文本
     */
    private String auditEventText(DbfAuditEvent event) {
        return String.join("|",
                safe(event.getRequestId()),
                safe(event.getTokenPrefix()),
                safe(event.getProjectKey()),
                safe(event.getEnvironmentKey()),
                safe(event.getClientName()),
                safe(event.getClientVersion()),
                safe(event.getUserAgent()),
                safe(event.getSourceIp()),
                safe(event.getTool()),
                safe(event.getOperationType()),
                safe(event.getRiskLevel()),
                safe(event.getStatus()),
                safe(event.getDecision()),
                safe(event.getSqlHash()),
                safe(event.getSqlText()),
                safe(event.getResultSummary()),
                safe(event.getErrorCode()),
                safe(event.getErrorMessage()),
                safe(event.getConfirmationId()));
    }

    /**
     * 将可能为空的字符串转为安全文本。
     *
     * @param value 原始字符串
     * @return 非空字符串
     */
    private String safe(String value) {
        return value == null ? "" : value;
    }

    /**
     * 从 JSON 中解析 Token 明文。
     *
     * @param json JSON 响应
     * @return Token 明文
     */
    private String findPlaintextToken(String json) {
        Matcher matcher = PLAINTEXT_TOKEN_PATTERN.matcher(json);
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }

    /**
     * 创建测试用户。
     *
     * @param username 用户名
     * @return 用户行视图
     */
    private UserRow createFixtureUser(String username) {
        return accessManagementService.createUser(
                new CreateUserCommand(username, username, "Admin123456!"));
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
