package com.refinex.dbflow.mcp;

import java.util.List;
import java.util.Map;

/**
 * DBFlow MCP skeleton 统一响应结构。
 *
 * @param status         skeleton 状态
 * @param surface        MCP 暴露面名称
 * @param authentication 认证上下文
 * @param authorization  授权边界结果
 * @param data           响应数据
 * @param warnings       约束或未实现能力提示
 * @author refinex
 */
public record DbflowMcpSkeletonResponse(
        String status,
        String surface,
        McpAuthenticationContext authentication,
        McpAuthorizationBoundary authorization,
        Map<String, Object> data,
        List<String> warnings
) {

    /**
     * 创建 skeleton 响应。
     *
     * @param surface        MCP 暴露面名称
     * @param authentication 认证上下文
     * @param authorization  授权边界结果
     * @param data           响应数据
     * @return skeleton 响应
     */
    public static DbflowMcpSkeletonResponse of(
            String surface,
            McpAuthenticationContext authentication,
            McpAuthorizationBoundary authorization,
            Map<String, Object> data
    ) {
        return new DbflowMcpSkeletonResponse(
                "SKELETON",
                surface,
                authentication,
                authorization,
                data,
                List.of("当前阶段仅建立 MCP 暴露面 skeleton，未执行目标数据库访问或 SQL 操作")
        );
    }
}
