package com.refinex.dbflow.admin.service;

import com.refinex.dbflow.admin.dto.AdminSessionResponse;
import com.refinex.dbflow.admin.view.ShellView;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 管理端当前 session 响应服务。
 *
 * @author refinex
 */
@Service
public class AdminSessionViewService {

    /**
     * 管理端共享 shell 视图服务。
     */
    private final AdminShellViewService shellViewService;

    /**
     * 创建管理端当前 session 响应服务。
     *
     * @param shellViewService 管理端共享 shell 视图服务
     */
    public AdminSessionViewService(AdminShellViewService shellViewService) {
        this.shellViewService = Objects.requireNonNull(shellViewService);
    }

    /**
     * 创建当前管理员 session 响应。
     *
     * @param authentication 当前认证信息
     * @return 当前 session 响应
     */
    public AdminSessionResponse current(Authentication authentication) {
        ShellView shell = shellViewService.shell(authentication);
        String username = authentication.getName();
        return new AdminSessionResponse(
                true,
                username,
                username,
                roles(authentication),
                new AdminSessionResponse.Shell(
                        shell.adminName(),
                        shell.mcpStatus(),
                        shell.mcpTone(),
                        shell.configSourceLabel()
                )
        );
    }

    /**
     * 提取当前认证角色。
     *
     * @param authentication 当前认证信息
     * @return 角色列表
     */
    private List<String> roles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();
    }
}
