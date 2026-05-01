package com.refinex.dbflow.admin.view;

/**
 * Token 筛选条件。
 *
 * @param username 用户名过滤
 * @param status   状态过滤
 * @author refinex
 */
public record TokenFilter(String username, String status) {
}
