package com.refinex.dbflow.audit.dto;

import org.springframework.util.StringUtils;

/**
 * 审计请求来源上下文。
 *
 * @param clientName    客户端名称
 * @param clientVersion 客户端版本
 * @param userAgent     HTTP User-Agent
 * @param sourceIp      来源 IP
 * @param tool          MCP 工具名称
 * @author refinex
 */
public record AuditRequestContext(
        String clientName,
        String clientVersion,
        String userAgent,
        String sourceIp,
        String tool
) {

    /**
     * 创建未知来源上下文。
     *
     * @param tool 工具名称
     * @return 审计请求来源上下文
     */
    public static AuditRequestContext unknown(String tool) {
        return new AuditRequestContext("unknown", "unknown", "unknown", "unknown", valueOrUnknown(tool));
    }

    /**
     * 从 MCP clientInfo 摘要创建上下文。
     *
     * @param clientInfo MCP clientInfo 摘要
     * @param userAgent  HTTP User-Agent
     * @param sourceIp   来源 IP
     * @param tool       工具名称
     * @return 审计请求来源上下文
     */
    public static AuditRequestContext fromClientInfo(
            String clientInfo,
            String userAgent,
            String sourceIp,
            String tool
    ) {
        String normalized = valueOrUnknown(clientInfo);
        int slashIndex = normalized.indexOf('/');
        if (slashIndex > 0 && slashIndex < normalized.length() - 1) {
            return new AuditRequestContext(
                    normalized.substring(0, slashIndex),
                    normalized.substring(slashIndex + 1),
                    valueOrUnknown(userAgent),
                    valueOrUnknown(sourceIp),
                    valueOrUnknown(tool)
            );
        }
        return new AuditRequestContext(normalized, "unknown", valueOrUnknown(userAgent), valueOrUnknown(sourceIp),
                valueOrUnknown(tool));
    }

    /**
     * 返回非空值或 unknown。
     *
     * @param value 原始值
     * @return 非空值或 unknown
     */
    private static String valueOrUnknown(String value) {
        return StringUtils.hasText(value) ? value.trim() : "unknown";
    }
}
