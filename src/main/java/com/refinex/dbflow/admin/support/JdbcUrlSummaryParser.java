package com.refinex.dbflow.admin.support;

import com.refinex.dbflow.common.util.TextUtils;

import java.net.URI;

/**
 * JDBC URL 安全展示解析器，只提取非敏感结构字段。
 *
 * @author refinex
 */
public final class JdbcUrlSummaryParser {

    /**
     * 工具类不允许实例化。
     */
    private JdbcUrlSummaryParser() {
    }

    /**
     * 解析 JDBC URL 的安全展示字段。
     *
     * @param jdbcUrl JDBC URL
     * @return 安全展示字段
     */
    public static JdbcParts parse(String jdbcUrl) {
        if (!TextUtils.hasText(jdbcUrl)) {
            return new JdbcParts("unknown", "-", "-", "-");
        }
        String normalized = jdbcUrl.trim();
        if (normalized.startsWith("jdbc:mysql:")) {
            return parseUriJdbc(normalized, "mysql");
        }
        if (normalized.startsWith("jdbc:h2:")) {
            return parseH2Jdbc(normalized);
        }
        return new JdbcParts("unknown", "-", "-", "-");
    }

    /**
     * 解析 URI 形态的 JDBC URL。
     *
     * @param jdbcUrl JDBC URL
     * @param type    数据库类型
     * @return 安全展示字段
     */
    private static JdbcParts parseUriJdbc(String jdbcUrl, String type) {
        try {
            URI uri = URI.create(stripJdbcQuery(jdbcUrl).substring("jdbc:".length()));
            String schema = uri.getPath();
            if (TextUtils.hasText(schema) && schema.startsWith("/")) {
                schema = schema.substring(1);
            }
            return new JdbcParts(
                    type,
                    TextUtils.displayText(uri.getHost()),
                    uri.getPort() < 0 ? "-" : Integer.toString(uri.getPort()),
                    TextUtils.displayText(schema)
            );
        } catch (IllegalArgumentException exception) {
            return new JdbcParts(type, "解析失败", "-", "-");
        }
    }

    /**
     * 解析 H2 JDBC URL。
     *
     * @param jdbcUrl JDBC URL
     * @return 安全展示字段
     */
    private static JdbcParts parseH2Jdbc(String jdbcUrl) {
        String safe = stripJdbcQuery(jdbcUrl);
        int semicolon = safe.indexOf(';');
        if (semicolon >= 0) {
            safe = safe.substring(0, semicolon);
        }
        String schema = safe.replaceFirst("^jdbc:h2:", "");
        return new JdbcParts("h2", "-", "-", TextUtils.displayText(schema));
    }

    /**
     * 移除 JDBC URL query 参数，避免展示连接参数。
     *
     * @param jdbcUrl JDBC URL
     * @return 移除 query 后的 URL
     */
    private static String stripJdbcQuery(String jdbcUrl) {
        int queryIndex = jdbcUrl.indexOf('?');
        return queryIndex >= 0 ? jdbcUrl.substring(0, queryIndex) : jdbcUrl;
    }
}
