package com.refinex.dbflow.admin;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端首页占位控制器，用于验证 session 安全链路。
 *
 * @author refinex
 */
@RestController
public class AdminHomeController {

    /**
     * 返回管理端首页占位内容。
     *
     * @return 管理端首页占位内容
     */
    @GetMapping(value = "/admin", produces = MediaType.TEXT_PLAIN_VALUE)
    public String home() {
        return "Refinex DBFlow Admin";
    }
}
