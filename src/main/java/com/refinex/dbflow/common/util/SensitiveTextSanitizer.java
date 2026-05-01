package com.refinex.dbflow.common.util;

import java.util.regex.Pattern;

/**
 * 敏感文本脱敏工具，统一隐藏连接串、密码和 Token 类字段。
 *
 * @author refinex
 */
public final class SensitiveTextSanitizer {

    /**
     * 统一脱敏占位符。
     */
    public static final String REDACTED = "[REDACTED]";

    /**
     * JDBC 连接串模式。
     */
    private static final Pattern JDBC_URL_PATTERN = Pattern.compile("jdbc:[^\\s,'\";]+", Pattern.CASE_INSENSITIVE);

    /**
     * 带引号密码参数模式。
     */
    private static final Pattern QUOTED_PASSWORD_PATTERN = Pattern.compile(
            "(?i)((?:password|pwd)\\s*=\\s*['\"])(.*?)(['\"])"
    );

    /**
     * 无引号密码参数模式。
     */
    private static final Pattern UNQUOTED_PASSWORD_PATTERN = Pattern.compile(
            "(?i)((?:password|pwd)\\s*=\\s*)([^\\s&,'\";]+)"
    );

    /**
     * MySQL 账号密码语句模式。
     */
    private static final Pattern IDENTIFIED_BY_PATTERN = Pattern.compile(
            "(?i)(identified\\s+by\\s+['\"])(.*?)(['\"])"
    );

    /**
     * Token 风险字段模式。
     */
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(?i)((?:token|access_token|authorization)\\s*[=:]\\s*)(bearer\\s+)?([^\\s&,'\";]+)"
    );

    /**
     * 工具类不允许实例化。
     */
    private SensitiveTextSanitizer() {
    }

    /**
     * 对敏感文本做基础脱敏。
     *
     * @param value 原始文本
     * @return 脱敏后的文本；空白输入保持原值
     */
    public static String sanitize(String value) {
        if (!TextUtils.hasText(value)) {
            return value;
        }
        String sanitized = JDBC_URL_PATTERN.matcher(value).replaceAll(REDACTED);
        sanitized = QUOTED_PASSWORD_PATTERN.matcher(sanitized).replaceAll("$1" + REDACTED + "$3");
        sanitized = UNQUOTED_PASSWORD_PATTERN.matcher(sanitized).replaceAll("$1" + REDACTED);
        sanitized = IDENTIFIED_BY_PATTERN.matcher(sanitized).replaceAll("$1" + REDACTED + "$3");
        sanitized = TOKEN_PATTERN.matcher(sanitized).replaceAll("$1" + REDACTED);
        return sanitized;
    }
}
