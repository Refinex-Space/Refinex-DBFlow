package com.refinex.dbflow.admin.dto;

import java.util.List;

/**
 * React 管理端当前 session 响应。
 *
 * @param authenticated 是否已认证
 * @param username      当前用户名
 * @param displayName   当前用户展示名
 * @param roles         当前用户角色
 * @param shell         管理端 shell 元数据
 * @author refinex
 */
public record AdminSessionResponse(
        boolean authenticated,
        String username,
        String displayName,
        List<String> roles,
        Shell shell
) {

    /**
     * 管理端 shell 元数据。
     *
     * @param adminName         当前管理员名称
     * @param mcpStatus         MCP 状态
     * @param mcpTone           MCP 状态色调
     * @param configSourceLabel 配置来源展示文本
     */
    public record Shell(
            String adminName,
            String mcpStatus,
            String mcpTone,
            String configSourceLabel
    ) {
    }
}
