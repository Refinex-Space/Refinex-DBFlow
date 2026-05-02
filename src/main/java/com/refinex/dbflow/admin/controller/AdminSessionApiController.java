package com.refinex.dbflow.admin.controller;

import com.refinex.dbflow.admin.dto.AdminSessionResponse;
import com.refinex.dbflow.admin.service.AdminShellViewService;
import com.refinex.dbflow.admin.view.ShellView;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * React 管理端当前 session API。
 *
 * @author refinex
 */
@RestController
@RequestMapping("/admin/api/session")
public class AdminSessionApiController {

    /**
     * 管理端共享 shell 视图服务。
     */
    private final AdminShellViewService shellViewService;

    /**
     * 创建 React 管理端当前 session API。
     *
     * @param shellViewService 管理端共享 shell 视图服务
     */
    public AdminSessionApiController(AdminShellViewService shellViewService) {
        this.shellViewService = Objects.requireNonNull(shellViewService);
    }

    /**
     * 查询当前管理员 session 信息。
     *
     * @param authentication 当前认证信息
     * @return 当前 session 响应
     */
    @GetMapping
    public AdminSessionResponse session(Authentication authentication) {
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
