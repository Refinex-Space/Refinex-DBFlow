package com.refinex.dbflow.admin;

import com.refinex.dbflow.observability.DbflowHealthService;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 管理端共享 shell 视图服务，提供顶栏运行状态和当前管理员展示模型。
 *
 * @author refinex
 */
@Service
public class AdminShellViewService {

    /**
     * DBFlow 健康服务。
     */
    private final DbflowHealthService healthService;

    /**
     * Spring 环境属性。
     */
    private final Environment environment;

    /**
     * 创建管理端共享 shell 视图服务。
     *
     * @param healthService DBFlow 健康服务
     * @param environment   Spring 环境属性
     */
    public AdminShellViewService(DbflowHealthService healthService, Environment environment) {
        this.healthService = Objects.requireNonNull(healthService);
        this.environment = Objects.requireNonNull(environment);
    }

    /**
     * 创建共享 shell 视图。
     *
     * @param authentication 当前认证信息
     * @return shell 视图
     */
    public ShellView shell(Authentication authentication) {
        DbflowHealthService.HealthComponent mcp = healthService.mcpEndpointReadiness();
        return new ShellView(
                adminName(authentication),
                mcp.status(),
                mcp.description(),
                mcp.tone(),
                configSource()
        );
    }

    /**
     * 解析当前管理员名称。
     *
     * @param authentication 当前认证信息
     * @return 管理员名称
     */
    private String adminName(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !StringUtils.hasText(authentication.getName())) {
            return "-";
        }
        return authentication.getName();
    }

    /**
     * 解析配置来源展示文本，不包含 Nacos 凭据。
     *
     * @return 配置来源展示文本
     */
    private String configSource() {
        boolean configEnabled = environment.getProperty("spring.cloud.nacos.config.enabled", Boolean.class, false);
        boolean discoveryEnabled = environment.getProperty("spring.cloud.nacos.discovery.enabled", Boolean.class, false);
        if (!configEnabled && !discoveryEnabled) {
            return "Local application config";
        }
        String namespace = environment.getProperty("spring.cloud.nacos.config.namespace", "default");
        return "Nacos enabled namespace=" + namespace;
    }

    /**
     * 管理端共享 shell 视图。
     *
     * @param adminName         当前管理员名称
     * @param mcpStatus         MCP 状态
     * @param mcpDescription    MCP 状态说明
     * @param mcpTone           MCP 状态色调
     * @param configSourceLabel 配置来源展示文本
     */
    public record ShellView(
            String adminName,
            String mcpStatus,
            String mcpDescription,
            String mcpTone,
            String configSourceLabel
    ) {
    }
}
