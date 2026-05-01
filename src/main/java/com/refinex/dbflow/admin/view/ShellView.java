package com.refinex.dbflow.admin.view;

/**
 * 管理端共享 shell 视图。
 *
 * @param adminName         当前管理员名称
 * @param mcpStatus         MCP 状态
 * @param mcpDescription    MCP 状态说明
 * @param mcpTone           MCP 状态色调
 * @param configSourceLabel 配置来源展示文本
 * @author refinex
 */
public record ShellView(
        String adminName,
        String mcpStatus,
        String mcpDescription,
        String mcpTone,
        String configSourceLabel
) {
}
