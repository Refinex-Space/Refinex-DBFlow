package com.refinex.dbflow.security;

import com.refinex.dbflow.access.entity.DbfApiToken;
import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.repository.DbfApiTokenRepository;
import com.refinex.dbflow.access.repository.DbfUserRepository;
import com.refinex.dbflow.common.DbflowException;
import com.refinex.dbflow.security.properties.McpTokenProperties;
import com.refinex.dbflow.security.token.McpTokenIssueResult;
import com.refinex.dbflow.security.token.McpTokenService;
import com.refinex.dbflow.security.token.McpTokenValidationResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MCP Token 生命周期服务 JPA 测试。
 *
 * @author refinex
 */
@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:mcp_token_service_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableConfigurationProperties(McpTokenProperties.class)
@Import(McpTokenService.class)
class McpTokenServiceJpaTests {

    /**
     * 测试专用 pepper，模拟来自安全外部配置的运行时注入值。
     */
    private static final String TEST_TOKEN_PEPPER = UUID.randomUUID().toString();

    /**
     * MCP Token 生命周期服务。
     */
    @Autowired
    private McpTokenService tokenService;

    /**
     * API Token repository。
     */
    @Autowired
    private DbfApiTokenRepository apiTokenRepository;

    /**
     * 用户 repository。
     */
    @Autowired
    private DbfUserRepository userRepository;

    /**
     * 注册测试专用 MCP Token pepper。
     *
     * @param registry 动态属性注册器
     */
    @DynamicPropertySource
    static void registerTokenPepper(DynamicPropertyRegistry registry) {
        registry.add("dbflow.security.mcp-token.pepper", () -> TEST_TOKEN_PEPPER);
    }

    /**
     * 验证 Token 颁发只返回一次明文，数据库仅保存 hash 和 prefix。
     */
    @Test
    void shouldIssueTokenWithoutPersistingPlaintext() {
        DbfUser user = createUser("token-alice");
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.DAYS);

        McpTokenIssueResult result = tokenService.issueToken(user.getId(), expiresAt);

        DbfApiToken storedToken = apiTokenRepository.findById(result.tokenId()).orElseThrow();
        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.plaintextToken()).startsWith("dbf_");
        assertThat(result.tokenPrefix()).isEqualTo(result.plaintextToken().substring(0, 16));
        assertThat(storedToken.getTokenHash()).isNotBlank();
        assertThat(storedToken.getTokenHash()).isNotEqualTo(result.plaintextToken());
        assertThat(storedToken.getTokenPrefix()).isEqualTo(result.tokenPrefix());
        assertThat(storedToken.getStatus()).isEqualTo("ACTIVE");
        assertThat(storedToken.getLastUsedAt()).isNull();
    }

    /**
     * 验证同一用户存在 active Token 时禁止重复颁发。
     */
    @Test
    void shouldRejectDuplicateActiveTokenIssue() {
        DbfUser user = createUser("token-bob");
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.DAYS);

        tokenService.issueToken(user.getId(), expiresAt);

        assertThatThrownBy(() -> tokenService.issueToken(user.getId(), expiresAt))
                .isInstanceOf(DbflowException.class)
                .hasMessageContaining("用户已有 active MCP Token");
        assertThat(apiTokenRepository.findByUserIdAndStatus(user.getId(), "ACTIVE")).isPresent();
    }

    /**
     * 验证 Token 校验成功后会更新最近使用时间。
     */
    @Test
    void shouldValidateTokenAndUpdateLastUsedAt() {
        DbfUser user = createUser("token-carol");
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant usedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        McpTokenIssueResult issuedToken = tokenService.issueToken(user.getId(), expiresAt);

        Optional<McpTokenValidationResult> validationResult =
                tokenService.validateToken(issuedToken.plaintextToken(), usedAt);

        DbfApiToken storedToken = apiTokenRepository.findById(issuedToken.tokenId()).orElseThrow();
        assertThat(validationResult).isPresent();
        assertThat(validationResult.orElseThrow().userId()).isEqualTo(user.getId());
        assertThat(validationResult.orElseThrow().tokenPrefix()).isEqualTo(issuedToken.tokenPrefix());
        assertThat(validationResult.orElseThrow().lastUsedAt()).isEqualTo(usedAt);
        assertThat(storedToken.getLastUsedAt()).isEqualTo(usedAt);
    }

    /**
     * 验证无效 Token 无法通过校验。
     */
    @Test
    void shouldRejectUnknownToken() {
        DbfUser user = createUser("token-dave");
        McpTokenIssueResult issuedToken = tokenService.issueToken(
                user.getId(),
                Instant.now().plus(1, ChronoUnit.DAYS)
        );

        Optional<McpTokenValidationResult> validationResult =
                tokenService.validateToken(issuedToken.plaintextToken() + "x", Instant.now());

        assertThat(validationResult).isEmpty();
    }

    /**
     * 验证吊销后旧 Token 不可用，并允许重新颁发新 Token。
     */
    @Test
    void shouldRejectRevokedTokenAndAllowReissue() {
        DbfUser user = createUser("token-erin");
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.DAYS);
        McpTokenIssueResult firstToken = tokenService.issueToken(user.getId(), expiresAt);

        DbfApiToken revokedToken = tokenService.revokeActiveToken(user.getId(), Instant.now());
        McpTokenIssueResult secondToken = tokenService.issueToken(user.getId(), expiresAt);

        assertThat(revokedToken.getStatus()).isEqualTo("REVOKED");
        assertThat(revokedToken.getRevokedAt()).isNotNull();
        assertThat(tokenService.validateToken(firstToken.plaintextToken(), Instant.now())).isEmpty();
        assertThat(secondToken.plaintextToken()).isNotEqualTo(firstToken.plaintextToken());
        assertThat(tokenService.validateToken(secondToken.plaintextToken(), Instant.now())).isPresent();
    }

    /**
     * 创建测试用户。
     *
     * @param username 用户名
     * @return 用户实体
     */
    private DbfUser createUser(String username) {
        return userRepository.save(DbfUser.create(username, username, "password-hash"));
    }
}
