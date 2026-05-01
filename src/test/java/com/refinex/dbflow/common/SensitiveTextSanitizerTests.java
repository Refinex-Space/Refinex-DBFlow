package com.refinex.dbflow.common;

import com.refinex.dbflow.common.util.SensitiveTextSanitizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 敏感文本脱敏工具测试。
 *
 * @author refinex
 */
class SensitiveTextSanitizerTests {

    /**
     * 验证 JDBC URL 会被整体替换，避免连接串进入审计和诊断输出。
     */
    @Test
    void sanitizeShouldRedactJdbcUrl() {
        String sanitized = SensitiveTextSanitizer.sanitize(
                "connect jdbc:mysql://localhost:3306/app?user=root&password=secret"
        );

        assertThat(sanitized).isEqualTo("connect [REDACTED]");
    }

    /**
     * 验证带引号和不带引号的密码参数都会被脱敏。
     */
    @Test
    void sanitizeShouldRedactPasswordParameters() {
        String sanitized = SensitiveTextSanitizer.sanitize("password=secret pwd='abc' pwd=\"def\"");

        assertThat(sanitized).isEqualTo("password=[REDACTED] pwd='[REDACTED]' pwd=\"[REDACTED]\"");
    }

    /**
     * 验证 MySQL identified by 语句不会泄露账号密码。
     */
    @Test
    void sanitizeShouldRedactIdentifiedByPassword() {
        String sanitized = SensitiveTextSanitizer.sanitize("create user app identified by 'secret'");

        assertThat(sanitized).isEqualTo("create user app identified by '[REDACTED]'");
    }

    /**
     * 验证 Token 类字段统一脱敏。
     */
    @Test
    void sanitizeShouldRedactTokenValues() {
        String sanitized = SensitiveTextSanitizer.sanitize(
                "token=abc access_token=def authorization: Bearer ghi"
        );

        assertThat(sanitized).isEqualTo(
                "token=[REDACTED] access_token=[REDACTED] authorization: [REDACTED]"
        );
    }

    /**
     * 验证空白输入保持原始语义。
     */
    @Test
    void sanitizeShouldKeepBlankValues() {
        assertThat(SensitiveTextSanitizer.sanitize(null)).isNull();
        assertThat(SensitiveTextSanitizer.sanitize("")).isEmpty();
        assertThat(SensitiveTextSanitizer.sanitize("  ")).isEqualTo("  ");
    }
}
