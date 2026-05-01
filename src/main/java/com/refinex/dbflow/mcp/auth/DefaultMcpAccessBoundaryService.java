package com.refinex.dbflow.mcp.auth;

import com.refinex.dbflow.access.dto.AccessDecision;
import com.refinex.dbflow.access.dto.AccessDecisionRequest;
import com.refinex.dbflow.access.service.AccessDecisionService;
import org.springframework.stereotype.Service;

/**
 * 默认 MCP 访问授权边界服务。
 *
 * @author refinex
 */
@Service
public class DefaultMcpAccessBoundaryService implements McpAccessBoundaryService {

    /**
     * 项目环境访问判断服务。
     */
    private final AccessDecisionService accessDecisionService;

    /**
     * 创建默认 MCP 访问授权边界服务。
     *
     * @param accessDecisionService 项目环境访问判断服务
     */
    public DefaultMcpAccessBoundaryService(AccessDecisionService accessDecisionService) {
        this.accessDecisionService = accessDecisionService;
    }

    /**
     * 记录只读元数据访问边界。
     *
     * @param context   MCP 请求认证上下文
     * @param operation MCP 操作名称
     * @return 授权边界结果
     */
    @Override
    public McpAuthorizationBoundary metadataBoundary(McpAuthenticationContext context, String operation) {
        if (!context.authenticated()) {
            return McpAuthorizationBoundary.authenticationRequired();
        }
        return McpAuthorizationBoundary.metadataOnly("已通过 MCP 元数据访问边界: " + operation);
    }

    /**
     * 检查目标项目环境访问边界。
     *
     * @param context     MCP 请求认证上下文
     * @param project     项目标识
     * @param environment 环境标识
     * @param operation   MCP 操作名称
     * @return 授权边界结果
     */
    @Override
    public McpAuthorizationBoundary targetBoundary(
            McpAuthenticationContext context,
            String project,
            String environment,
            String operation
    ) {
        if (!context.authenticated() || context.userId() == null || context.tokenId() == null) {
            return McpAuthorizationBoundary.authenticationRequired();
        }
        AccessDecision decision = accessDecisionService.decide(new AccessDecisionRequest(
                context.userId(),
                context.tokenId(),
                project,
                environment
        ));
        return new McpAuthorizationBoundary(true, decision.allowed(), decision.reason().name(), decision.message());
    }
}
