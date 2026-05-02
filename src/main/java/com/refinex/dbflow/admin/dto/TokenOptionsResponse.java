package com.refinex.dbflow.admin.dto;

import com.refinex.dbflow.admin.view.UserOption;

import java.util.List;

/**
 * React 管理端 Token 表单选项响应。
 *
 * @param users active 用户选项
 * @author refinex
 */
public record TokenOptionsResponse(List<UserOption> users) {
}
