package com.refinex.dbflow.audit.service;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 审计查询文本脱敏器，用于管理端展示前移除密码、Token 和连接串。
 *
 * @author refinex
 */
final class AuditTextSanitizer {

    /**
     * 统一脱敏占位符。
     */
    private static final String REDACTED = "[REDACTED]";

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
    private AuditTextSanitizer() {
    }

    /**
     * 对审计展示文本做基础脱敏。
     *
     * @param value 原始文本
     * @return 脱敏文本
     */
    static String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
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
