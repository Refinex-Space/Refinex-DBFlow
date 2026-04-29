package com.refinex.dbflow.mcp;

/**
 * MCP 授权边界检查结果。
 *
 * @param checked 是否已经过授权边界
 * @param allowed 是否允许继续
 * @param reason  授权原因码
 * @param message 授权说明
 * @author refinex
 */
public record McpAuthorizationBoundary(
        boolean checked,
        boolean allowed,
        String reason,
        String message
) {

    /**
     * 创建无需目标环境授权的边界结果。
     *
     * @param message 授权说明
     * @return 授权边界结果
     */
    public static McpAuthorizationBoundary metadataOnly(String message) {
        return new McpAuthorizationBoundary(true, true, "METADATA_ONLY", message);
    }

    /**
     * 创建需要认证的边界结果。
     *
     * @return 授权边界结果
     */
    public static McpAuthorizationBoundary authenticationRequired() {
        return new McpAuthorizationBoundary(true, false, "AUTHENTICATION_REQUIRED", "MCP Bearer Token 认证尚未接入");
    }
}
