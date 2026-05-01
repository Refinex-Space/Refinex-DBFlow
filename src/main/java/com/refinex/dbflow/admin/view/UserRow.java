package com.refinex.dbflow.admin.view;

/**
 * 用户表格行。
 *
 * @param id               用户主键
 * @param username         用户名
 * @param displayName      显示名
 * @param role             管理端角色展示
 * @param status           用户状态
 * @param grantCount       active 授权数量
 * @param activeTokenCount active Token 数量
 * @author refinex
 */
public record UserRow(Long id, String username, String displayName, String role, String status, long grantCount,
                      long activeTokenCount) {
}
