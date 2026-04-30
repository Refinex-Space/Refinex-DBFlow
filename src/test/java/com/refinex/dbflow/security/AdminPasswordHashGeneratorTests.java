package com.refinex.dbflow.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 管理员初始密码 BCrypt hash 生成辅助测试。
 *
 * @author refinex
 */
class AdminPasswordHashGeneratorTests {

    /**
     * 启用 hash 生成测试的 system property。
     */
    private static final String ENABLE_PROPERTY = "dbflow.generate-admin-password-hash";

    /**
     * 原始密码 system property，适合一次性本地命令使用。
     */
    private static final String PASSWORD_PROPERTY = "dbflow.admin.initial-password";

    /**
     * 原始密码环境变量，避免把密码直接写进命令行参数。
     */
    private static final String PASSWORD_ENV = "DBFLOW_ADMIN_INITIAL_PASSWORD";

    /**
     * 按需生成可写入 DBFLOW_ADMIN_INITIAL_PASSWORD_HASH 的 BCrypt hash。
     */
    @Test
    @EnabledIfSystemProperty(named = ENABLE_PROPERTY, matches = "true")
    void shouldPrintBcryptHashForInitialAdminPassword() {
        String rawPassword = resolveRawPassword();
        assertThat(StringUtils.hasText(rawPassword))
                .as("Set -D%s='<password>' or environment variable %s.", PASSWORD_PROPERTY, PASSWORD_ENV)
                .isTrue();

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String passwordHash = passwordEncoder.encode(rawPassword);

        assertThat(passwordEncoder.matches(rawPassword, passwordHash)).isTrue();
        System.out.printf("%nDBFLOW_ADMIN_INITIAL_PASSWORD_HASH='%s'%n%n", passwordHash);
    }

    /**
     * 解析原始密码，system property 优先于环境变量。
     *
     * @return 原始密码
     */
    private String resolveRawPassword() {
        String propertyValue = System.getProperty(PASSWORD_PROPERTY);
        if (StringUtils.hasText(propertyValue)) {
            return propertyValue;
        }
        return System.getenv(PASSWORD_ENV);
    }
}
