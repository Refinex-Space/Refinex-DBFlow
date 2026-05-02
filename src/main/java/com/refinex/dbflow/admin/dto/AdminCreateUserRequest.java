package com.refinex.dbflow.admin.dto;

/**
 * React 管理端创建用户请求。
 *
 * @param username    用户名
 * @param displayName 显示名
 * @param password    初始明文密码
 * @author refinex
 */
public record AdminCreateUserRequest(String username, String displayName, String password) {
}
