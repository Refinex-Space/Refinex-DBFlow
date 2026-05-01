package com.refinex.dbflow.admin.view;

/**
 * 用户下拉选项。
 *
 * @param id          用户主键
 * @param username    用户名
 * @param displayName 显示名
 * @author refinex
 */
public record UserOption(Long id, String username, String displayName) {
}
