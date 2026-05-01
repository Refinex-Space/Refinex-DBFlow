package com.refinex.dbflow.admin.command;

/**
 * 创建用户命令。
 *
 * @param username    用户名
 * @param displayName 显示名
 * @param password    初始明文密码
 * @author refinex
 */
public record CreateUserCommand(String username, String displayName, String password) {
}
