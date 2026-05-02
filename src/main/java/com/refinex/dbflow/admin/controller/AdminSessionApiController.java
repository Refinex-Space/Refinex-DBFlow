package com.refinex.dbflow.admin.controller;

import com.refinex.dbflow.admin.dto.AdminSessionResponse;
import com.refinex.dbflow.admin.service.AdminSessionViewService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * 管理端当前 session 响应服务。
     */
    private final AdminSessionViewService sessionViewService;

    /**
     * 创建 React 管理端当前 session API。
     *
     * @param sessionViewService 管理端当前 session 响应服务
     */
    public AdminSessionApiController(AdminSessionViewService sessionViewService) {
        this.sessionViewService = Objects.requireNonNull(sessionViewService);
    }

    /**
     * 查询当前管理员 session 信息。
     *
     * @param authentication 当前认证信息
     * @return 当前 session 响应
     */
    @GetMapping
    public AdminSessionResponse session(Authentication authentication) {
        return sessionViewService.current(authentication);
    }
}
