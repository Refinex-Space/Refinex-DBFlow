package com.refinex.dbflow.admin.view;

/**
 * 用户筛选条件。
 *
 * @param username 用户名过滤
 * @param status   状态过滤
 * @author refinex
 */
public record UserFilter(String username, String status) {
}
